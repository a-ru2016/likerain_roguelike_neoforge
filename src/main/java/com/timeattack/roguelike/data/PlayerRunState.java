package com.timeattack.roguelike.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NeoForge版: SavedData の save / load 形式を 1.21.1 仕様にアップデート。
 */
public class PlayerRunState extends SavedData {

    private final Map<UUID, PlayerState> playerStates = new HashMap<>();

    public static class PlayerState {
        public long startTick = -1;
        public boolean cleared = false;
        public boolean pendingClear = false;

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("startTick", startTick);
            nbt.putBoolean("cleared", cleared);
            nbt.putBoolean("pendingClear", pendingClear);
            return nbt;
        }

        public static PlayerState fromNbt(CompoundTag nbt) {
            PlayerState state = new PlayerState();
            state.startTick = nbt.getLong("startTick");
            state.cleared = nbt.getBoolean("cleared");
            state.pendingClear = nbt.getBoolean("pendingClear");
            return state;
        }
    }

    public static PlayerRunState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        PlayerRunState state = new PlayerRunState();
        ListTag list = nbt.getList("playerStates", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag compound = list.getCompound(i);
            UUID uuid = compound.getUUID("uuid");
            state.playerStates.put(uuid, PlayerState.fromNbt(compound.getCompound("state")));
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PlayerState> entry : playerStates.entrySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putUUID("uuid", entry.getKey());
            compound.put("state", entry.getValue().toNbt());
            list.add(compound);
        }
        nbt.put("playerStates", list);
        return nbt;
    }

    public static PlayerRunState get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PlayerRunState::new, PlayerRunState::fromNbt, null),
                "timeattack_playerrunstate"
        );
    }

    public PlayerState getOrCreateState(UUID uuid) {
        return playerStates.computeIfAbsent(uuid, k -> new PlayerState());
    }

    public void markDirtyAndSave() {
        setDirty();
    }
}
