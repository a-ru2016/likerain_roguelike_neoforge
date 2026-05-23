/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 */
package com.timeattack.roguelike.data;

import com.timeattack.roguelike.event.ReincarnationHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public class PlayerRunState
extends SavedData {
    public boolean gameStarted = false;
    public float difficulty = 0.0f;
    public int dayCount = 0;
    public int currentNightTicks = 0;
    private final Map<UUID, PlayerState> playerStates = new HashMap<UUID, PlayerState>();
    public final Set<UUID> votedPlayers = new HashSet<UUID>();

    public static PlayerRunState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        PlayerRunState state = new PlayerRunState();
        state.gameStarted = nbt.getBoolean("gameStarted");
        state.difficulty = nbt.getFloat("difficulty");
        state.dayCount = nbt.getInt("dayCount");
        state.currentNightTicks = nbt.getInt("currentNightTicks");
        ListTag list = nbt.getList("playerStates", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag compound = list.getCompound(i);
            UUID uuid = compound.getUUID("uuid");
            state.playerStates.put(uuid, PlayerState.fromNbt(compound.getCompound("state")));
        }
        return state;
    }

    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putBoolean("gameStarted", this.gameStarted);
        nbt.putFloat("difficulty", this.difficulty);
        nbt.putInt("dayCount", this.dayCount);
        nbt.putInt("currentNightTicks", this.currentNightTicks);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PlayerState> entry : this.playerStates.entrySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putUUID("uuid", entry.getKey());
            compound.put("state", (Tag)entry.getValue().toNbt());
            list.add(compound);
        }
        nbt.put("playerStates", (Tag)list);
        return nbt;
    }


    public static PlayerRunState get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PlayerRunState::new, PlayerRunState::fromNbt, null),
                "timeattack_playerrunstate"
        );
    }

    public PlayerState getOrCreateState(UUID uuid) {
        return this.playerStates.computeIfAbsent(uuid, k -> new PlayerState());
    }

    public Map<UUID, PlayerState> getPlayerStates() {
        return this.playerStates;
    }

    public void markDirtyAndSave() {
        this.setDirty();
    }

    public void addVote(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (this.votedPlayers.contains(uuid)) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u3042\u306a\u305f\u306f\u3059\u3067\u306b\u6295\u7968\u3057\u3066\u3044\u307e\u3059\u3002"));
            return;
        }
        this.votedPlayers.add(uuid);
        int onlineCount = player.getServer().getPlayerCount();
        int required = 3;
        if (onlineCount <= 3) {
            required = 1;
        } else if (onlineCount <= 5) {
            required = 2;
        }
        player.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)("\u00a7d[\u8ee2\u751f\u6295\u7968] \u00a7e" + player.getName().getString() + " \u00a7f\u304c\u8ee2\u751f\u306b\u6295\u7968\u3057\u307e\u3057\u305f\u3002 (" + this.votedPlayers.size() + "/" + required + ")")), false);
        if (this.votedPlayers.size() >= required) {
            this.votedPlayers.clear();
            player.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)"\u00a7d[\u8ee2\u751f\u6295\u7968] \u00a7a\u6295\u7968\u6570\u304c\u76ee\u6a19\u306b\u9054\u3057\u307e\u3057\u305f\uff01\u8ee2\u751f\u3092\u958b\u59cb\u3057\u307e\u3059\u3002"), false);
            ReincarnationHandler.executeReincarnation(player.getServer());
        }
    }

    public static class PlayerState {
        public long startTick = -1L;
        public boolean cleared = false;
        public boolean pendingClear = false;
        public int accumulatedItems = 0;
        public int accumulatedXp = 0;
        public double distance = 0.0;
        public int mobKills = 0;
        public int bossKills = 0;
        public boolean isSpectator = false;

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("startTick", this.startTick);
            nbt.putBoolean("cleared", this.cleared);
            nbt.putBoolean("pendingClear", this.pendingClear);
            nbt.putInt("accumulatedItems", this.accumulatedItems);
            nbt.putInt("accumulatedXp", this.accumulatedXp);
            nbt.putDouble("distance", this.distance);
            nbt.putInt("mobKills", this.mobKills);
            nbt.putInt("bossKills", this.bossKills);
            nbt.putBoolean("isSpectator", this.isSpectator);
            return nbt;
        }

        public static PlayerState fromNbt(CompoundTag nbt) {
            PlayerState state = new PlayerState();
            state.startTick = nbt.getLong("startTick");
            state.cleared = nbt.getBoolean("cleared");
            state.pendingClear = nbt.getBoolean("pendingClear");
            state.accumulatedItems = nbt.getInt("accumulatedItems");
            state.accumulatedXp = nbt.getInt("accumulatedXp");
            state.distance = nbt.getDouble("distance");
            state.mobKills = nbt.getInt("mobKills");
            state.bossKills = nbt.getInt("bossKills");
            state.isSpectator = nbt.getBoolean("isSpectator");
            return state;
        }
    }
}

