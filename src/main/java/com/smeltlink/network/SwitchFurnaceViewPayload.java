package com.smeltlink.network;

import com.smeltlink.SmeltLink;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload sent when the player clicks the top-right toggle button
 * to switch between the individual furnace GUI and the SmeltLink cluster GUI.
 */
public record SwitchFurnaceViewPayload(
        BlockPos pos,
        boolean openCluster
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SwitchFurnaceViewPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SmeltLink.MODID, "switch_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchFurnaceViewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SwitchFurnaceViewPayload::pos,
                    ByteBufCodecs.BOOL, SwitchFurnaceViewPayload::openCluster,
                    SwitchFurnaceViewPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}