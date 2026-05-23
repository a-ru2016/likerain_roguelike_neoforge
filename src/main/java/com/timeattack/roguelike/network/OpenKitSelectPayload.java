package com.timeattack.roguelike.network;

import com.timeattack.roguelike.TimeAttackRoguelike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.ArrayList;

public record OpenKitSelectPayload(List<String> kits) implements CustomPacketPayload {
    public static final Type<OpenKitSelectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TimeAttackRoguelike.MOD_ID, "open_kit_select"));

    public static final StreamCodec<FriendlyByteBuf, OpenKitSelectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
            OpenKitSelectPayload::kits,
            OpenKitSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
