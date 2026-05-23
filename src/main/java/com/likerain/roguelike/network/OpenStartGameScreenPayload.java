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
package com.likerain.roguelike.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenStartGameScreenPayload(List<String> kits) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<OpenStartGameScreenPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"likerain_roguelike", (String)"open_start_game_screen"));
    public static final StreamCodec<FriendlyByteBuf, OpenStartGameScreenPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.collection(ArrayList::new, (StreamCodec)ByteBufCodecs.STRING_UTF8), OpenStartGameScreenPayload::kits, OpenStartGameScreenPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

