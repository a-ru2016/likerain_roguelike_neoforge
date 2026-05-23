package com.timeattack.roguelike.network;

import com.timeattack.roguelike.TimeAttackRoguelike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KitSelectedPayload(String selectedKit) implements CustomPacketPayload {
    public static final Type<KitSelectedPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TimeAttackRoguelike.MOD_ID, "kit_selected"));

    public static final StreamCodec<FriendlyByteBuf, KitSelectedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, KitSelectedPayload::selectedKit,
            KitSelectedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
