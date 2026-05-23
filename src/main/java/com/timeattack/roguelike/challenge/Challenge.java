package com.timeattack.roguelike.challenge;

import com.timeattack.roguelike.data.CarryoverData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public interface Challenge {
    ResourceLocation getId();
    Component getDisplayName();
    void update(ServerPlayer player, CarryoverData carryover);
    boolean isCompleted(CarryoverData carryover);
    List<Reward> getRewards();
}
