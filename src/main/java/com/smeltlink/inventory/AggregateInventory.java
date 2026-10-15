package com.smeltlink.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.smeltlink.cluster.FurnaceCluster;
import com.smeltlink.config.SmeltLinkConfig;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Virtual Aggregate Proxy implementing {@link Container} directly over the active
 * subset of furnaces in the cluster (controlled via scroll wheel from 1 to max).
 */
public class AggregateInventory implements Container {

    public static final int SLOT_INPUT  = 0;
    public static final int SLOT_FUEL   = 1;
    public static final int SLOT_OUTPUT = 2;

    private final FurnaceCluster cluster;
    private final Level level;
    private int activeClusterSize;

    public AggregateInventory(FurnaceCluster cluster, Level level) {
        this.cluster = cluster;
        this.level = level;
        this.activeClusterSize = getMaxAllowedClusterSize();
    }

    public FurnaceCluster getCluster() {
        return cluster;
    }

    public int getMaxAllowedClusterSize() {
        int configCap = Math.min(64, SmeltLinkConfig.MAX_CONNECTED_FURNACES.getAsInt());
        return Math.max(1, Math.min(cluster.size(), configCap));
    }

    public int getActiveClusterSize() {
        return activeClusterSize;
    }

    public void setActiveClusterSize(int size) {
        this.activeClusterSize = Mth.clamp(size, 1, getMaxAllowedClusterSize());
    }

    public List<AbstractFurnaceBlockEntity> getActiveFurnaces() {
        int limit = Math.min(activeClusterSize, cluster.furnaces().size());
        return cluster.furnaces().subList(0, limit);
    }

