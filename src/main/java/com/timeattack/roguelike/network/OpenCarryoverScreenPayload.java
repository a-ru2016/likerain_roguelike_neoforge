/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package com.timeattack.roguelike.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCarryoverScreenPayload() implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<OpenCarryoverScreenPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"timeattackroguelike", (String)"open_carryover_screen"));
    public static final StreamCodec<FriendlyByteBuf, OpenCarryoverScreenPayload> STREAM_CODEC = StreamCodec.unit(new OpenCarryoverScreenPayload());

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

