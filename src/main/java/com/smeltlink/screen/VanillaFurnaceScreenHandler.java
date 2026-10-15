package com.smeltlink.screen;

import com.smeltlink.network.ClientPayloadHandler;
import com.smeltlink.network.SwitchFurnaceViewPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Injects the top-right "⛓ xN" Single Mode toggle button into the vanilla
 * individual furnace screen, preserves cursor position when switching views,
 * and dynamically shifts with the Recipe Book.
 */
public final class VanillaFurnaceScreenHandler {

    private static Button clusterToggleButton;

    private VanillaFurnaceScreenHandler() { }

    private static Component buildSingleModeLabel(int maxCluster) {
        return Component.literal("⛓ x" + maxCluster).withStyle(ChatFormatting.AQUA);
    }

    private static Tooltip buildSingleModeTooltip(int maxCluster) {
        return Tooltip.create(
                Component.literal("Mode: ")
                        .append(Component.literal("SINGLE FURNACE").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD))
                        .append(Component.literal("\nClick: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("Open Share Mode (x" + maxCluster + ")").withStyle(ChatFormatting.AQUA))
        );
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        clusterToggleButton = null;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (containerScreen instanceof SmeltLinkScreen) return;
        if (!(containerScreen.getMenu() instanceof AbstractFurnaceMenu)) return;

        int btnW = 38;
        int btnH = 14;
        int btnX = containerScreen.getLeftPos() + containerScreen.getImageWidth() - btnW - 6;
        int btnY = containerScreen.getTopPos() + 5;

        int maxCluster = ClientPayloadHandler.getCurrentMaxClusterSize();

        clusterToggleButton = Button.builder(
                buildSingleModeLabel(maxCluster),
                btn -> {
                    CursorStateTracker.captureBeforeSwitch();
                    ClientPacketDistributor.sendToServer(
                            new SwitchFurnaceViewPayload(ClientPayloadHandler.getCurrentFurnacePos(), true)
                    );
                })
                .bounds(btnX, btnY, btnW, btnH)
                .tooltip(buildSingleModeTooltip(maxCluster))
                .build();

        clusterToggleButton.visible = maxCluster > 1;
        event.addListener(clusterToggleButton);

        // Restore cursor position if returning from SmeltLink Share Mode
        CursorStateTracker.restoreIfPending(containerScreen);
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (containerScreen instanceof SmeltLinkScreen) return;
        if (!(containerScreen.getMenu() instanceof AbstractFurnaceMenu)) return;

        CursorStateTracker.restoreIfPending(containerScreen);

        if (clusterToggleButton == null) return;

        int maxCluster = ClientPayloadHandler.getCurrentMaxClusterSize();
        clusterToggleButton.visible = maxCluster > 1;

        if (clusterToggleButton.visible) {
            clusterToggleButton.setMessage(buildSingleModeLabel(maxCluster));
            clusterToggleButton.setTooltip(buildSingleModeTooltip(maxCluster));

            int btnX = containerScreen.getLeftPos() + containerScreen.getImageWidth() - clusterToggleButton.getWidth() - 6;
            int btnY = containerScreen.getTopPos() + 5;
            clusterToggleButton.setX(btnX);
            clusterToggleButton.setY(btnY);
        }
    }
}