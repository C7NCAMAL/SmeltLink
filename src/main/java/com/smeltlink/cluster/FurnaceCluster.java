package com.smeltlink.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public record FurnaceCluster(
        FurnaceVariant variant,
        List<BlockPos> positions,
        List<AbstractFurnaceBlockEntity> furnaces
) {

    public static FurnaceCluster create(Level level, List<BlockPos> positions, FurnaceVariant variant) {
        List<AbstractFurnaceBlockEntity> furnaces = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity be) {
                furnaces.add(be);
            } else {
                return null;
            }
        }
        return new FurnaceCluster(variant,
                Collections.unmodifiableList(new ArrayList<>(positions)),
                Collections.unmodifiableList(furnaces));
    }

    public int size() {
        return positions.size();
    }

    public BlockPos rootPos() {
        return positions.getFirst();
    }

    public boolean isValid(Level level) {
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            if (!level.isLoaded(pos)) return false;
            if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity be)) return false;
            if (be.getType() != variant.blockEntityType()) return false;
            if (be != furnaces.get(i)) return false;
        }
        return true;
    }
}