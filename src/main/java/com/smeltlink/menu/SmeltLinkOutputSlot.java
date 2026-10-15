package com.smeltlink.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SmeltLinkOutputSlot extends Slot {

    private final SmeltLinkMenu menu;
    private final Player player;
    private int removeCount;

    public SmeltLinkOutputSlot(SmeltLinkMenu menu, Player player,
                               Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.menu = menu;
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }
        return super.remove(amount);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        super.onTake(player, stack);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.removeCount += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        stack.onCraftedBy(player, this.removeCount);
        menu.awardClusterExperience(player);
        this.removeCount = 0;
    }
}