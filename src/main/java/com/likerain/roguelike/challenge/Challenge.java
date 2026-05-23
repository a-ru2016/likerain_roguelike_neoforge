/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 */
package com.likerain.roguelike.challenge;

import com.likerain.roguelike.challenge.Reward;
import com.likerain.roguelike.data.CarryoverData;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface Challenge {
    public ResourceLocation getId();

    public Component getDisplayName();

    public void update(ServerPlayer var1, CarryoverData var2);

    public boolean isCompleted(CarryoverData var1);

    public List<Reward> getRewards();
}

