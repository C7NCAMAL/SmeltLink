package com.smeltlink.event;

import com.smeltlink.SmeltLink;
import com.smeltlink.menu.SmeltLinkMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public final class FurnaceBreakHandler {

    private FurnaceBreakHandler() { }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockPos brokenPos = event.getPos();

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                AbstractContainerMenu menu = player.containerMenu;
                if (menu instanceof SmeltLinkMenu smeltMenu) {
                    var cluster = smeltMenu.getCluster();
                    if (cluster != null && cluster.positions().contains(brokenPos)) {
                        SmeltLink.LOGGER.debug("Furnace at {} broken while in SmeltLink session — closing for {}",
                                brokenPos, player.getName().getString());
                        player.closeContainer();
                    }
                }
            }
        }
    }
}