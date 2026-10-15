package com.smeltlink.network;

import com.smeltlink.SmeltLink;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncClusterPayload(
        int containerId,
        int litTime,
        int litDuration,
        int cookProgress,
        int cookTotal,
        int burningCount
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncClusterPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SmeltLink.MODID, "sync_cluster"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncClusterPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncClusterPayload::containerId,
                    ByteBufCodecs.VAR_INT, SyncClusterPayload::litTime,
                    ByteBufCodecs.VAR_INT, SyncClusterPayload::litDuration,
                    ByteBufCodecs.VAR_INT, SyncClusterPayload::cookProgress,
                    ByteBufCodecs.VAR_INT, SyncClusterPayload::cookTotal,
                    ByteBufCodecs.VAR_INT, SyncClusterPayload::burningCount,
                    SyncClusterPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}