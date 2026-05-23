package com.timeattack.roguelike.network;

import com.timeattack.roguelike.TimeAttackRoguelike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * クライアント → サーバー: 達人ボーナスアイテムを選択したことを通知
 */
public record MasterBonusSelectedPayload(String selectedKit, String selectedItemLine) implements CustomPacketPayload {
    public static final Type<MasterBonusSelectedPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TimeAttackRoguelike.MOD_ID, "master_bonus_selected"));

    public static final StreamCodec<FriendlyByteBuf, MasterBonusSelectedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MasterBonusSelectedPayload::selectedKit,
            ByteBufCodecs.STRING_UTF8, MasterBonusSelectedPayload::selectedItemLine,
            MasterBonusSelectedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
