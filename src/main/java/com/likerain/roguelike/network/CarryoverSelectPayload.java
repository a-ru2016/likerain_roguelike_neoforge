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
package com.likerain.roguelike.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CarryoverSelectPayload(List<String> selectedItems) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CarryoverSelectPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"likerain_roguelike", (String)"carryover_select"));
    public static final StreamCodec<FriendlyByteBuf, CarryoverSelectPayload> STREAM_CODEC = StreamCodec.of((buf, value) -> {
        buf.writeInt(value.selectedItems.size());
        for (String item : value.selectedItems) {
            buf.writeUtf(item);
        }
    }, buf -> {
        int size = buf.readInt();
        ArrayList<String> items = new ArrayList<String>(size);
        for (int i = 0; i < size; ++i) {
            items.add(buf.readUtf());
        }
        return new CarryoverSelectPayload(items);
    });

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

