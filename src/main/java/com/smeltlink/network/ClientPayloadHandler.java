package com.smeltlink.network;

import com.smeltlink.menu.SmeltLinkMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class ClientPayloadHandler {

    private static BlockPos currentFurnacePos = BlockPos.ZERO;
    private static int currentMaxClusterSize = 1;

    private ClientPayloadHandler() { }

    public static BlockPos getCurrentFurnacePos() {
        return currentFurnacePos;
    }

    public static int getCurrentMaxClusterSize() {
        return currentMaxClusterSize;
    }

    public static void handleClusterContext(FurnaceClusterContextPayload payload) {
        currentFurnacePos = payload.pos();
        currentMaxClusterSize = payload.maxClusterSize();
    }

    public static void handleSync(SyncClusterPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu != null && player.containerMenu.containerId == payload.containerId()) {
            AbstractContainerMenu menu = player.containerMenu;
            if (menu instanceof SmeltLinkMenu smeltMenu) {
                smeltMenu.getData().set(SmeltLinkMenu.DATA_LIT_TIME, payload.litTime());
                smeltMenu.getData().set(SmeltLinkMenu.DATA_LIT_DURATION, payload.litDuration());
                smeltMenu.getData().set(SmeltLinkMenu.DATA_COOK_PROGRESS, payload.cookProgress());
                smeltMenu.getData().set(SmeltLinkMenu.DATA_COOK_TOTAL, payload.cookTotal());
                smeltMenu.getData().set(SmeltLinkMenu.DATA_BURNING_COUNT, payload.burningCount());
            }
        }
    }
}