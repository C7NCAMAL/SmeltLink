package com.smeltlink.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.smeltlink.SmeltLink;
import com.smeltlink.config.SmeltLinkConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class FurnaceClusterFinder {

    private FurnaceClusterFinder() { }

    public static List<BlockPos> findCluster(Level level, BlockPos rootPos) {
        if (!level.isLoaded(rootPos)) return Collections.emptyList();

        BlockState rootState = level.getBlockState(rootPos);
        if (!rootState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return Collections.emptyList();
        }

        if (!(level.getBlockEntity(rootPos) instanceof AbstractFurnaceBlockEntity rootBE)) {
            return Collections.emptyList();
        }

        Direction facing = rootState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockEntityType<?> rootType = rootBE.getType();

        int maxSize = Math.min(64, SmeltLinkConfig.MAX_CONNECTED_FURNACES.getAsInt());
        Direction[] neighbors = getPerpendicularDirections(facing);

        List<BlockPos> cluster = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        cluster.add(rootPos);
        visited.add(rootPos);
        queue.add(rootPos);

        while (!queue.isEmpty() && cluster.size() < maxSize) {
            BlockPos current = queue.poll();

            for (Direction dir : neighbors) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                if (!level.isLoaded(neighbor)) continue;

                BlockState neighborState = level.getBlockState(neighbor);
                if (!neighborState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) continue;
                if (neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) != facing) continue;

                if (!(level.getBlockEntity(neighbor) instanceof AbstractFurnaceBlockEntity neighborBE)) continue;
                if (neighborBE.getType() != rootType) continue;

                cluster.add(neighbor);
                if (cluster.size() >= maxSize) break;
                queue.add(neighbor);
            }
        }

        SmeltLink.LOGGER.debug("SmeltLink BFS found cluster of {} at root {}", cluster.size(), rootPos);
        return Collections.unmodifiableList(cluster);
    }

    private static Direction[] getPerpendicularDirections(Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH -> new Direction[]{
                    Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
            };
            case EAST, WEST -> new Direction[]{
                    Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN
            };
            default -> new Direction[]{
                    Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
            };
        };
    }
}