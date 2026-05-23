/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package com.timeattack.roguelike.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenMasterBonusSelectPayload(List<String> masterKitNames, List<List<String>> masterKitItemLines) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<OpenMasterBonusSelectPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"timeattackroguelike", (String)"open_master_bonus_select"));
    public static final StreamCodec<FriendlyByteBuf, OpenMasterBonusSelectPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.collection(ArrayList::new, (StreamCodec)ByteBufCodecs.STRING_UTF8), OpenMasterBonusSelectPayload::masterKitNames, (StreamCodec)ByteBufCodecs.collection(ArrayList::new, (StreamCodec)ByteBufCodecs.collection(ArrayList::new, (StreamCodec)ByteBufCodecs.STRING_UTF8)), OpenMasterBonusSelectPayload::masterKitItemLines, OpenMasterBonusSelectPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

