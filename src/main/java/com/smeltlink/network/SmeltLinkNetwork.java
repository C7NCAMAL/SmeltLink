package com.smeltlink.network;

import com.smeltlink.SmeltLink;
import com.smeltlink.event.FurnaceInteractHandler;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SmeltLinkNetwork {

    private SmeltLinkNetwork() { }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(SmeltLink.MODID);

        registrar.playToClient(
                SyncClusterPayload.TYPE,
                SyncClusterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handleSync(payload))
        );

        registrar.playToClient(
                FurnaceClusterContextPayload.TYPE,
                FurnaceClusterContextPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handleClusterContext(payload))
        );

        registrar.playToServer(
                SwitchFurnaceViewPayload.TYPE,
                SwitchFurnaceViewPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        if (payload.openCluster()) {
                            FurnaceInteractHandler.openClusterMenu(serverPlayer, payload.pos());
                        } else {
                            FurnaceInteractHandler.openSingleFurnaceMenu(serverPlayer, payload.pos());
                        }
                    }
                })
        );

        SmeltLink.LOGGER.info("SmeltLink network payload handlers registered.");
    }
}