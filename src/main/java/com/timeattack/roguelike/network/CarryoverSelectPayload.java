package com.timeattack.roguelike.network;

import com.timeattack.roguelike.TimeAttackRoguelike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record CarryoverSelectPayload(List<String> selectedItems) implements CustomPacketPayload {
    public static final Type<CarryoverSelectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TimeAttackRoguelike.MOD_ID, "carryover_select"));

    public static final StreamCodec<FriendlyByteBuf, CarryoverSelectPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeInt(value.selectedItems.size());
                for (String item : value.selectedItems) {
                    buf.writeUtf(item);
                }
            },
            buf -> {
                int size = buf.readInt();
                List<String> items = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    items.add(buf.readUtf());
                }
                return new CarryoverSelectPayload(items);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
