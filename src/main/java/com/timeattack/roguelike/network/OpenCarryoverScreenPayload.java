package com.timeattack.roguelike.network;

import com.timeattack.roguelike.TimeAttackRoguelike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCarryoverScreenPayload() implements CustomPacketPayload {
    public static final Type<OpenCarryoverScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TimeAttackRoguelike.MOD_ID, "open_carryover_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenCarryoverScreenPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenCarryoverScreenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
