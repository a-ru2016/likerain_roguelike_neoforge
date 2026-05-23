/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.ChestBlockEntity
 *  net.minecraft.world.level.chunk.status.ChunkStatus
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 */
package com.likerain.roguelike.event;

import com.likerain.roguelike.data.CarryoverData;
import com.likerain.roguelike.data.CarryoverStorage;
import com.likerain.roguelike.data.PlayerRunState;
import com.likerain.roguelike.event.RunEndHandler;
import com.likerain.roguelike.util.AccessoriesCompat;
import com.likerain.roguelike.util.ApotheosisCompat;
import com.likerain.roguelike.util.EndlessInventoryCompat;
import com.likerain.roguelike.util.LootablesCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

public class ReincarnationHandler {
    public static final Set<UUID> invulnerablePlayers = Collections.synchronizedSet(new HashSet());

    public static BlockPos findSafeLocation(ServerLevel level, double currentX, double currentZ) {
        return ReincarnationHandler.findSafeLocation(level, currentX, currentZ, 10000.0, 50000.0);
    }

    public static BlockPos findSafeLocation(ServerLevel level, double currentX, double currentZ, double minDist, double maxDist) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 50; ++attempt) {
            double dist = minDist + random.nextDouble() * (maxDist - minDist);
            double angle = random.nextDouble() * 2.0 * Math.PI;
            int x = (int)(currentX + dist * Math.cos(angle));
            int z = (int)(currentZ + dist * Math.sin(angle));
            level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).isAir() || level.getBlockState(pos.below()).is(Blocks.LAVA) || level.getBlockState(pos.below()).is(Blocks.WATER) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            return pos;
        }
        int x = (int)(currentX + minDist);
        int z = (int)(currentZ + minDist);
        level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new BlockPos(x, y, z);
    }

    public static void executeReincarnation(MinecraftServer server) {
        boolean wasNight;
        ServerLevel overworld = server.overworld();
        PlayerRunState runState = PlayerRunState.get(overworld);
        ServerPlayer firstPlayer = (ServerPlayer)server.getPlayerList().getPlayers().get(0);
        BlockPos targetPos = ReincarnationHandler.findSafeLocation(overworld, firstPlayer.getX(), firstPlayer.getZ());
        long time = overworld.getDayTime() % 24000L;
        boolean bl = wasNight = time >= 12000L;
        if (wasNight) {
            long currentDay = overworld.getDayTime() / 24000L;
            overworld.setDayTime(currentDay * 24000L + 6000L);
            server.getPlayerList().broadcastSystemMessage((Component)Component.literal((String)"\u00a7e\u591c\u304c\u30b9\u30ad\u30c3\u30d7\u3055\u308c\u307e\u3057\u305f\u3002\u5373\u5ea7\u306b\u663c\u306b\u306a\u308a\u307e\u3059\u3002"), false);
        }
        boolean isWhiteNight = runState.dayCount > 0 && runState.dayCount % 3 == 0;
        double whiteNightMult = isWhiteNight ? 1.5 : 1.0;
        double diffMult = 1.0 + (double)runState.difficulty * 0.5;
        ApotheosisCompat.clearBosses(overworld);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean hasDoneAnything;
            UUID uuid = player.getUUID();
            PlayerRunState.PlayerState state = runState.getOrCreateState(uuid);
            double distancePoints = state.distance / 1000.0 * 0.2;
            double mobPoints = (double)state.mobKills * 0.05;
            double bossPoints = (double)state.bossKills * 1.0;
            double survivalPoints = state.isSpectator ? 0.0 : 2.0;
            boolean bl2 = hasDoneAnything = state.distance >= 100.0 || state.mobKills > 0 || state.bossKills > 0;
            if (!hasDoneAnything) {
                survivalPoints = 0.0;
            }
            double totalBasePoints = distancePoints + mobPoints + bossPoints + survivalPoints;
            double earnedPoints = totalBasePoints * whiteNightMult * diffMult;
            CarryoverData data = CarryoverStorage.load(uuid);
            double before = data.getAvailablePoints();
            data.setAvailablePoints(before + earnedPoints);
            CarryoverStorage.save(data);
            player.sendSystemMessage((Component)Component.literal((String)String.format("\u00a7d[\u8ee2\u751f\u30dd\u30a4\u30f3\u30c8\u7cbe\u7b97]\u00a7f \u7372\u5f97: \u00a7a+%.2f\u00a7f \u30dd\u30a4\u30f3\u30c8\uff01 (\u5408\u8a08: \u00a7e%.2f\u00a7f)", earnedPoints, data.getAvailablePoints())));
            player.sendSystemMessage((Component)Component.literal((String)String.format("\u00a77(\u8a73\u7d30: \u79fb\u52d5\u8ddd\u96e2\u30dd\u30a4\u30f3\u30c8: %.2f, Mob\u8a0e\u4f10: %.2f, \u30dc\u30b9\u8a0e\u4f10: %.2f, \u751f\u5b58: %.2f) x\u767d\u591c\u500d\u7387: %.1f x\u96e3\u6613\u5ea6\u500d\u7387: %.2f", distancePoints, mobPoints, bossPoints, survivalPoints, whiteNightMult, diffMult)));
            state.distance = 0.0;
            state.mobKills = 0;
            state.bossKills = 0;
            // アクセサリーをクリア（転生時）
            AccessoriesCompat.clearEquippedAccessories((Player)player);
            LootablesCompat.resetUsageData(uuid, server);
            player.setExperienceLevels(0);
            player.setExperiencePoints(0);
            player.teleportTo(overworld, (double)targetPos.getX() + 0.5, (double)targetPos.getY() + 0.5, (double)targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            player.setRespawnPosition(overworld.dimension(), targetPos, 0.0f, true, false);
            if (state.isSpectator) {
                state.isSpectator = false;
                player.setGameMode(GameType.SURVIVAL);
                player.sendSystemMessage((Component)Component.literal((String)"\u00a7a\u8ee2\u751f\u306b\u3088\u308a\u5fa9\u6d3b\u3057\u307e\u3057\u305f\uff01"));
            }
            invulnerablePlayers.add(uuid);
            player.setInvulnerable(true);
            RunEndHandler.openCarryoverSelectScreen(player);
        }
        runState.markDirtyAndSave();
    }

    public static void executeTotalDefeat(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        PlayerRunState runState = PlayerRunState.get(overworld);
        server.getPlayerList().broadcastSystemMessage((Component)Component.literal((String)"\u00a7c\u00a7l\u6557\u5317\u3057\u307e\u3057\u305f\u3002\u30ea\u30b9\u30dd\u30fc\u30f3\u3057\u307e\u3059"), false);
        ServerPlayer firstPlayer = (ServerPlayer)server.getPlayerList().getPlayers().get(0);
        BlockPos spawnPos = overworld.getSharedSpawnPos();
        BlockPos targetPos = ReincarnationHandler.findSafeLocation(overworld, spawnPos.getX(), spawnPos.getZ(), 5000.0, 10000.0);
        BlockPos deathPos = firstPlayer.blockPosition();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.blockPosition() == null) continue;
            deathPos = player.blockPosition();
        }
        ApotheosisCompat.clearBosses(overworld);
        ArrayList<ItemStack> lostItems = new ArrayList<ItemStack>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                lostItems.add(stack.copy());
            }
            player.getInventory().clearContent();
            lostItems.addAll(EndlessInventoryCompat.getEndlessInventoryItems((Player)player));
            EndlessInventoryCompat.clearEndlessInventory((Player)player);
            lostItems.addAll(AccessoriesCompat.getEquippedAccessories((Player)player));
            AccessoriesCompat.clearEquippedAccessories((Player)player);
            LootablesCompat.resetUsageData(uuid, server);
            player.setExperienceLevels(0);
            player.setExperiencePoints(0);
            CarryoverData carryover = CarryoverStorage.load(uuid);
            carryover.resetAll();
            CarryoverStorage.save(carryover);
            PlayerRunState.PlayerState state = runState.getOrCreateState(uuid);
            state.distance = 0.0;
            state.mobKills = 0;
            state.bossKills = 0;
            state.accumulatedItems = 0;
            state.accumulatedXp = 0;
            state.isSpectator = false;
            player.teleportTo(overworld, (double)targetPos.getX() + 0.5, (double)targetPos.getY() + 0.5, (double)targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            player.setRespawnPosition(overworld.dimension(), targetPos, 0.0f, true, false);
            player.setGameMode(GameType.SURVIVAL);
            player.getTags().remove("received_rogue_items");
            ItemStack book = new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath((String)"likerain_roguelike", (String)"start_book")));
            if (player.getInventory().add(book)) continue;
            player.drop(book, false);
        }
        if (!lostItems.isEmpty()) {
            BlockPos chestPos = deathPos;
            int radius = 3;
            boolean chestPlaced = false;
            for (int dx = -radius; dx <= radius && !chestPlaced; ++dx) {
                for (int dy = -radius; dy <= radius && !chestPlaced; ++dy) {
                    for (int dz = -radius; dz <= radius && !chestPlaced; ++dz) {
                        BlockPos p = deathPos.offset(dx, dy, dz);
                        if (!overworld.getBlockState(p).isAir() || overworld.getBlockState(p.below()).isAir()) continue;
                        overworld.setBlock(p, Blocks.CHEST.defaultBlockState(), 3);
                        BlockEntity be = overworld.getBlockEntity(p);
                        if (!(be instanceof ChestBlockEntity)) continue;
                        ChestBlockEntity chest = (ChestBlockEntity)be;
                        int toKeepCount = 5 + overworld.random.nextInt(Math.min(11, lostItems.size()));
                        Collections.shuffle(lostItems);
                        for (int i = 0; i < Math.min(toKeepCount, lostItems.size()); ++i) {
                            chest.setItem(i % chest.getContainerSize(), (ItemStack)lostItems.get(i));
                        }
                        chestPlaced = true;
                    }
                }
            }
            if (chestPlaced) {
                server.getPlayerList().broadcastSystemMessage((Component)Component.literal((String)"\u00a7e\u30ed\u30b9\u30c8\u3057\u305f\u30a2\u30a4\u30c6\u30e0\u306e\u4e00\u90e8\u304c\u6b7b\u4ea1\u5730\u70b9\u306e\u30c1\u30a7\u30b9\u30c8\u306b\u4fdd\u7ba1\u3055\u308c\u307e\u3057\u305f\u3002"), false);
            }
        }
        runState.gameStarted = false;
        runState.difficulty = 0.0f;
        runState.dayCount = 0;
        runState.votedPlayers.clear();
        runState.markDirtyAndSave();
        overworld.setDayTime(6000L);
    }
}

