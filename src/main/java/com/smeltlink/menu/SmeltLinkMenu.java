package com.smeltlink.menu;

import com.smeltlink.SmeltLink;
import com.smeltlink.cluster.FurnaceCluster;
import com.smeltlink.cluster.FurnaceVariant;
import com.smeltlink.event.FurnaceInteractHandler;
import com.smeltlink.inventory.AggregateInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * SmeltLink unified furnace menu with server-authoritative >64 slot handling,
 * Creative middle-click cloning, Q-key throw support, duplication-free shift-clicking,
 * and live scroll-wheel cluster resizing.
 */
public class SmeltLinkMenu extends AbstractContainerMenu {

    public static final int DATA_LIT_TIME         = 0;
    public static final int DATA_LIT_DURATION     = 1;
    public static final int DATA_COOK_PROGRESS    = 2;
    public static final int DATA_COOK_TOTAL       = 3;
    public static final int DATA_CLUSTER_SIZE     = 4;
    public static final int DATA_BURNING_COUNT    = 5;
    public static final int DATA_VARIANT_ORDINAL  = 6;
    public static final int DATA_MAX_CLUSTER_SIZE = 7;
    public static final int DATA_INPUT_COUNT      = 8;
    public static final int DATA_FUEL_COUNT       = 9;
    public static final int DATA_OUTPUT_COUNT     = 10;
    public static final int DATA_SLOT_COUNT       = 11;

    private final Container container;
    private final ContainerData data;
    private final FurnaceVariant variant;
    private final BlockPos rootPos;
    private final int maxClusterSize;

    // Server-side only
    private final FurnaceCluster cluster;
    private final AggregateInventory aggregateInventory;

    // ── Server-side Constructor ────────────────────────────────────────

    public SmeltLinkMenu(int containerId, Inventory playerInv,
                         FurnaceCluster cluster, AggregateInventory aggregateInv) {
        super(SmeltLink.SMELTLINK_MENU.get(), containerId);
        this.cluster = cluster;
        this.aggregateInventory = aggregateInv;
        this.variant = cluster.variant();
        this.rootPos = cluster.rootPos();
        this.maxClusterSize = aggregateInv.getMaxAllowedClusterSize();
        this.container = aggregateInv;

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_LIT_TIME         -> aggregateInv.getAverageBurnTimeRemaining();
                    case DATA_LIT_DURATION     -> aggregateInv.getAverageMaxBurnTime();
                    case DATA_COOK_PROGRESS    -> aggregateInv.getAverageCookProgress();
                    case DATA_COOK_TOTAL       -> aggregateInv.getAverageCookTimeTotal();
                    case DATA_CLUSTER_SIZE     -> aggregateInv.getActiveClusterSize();
                    case DATA_BURNING_COUNT    -> aggregateInv.getBurningCount();
                    case DATA_VARIANT_ORDINAL  -> variant.ordinalId();
                    case DATA_MAX_CLUSTER_SIZE -> aggregateInv.getMaxAllowedClusterSize();
                    case DATA_INPUT_COUNT      -> aggregateInv.getTotalItemCount(0);
                    case DATA_FUEL_COUNT       -> aggregateInv.getTotalItemCount(1);
                    case DATA_OUTPUT_COUNT     -> aggregateInv.getTotalItemCount(2);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) { }