    // ── Container Implementation ────────────────────────────────────────

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            if (!be.isEmpty()) return false;
        }
        return true;
    }

    /**
     * Returns a visual representative stack clamped to standard maxStackSize (64)
     * so vanilla network slot packets never crash or reject oversized counts.
     * True counts (>64) are synced separately via ContainerData.
     */
    @Override
    public ItemStack getItem(int slot) {
        ItemStack result = ItemStack.EMPTY;
        int totalCount = 0;

        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            ItemStack inBe = be.getItem(slot);
            if (inBe.isEmpty()) continue;

            if (result.isEmpty()) {
                result = inBe.copy();
                totalCount = inBe.getCount();
            } else if (ItemStack.isSameItemSameComponents(result, inBe)) {
                totalCount += inBe.getCount();
            }
        }

        if (!result.isEmpty()) {
            result.setCount(Math.min(totalCount, result.getMaxStackSize()));
        }
        return result;
    }

    /**
     * Calculates the exact total count of matching items in the given slot
     * across all currently active furnaces in the cluster (up to 64 * N).
     */
    public int getTotalItemCount(int slot) {
        ItemStack template = ItemStack.EMPTY;
        int total = 0;

        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            ItemStack inBe = be.getItem(slot);
            if (inBe.isEmpty()) continue;

            if (template.isEmpty()) {
                template = inBe;
                total += inBe.getCount();
            } else if (ItemStack.isSameItemSameComponents(template, inBe)) {
                total += inBe.getCount();
            }
        }
        return total;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) return ItemStack.EMPTY;

        ItemStack result = ItemStack.EMPTY;
        int remaining = amount;

        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            ItemStack inBe = be.getItem(slot);
            if (inBe.isEmpty()) continue;

            if (result.isEmpty()) {
                result = inBe.copy();
                int toTake = Math.min(remaining, inBe.getCount());
                result.setCount(toTake);
                inBe.shrink(toTake);
                remaining -= toTake;
            } else if (ItemStack.isSameItemSameComponents(result, inBe)) {
                int toTake = Math.min(remaining, inBe.getCount());
                result.grow(toTake);
                inBe.shrink(toTake);
                remaining -= toTake;
            }

            if (inBe.isEmpty()) {
                be.setItem(slot, ItemStack.EMPTY);
            }
            be.setChanged();

            if (remaining <= 0) break;
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        int total = getTotalItemCount(slot);
        if (total <= 0) return ItemStack.EMPTY;
        return removeItem(slot, Math.min(64, total));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Do not wipe furnaces when vanilla slot synchronization calls setItem
        if (stack.isEmpty()) {
            for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
                be.setItem(slot, ItemStack.EMPTY);
                be.setChanged();
            }
            return;
        }
        int currentTotal = getTotalItemCount(slot);
        if (currentTotal == 0) {
            insertItem(slot, stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64 * Math.max(1, activeClusterSize);
    }

    @Override
    public void setChanged() {
        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            be.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return cluster.isValid(player.level());
    }

    @Override
    public void clearContent() {
        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            be.clearContent();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) {
            return false;
        }
        if (slot == SLOT_FUEL) {
            return isFuel(stack);
        }
        if (slot == SLOT_INPUT) {
            if (SmeltLinkConfig.STRICT_INPUT_MATCHING.get() && level != null) {
                return isValidInputForVariant(stack);
            }
            return true;
        }
        return false;
    }

    // ── Two-Pass Distribution (Balanced Round-Robin + Waterfall) ────────

    public ItemStack insertItem(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot == SLOT_OUTPUT) return stack;

        List<AbstractFurnaceBlockEntity> active = getActiveFurnaces();
        List<AbstractFurnaceBlockEntity> targets = active;

        if (slot == SLOT_FUEL) {
            List<AbstractFurnaceBlockEntity> withInput = active.stream()
                    .filter(be -> !be.getItem(SLOT_INPUT).isEmpty())
                    .toList();
            if (!withInput.isEmpty()) {
                targets = withInput;
            }
        }

        return distributeTwoPass(targets, slot, stack);
    }

    /**
     * Used only when returning unaccepted output items back to the cluster
     * if player inventory filled up mid-shift-click.
     */
    public void putBackOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        distributeTwoPass(getActiveFurnaces(), SLOT_OUTPUT, stack);
    }

    private ItemStack distributeTwoPass(List<AbstractFurnaceBlockEntity> targets, int slot, ItemStack stack) {
        if (targets.isEmpty() || stack.isEmpty()) return stack;

        // Filter only compatible furnaces with available space and sort by least filled first
        List<AbstractFurnaceBlockEntity> compatible = new ArrayList<>();
        for (AbstractFurnaceBlockEntity be : targets) {
            ItemStack current = be.getItem(slot);
            if (current.isEmpty() || (ItemStack.isSameItemSameComponents(current, stack)
                    && current.getCount() < be.getMaxStackSize(current))) {
                compatible.add(be);
            }
        }

        if (compatible.isEmpty()) return stack;

        compatible.sort(Comparator.comparingInt(be -> be.getItem(slot).getCount()));

        int remaining = stack.getCount();
        int count = compatible.size();

        // Pass 1: Even round-robin share across all compatible furnaces
        int basePerFurnace = remaining / count;
        int rem = remaining % count;

        for (int i = 0; i < count && remaining > 0; i++) {
            int share = basePerFurnace + (i < rem ? 1 : 0);
            if (share <= 0) continue;

            AbstractFurnaceBlockEntity be = compatible.get(i);
            int accepted = insertIntoSingleFurnace(be, slot, stack, share);
            remaining -= accepted;
        }

        // Pass 2: Waterfall any leftover items into any furnace that still has space
        if (remaining > 0) {
            for (AbstractFurnaceBlockEntity be : compatible) {
                if (remaining <= 0) break;
                int accepted = insertIntoSingleFurnace(be, slot, stack, remaining);
                remaining -= accepted;
            }
        }

        if (remaining <= 0) return ItemStack.EMPTY;
        return stack.copyWithCount(remaining);
    }

    private int insertIntoSingleFurnace(AbstractFurnaceBlockEntity be, int slot, ItemStack template, int maxToInsert) {
        if (maxToInsert <= 0) return 0;
        ItemStack current = be.getItem(slot);
        int maxStack = Math.min(64, be.getMaxStackSize(template));

        if (current.isEmpty()) {
            int accepted = Math.min(maxToInsert, maxStack);
            be.setItem(slot, template.copyWithCount(accepted));
            be.setChanged();
            return accepted;
        } else if (ItemStack.isSameItemSameComponents(current, template)) {
            int space = Math.max(0, maxStack - current.getCount());
            int accepted = Math.min(maxToInsert, space);
            if (accepted > 0) {
                current.grow(accepted);
                be.setChanged();
            }
            return accepted;
        }
        return 0;
    }

    // ── Recipe & Fuel Validation ───────────────────────────────────────

    public boolean isFuel(ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL) ||
                (stack.is(Items.BUCKET) && !getItem(SLOT_FUEL).is(Items.BUCKET));
    }

    public boolean isValidInputForVariant(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        return level.recipeAccess()
                .propertySet(cluster.variant().recipePropertySet())
                .test(stack);
    }

    // ── Telemetry (ContainerData Sync over Active Furnaces) ────────────

    public int getAverageBurnTimeRemaining() {
        int count = 0;
        int total = 0;
        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            int litTime = FurnaceTelemetry.getLitTime(be);
            if (litTime > 0) {
                total += litTime;
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    public int getAverageMaxBurnTime() {
        int count = 0;
        int total = 0;
        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            int litTime = FurnaceTelemetry.getLitTime(be);
            if (litTime > 0) {
                total += FurnaceTelemetry.getLitDuration(be);
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    public int getAverageCookProgress() {
        int count = 0;
        int total = 0;
        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            int progress = FurnaceTelemetry.getCookProgress(be);
            if (progress > 0) {
                total += progress;
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    public int getAverageCookTimeTotal() {
        int count = 0;
        int total = 0;
        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            int progress = FurnaceTelemetry.getCookProgress(be);
            if (progress > 0) {
                total += FurnaceTelemetry.getCookTotalTime(be);
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    public int getBurningCount() {
        int count = 0;
        for (AbstractFurnaceBlockEntity be : getActiveFurnaces()) {
            if (FurnaceTelemetry.getLitTime(be) > 0) count++;
        }
        return count;
    }

    public boolean isLit() {
        return getBurningCount() > 0;
    }
}