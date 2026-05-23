package com.timeattack.roguelike.network;

import com.timeattack.roguelike.TimeAttackRoguelike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.ArrayList;

/**
 * サーバー → クライアント: 達人ボーナス選択画面を開く
 */
public record OpenMasterBonusSelectPayload(List<String> masterKitNames, List<List<String>> masterKitItemLines) implements CustomPacketPayload {
    public static final Type<OpenMasterBonusSelectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TimeAttackRoguelike.MOD_ID, "open_master_bonus_select"));

    public static final StreamCodec<FriendlyByteBuf, OpenMasterBonusSelectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
            OpenMasterBonusSelectPayload::masterKitNames,
            ByteBufCodecs.collection(ArrayList::new,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
            ),
            OpenMasterBonusSelectPayload::masterKitItemLines,
            OpenMasterBonusSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