            @Override
            public int getCount() { return DATA_SLOT_COUNT; }
        };

        addMenuSlots(playerInv);
        addDataSlots(this.data);
    }

    // ── Client-side Constructor (from Network) ─────────────────────────

    public static SmeltLinkMenu fromNetwork(int containerId, Inventory playerInv,
                                            RegistryFriendlyByteBuf buf) {
        BlockPos rootPos = buf.readBlockPos();
        int maxClusterSize = buf.readVarInt();
        int variantOrdinal = buf.readVarInt();
        return new SmeltLinkMenu(containerId, playerInv, rootPos, maxClusterSize,
                FurnaceVariant.fromOrdinal(variantOrdinal));
    }

    private SmeltLinkMenu(int containerId, Inventory playerInv,
                          BlockPos rootPos, int maxClusterSize, FurnaceVariant variant) {
        super(SmeltLink.SMELTLINK_MENU.get(), containerId);
        this.cluster = null;
        this.aggregateInventory = null;
        this.variant = variant;
        this.rootPos = rootPos;
        this.maxClusterSize = Math.min(64, maxClusterSize);

        this.container = new SimpleContainer(3);
        this.data = new SimpleContainerData(DATA_SLOT_COUNT);
        this.data.set(DATA_CLUSTER_SIZE, this.maxClusterSize);
        this.data.set(DATA_MAX_CLUSTER_SIZE, this.maxClusterSize);

        addMenuSlots(playerInv);
        addDataSlots(this.data);
    }

    // ── Slot Layout ────────────────────────────────────────────────────

    private void addMenuSlots(Inventory playerInv) {
        this.addSlot(new ScaledSlot(container, 0, 56, 17, this::getDataClusterSize));
        this.addSlot(new ScaledFuelSlot(container, 1, 56, 53, this::getDataClusterSize));
        this.addSlot(new SmeltLinkOutputSlot(this, playerInv.player, container, 2, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    // ── Telemetry Accessors (for Screen) ───────────────────────────────

    public FurnaceVariant getVariant()   { return variant; }
    public BlockPos getRootPos()         { return rootPos; }
    public FurnaceCluster getCluster()   { return cluster; }
    public ContainerData getData()       { return data; }

    public float getLitProgress() {
        int litDuration = data.get(DATA_LIT_DURATION);
        if (litDuration == 0) litDuration = 200;
        return (float) data.get(DATA_LIT_TIME) / (float) litDuration;
    }

    public float getBurnProgress() {
        int cookTotal = data.get(DATA_COOK_TOTAL);
        if (cookTotal == 0) cookTotal = 200;
        return (float) data.get(DATA_COOK_PROGRESS) / (float) cookTotal;
    }

    public boolean isLit() {
        return data.get(DATA_LIT_TIME) > 0;
    }

    public int getDataClusterSize() {
        int size = data.get(DATA_CLUSTER_SIZE);
        return size > 0 ? size : maxClusterSize;
    }

    public int getDataMaxClusterSize() {
        int max = data.get(DATA_MAX_CLUSTER_SIZE);
        return max > 0 ? max : maxClusterSize;
    }

    public int getDataBurningCount() {
        return data.get(DATA_BURNING_COUNT);
    }

    public int getSlotTrueCount(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> data.get(DATA_INPUT_COUNT);
            case 1 -> data.get(DATA_FUEL_COUNT);
            case 2 -> data.get(DATA_OUTPUT_COUNT);
            default -> 0;
        };
    }

    // ── Button & Scroll Wheel Actions (via Vanilla Button Packet) ──────

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer && aggregateInventory != null) {
            if (id == 0) {
                // Button 0: Return to individual furnace view
                FurnaceInteractHandler.openSingleFurnaceMenu(serverPlayer, rootPos);
                return true;
            } else if (id >= 1 && id <= 64) {
                // Button 1..64: Set active linked furnace count via scroll wheel
                aggregateInventory.setActiveClusterSize(id);
                this.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    // ── Custom Click, Clone, & Q-Throw Handling for Aggregate Slots ────

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < 3) {
            if (clickType == ContainerInput.QUICK_MOVE) {
                if (button == 0 || button == 1) {
                    this.quickMoveStack(player, slotId);
                }
                return;
            }

            // Support Creative Mode middle-click (ContainerInput.CLONE) on aggregate slots 0, 1, 2
            if (clickType == ContainerInput.CLONE) {
                if (player.hasInfiniteMaterials() && this.getCarried().isEmpty()) {
                    ItemStack inSlot = (aggregateInventory != null)
                            ? aggregateInventory.getItem(slotId)
                            : this.slots.get(slotId).getItem();
                    if (!inSlot.isEmpty()) {
                        this.setCarried(inSlot.copyWithCount(inSlot.getMaxStackSize()));
                    }
                }
                return;
            }

            // Support pressing Q (button 0 = drop 1) and Ctrl+Q (button 1 = drop stack)
            if (clickType == ContainerInput.THROW && this.getCarried().isEmpty() && (button == 0 || button == 1)) {
                if (aggregateInventory == null) {
                    return; // Server authoritative
                }

                ItemStack inSlot = aggregateInventory.getItem(slotId);
                int totalAvail = aggregateInventory.getTotalItemCount(slotId);
                if (inSlot.isEmpty() || totalAvail <= 0) return;

                int toDrop = (button == 0) ? 1 : Math.min(inSlot.getMaxStackSize(), totalAvail);
                ItemStack dropped = aggregateInventory.removeItem(slotId, toDrop);
                if (!dropped.isEmpty()) {
                    if (slotId == 2) {
                        awardClusterExperience(player);
                    }
                    player.drop(dropped, true, Prediction.PREDICTED);
                    this.broadcastChanges();
                }
                return;
            }

            if (clickType == ContainerInput.PICKUP && (button == 0 || button == 1)) {
                if (aggregateInventory == null) {
                    return; // Server authoritative
                }

                ItemStack carried = this.getCarried();

                if (slotId == 2) {
                    // Output slot extraction
                    ItemStack inSlot = aggregateInventory.getItem(2);
                    int totalAvail = aggregateInventory.getTotalItemCount(2);
                    if (inSlot.isEmpty() || totalAvail <= 0) return;

                    if (carried.isEmpty()) {
                        int maxStack = inSlot.getMaxStackSize();
                        int toTake = (button == 1)
                                ? Math.min((maxStack + 1) / 2, (totalAvail + 1) / 2)
                                : Math.min(maxStack, totalAvail);
                        ItemStack extracted = aggregateInventory.removeItem(2, Math.max(1, toTake));
                        this.setCarried(extracted);
                        awardClusterExperience(player);
                    } else if (ItemStack.isSameItemSameComponents(carried, inSlot)) {
                        int space = carried.getMaxStackSize() - carried.getCount();
                        if (space > 0) {
                            int toTake = Math.min(space, totalAvail);
                            ItemStack extracted = aggregateInventory.removeItem(2, toTake);
                            carried.grow(extracted.getCount());
                            this.setCarried(carried);
                            awardClusterExperience(player);
                        }
                    }
                    this.broadcastChanges();
                    return;
                }

                // Slots 0 (Input) and 1 (Fuel)
                if (carried.isEmpty()) {
                    // Extract from slot onto cursor
                    ItemStack inSlot = aggregateInventory.getItem(slotId);
                    int totalAvail = aggregateInventory.getTotalItemCount(slotId);
                    if (inSlot.isEmpty() || totalAvail <= 0) return;

                    int maxStack = inSlot.getMaxStackSize();
                    int toTake = (button == 1)
                            ? Math.min((maxStack + 1) / 2, (totalAvail + 1) / 2)
                            : Math.min(maxStack, totalAvail);
                    ItemStack extracted = aggregateInventory.removeItem(slotId, Math.max(1, toTake));
                    this.setCarried(extracted);
                    this.broadcastChanges();
                } else {
                    // Insert from cursor into aggregate slot (supports >64 up to 64*N)
                    if (!aggregateInventory.canPlaceItem(slotId, carried)) return;

                    ItemStack existing = aggregateInventory.getItem(slotId);
                    if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, carried)) {
                        return;
                    }

                    int toInsertCount = (button == 1) ? 1 : carried.getCount();
                    ItemStack leftover = aggregateInventory.insertItem(slotId, carried.copyWithCount(toInsertCount));
                    int accepted = toInsertCount - leftover.getCount();
                    if (accepted > 0) {
                        carried.shrink(accepted);
                        this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                        this.broadcastChanges();
                    }
                }
                return;
            }

            // Block unsupported click types (SWAP, CLONE, QUICK_CRAFT, THROW) on virtual proxy slots
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    // ── Duplication-Free QuickMove (Shift-Click) ───────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) return ItemStack.EMPTY;

        // Server-authoritative handling when moving to/from virtual aggregate slots
        if (aggregateInventory == null) {
            return ItemStack.EMPTY;
        }

        if (slotIndex >= 0 && slotIndex < 3) {
            // Aggregate Slot (0, 1, 2) -> Player Inventory (3..38)
            ItemStack sample = aggregateInventory.getItem(slotIndex);
            int totalAvail = aggregateInventory.getTotalItemCount(slotIndex);
            if (sample.isEmpty() || totalAvail <= 0) return ItemStack.EMPTY;

            int batchSize = Math.min(sample.getMaxStackSize(), totalAvail);
            ItemStack extracted = aggregateInventory.removeItem(slotIndex, batchSize);
            if (extracted.isEmpty()) return ItemStack.EMPTY;

            int initialExtracted = extracted.getCount();
            this.moveItemStackTo(extracted, 3, 39, slotIndex == 2);
            int moved = initialExtracted - extracted.getCount();

            // Return any unaccepted remainder back into the cluster
            if (!extracted.isEmpty()) {
                if (slotIndex == 2) {
                    aggregateInventory.putBackOutput(extracted);
                } else {
                    aggregateInventory.insertItem(slotIndex, extracted);
                }
            }

            if (moved > 0) {
                if (slotIndex == 2) {
                    awardClusterExperience(player);
                }
                this.broadcastChanges();
            }

            // Always return EMPTY to prevent vanilla's while-loop from infinitely duplicating
            return ItemStack.EMPTY;
        }

        // Player Inventory (3..38) -> Aggregate Slots (0 or 1)
        Slot playerSlot = this.slots.get(slotIndex);
        if (!playerSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = playerSlot.getItem();
        int moved = 0;

        if (aggregateInventory.canPlaceItem(0, sourceStack)) {
            ItemStack existing = aggregateInventory.getItem(0);
            if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, sourceStack)) {
                ItemStack leftover = aggregateInventory.insertItem(0, sourceStack.copy());
                moved = sourceStack.getCount() - leftover.getCount();
            }
        }

        if (moved == 0 && aggregateInventory.canPlaceItem(1, sourceStack)) {
            ItemStack existing = aggregateInventory.getItem(1);
            if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, sourceStack)) {
                ItemStack leftover = aggregateInventory.insertItem(1, sourceStack.copy());
                moved = sourceStack.getCount() - leftover.getCount();
            }
        }

        if (moved > 0) {
            sourceStack.shrink(moved);
            playerSlot.setByPlayer(sourceStack.isEmpty() ? ItemStack.EMPTY : sourceStack);
            playerSlot.setChanged();
            this.broadcastChanges();
            return ItemStack.EMPTY;
        }

        // Fallback: move between main inventory and hotbar
        ItemStack copy = sourceStack.copy();
        boolean movedInternal;
        if (slotIndex < 30) {
            movedInternal = this.moveItemStackTo(sourceStack, 30, 39, false);
        } else {
            movedInternal = this.moveItemStackTo(sourceStack, 3, 30, false);
        }

        if (!movedInternal) return ItemStack.EMPTY;

        if (sourceStack.isEmpty()) {
            playerSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            playerSlot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (cluster == null) return true;
        Level level = player.level();
        if (!cluster.isValid(level)) return false;

        for (BlockPos pos : cluster.positions()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0) {
                return true;
            }
        }
        return false;
    }

    public void awardClusterExperience(Player player) {
        if (cluster == null || aggregateInventory == null) return;
        if (player instanceof ServerPlayer serverPlayer) {
            for (AbstractFurnaceBlockEntity be : aggregateInventory.getActiveFurnaces()) {
                be.awardUsedRecipesAndPopExperience(serverPlayer);
            }
        }
    }
}