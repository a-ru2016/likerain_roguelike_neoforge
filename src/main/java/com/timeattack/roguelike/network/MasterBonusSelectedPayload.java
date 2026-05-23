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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MasterBonusSelectedPayload(String selectedKit, String selectedItemLine) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<MasterBonusSelectedPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"timeattackroguelike", (String)"master_bonus_selected"));
    public static final StreamCodec<FriendlyByteBuf, MasterBonusSelectedPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.STRING_UTF8, MasterBonusSelectedPayload::selectedKit, (StreamCodec)ByteBufCodecs.STRING_UTF8, MasterBonusSelectedPayload::selectedItemLine, MasterBonusSelectedPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

