package com.smeltlink.network;

import com.smeltlink.SmeltLink;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload informing the client of the furnace position and discovered cluster size
 * when an individual vanilla furnace screen is opened.
 */
public record FurnaceClusterContextPayload(
        BlockPos pos,
        int maxClusterSize
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FurnaceClusterContextPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SmeltLink.MODID, "cluster_context"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FurnaceClusterContextPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FurnaceClusterContextPayload::pos,
                    ByteBufCodecs.VAR_INT, FurnaceClusterContextPayload::maxClusterSize,
                    FurnaceClusterContextPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}