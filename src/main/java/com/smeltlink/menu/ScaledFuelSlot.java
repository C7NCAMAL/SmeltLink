package com.smeltlink.menu;

import java.util.function.IntSupplier;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Custom fuel slot with dynamic capacity scaling based on the active cluster size.
 */
public class ScaledFuelSlot extends Slot {

    private final IntSupplier activeClusterSizeSupplier;

    public ScaledFuelSlot(Container container, int slot, int x, int y, IntSupplier activeClusterSizeSupplier) {
        super(container, slot, x, y);
        this.activeClusterSizeSupplier = activeClusterSizeSupplier;
    }

    @Override
    public int getMaxStackSize() {
        return 64 * Math.max(1, activeClusterSizeSupplier.getAsInt());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 64 * Math.max(1, activeClusterSizeSupplier.getAsInt());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.container.canPlaceItem(this.getContainerSlot(), stack);
    }
}