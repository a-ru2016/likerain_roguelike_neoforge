/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.stats.ServerStatsCounter
 *  net.minecraft.stats.Stats
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.MobSpawnType
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.neoforge.common.NeoForge
 *  net.neoforged.neoforge.common.extensions.IMenuTypeExtension
 *  net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent
 *  net.neoforged.neoforge.event.entity.living.LivingDeathEvent
 *  net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
 *  net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent$Post
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerRespawnEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerXpEvent$PickupXp
 *  net.neoforged.neoforge.event.tick.ServerTickEvent$Post
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
 *  net.neoforged.neoforge.network.registration.PayloadRegistrar
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.timeattack.roguelike;

import com.timeattack.roguelike.TimeAttackRoguelikeClient;
import com.timeattack.roguelike.config.ModConfig;
import com.timeattack.roguelike.data.CarryoverData;
import com.timeattack.roguelike.data.CarryoverStorage;
import com.timeattack.roguelike.data.PlayerRunState;
import com.timeattack.roguelike.event.ReincarnationHandler;
import com.timeattack.roguelike.event.RunEndHandler;
import com.timeattack.roguelike.gui.CarryoverSelectScreenHandler;
import com.timeattack.roguelike.item.ReincarnationItem;
import com.timeattack.roguelike.item.StartBookItem;
import com.timeattack.roguelike.network.CarryoverSelectPayload;
import com.timeattack.roguelike.network.OpenCarryoverScreenPayload;
import com.timeattack.roguelike.network.OpenKitSelectPayload;
import com.timeattack.roguelike.network.OpenMasterBonusSelectPayload;
import com.timeattack.roguelike.network.OpenStartGameScreenPayload;
import com.timeattack.roguelike.network.StartGamePayload;
import com.timeattack.roguelike.network.SyncRunStatePayload;
import com.timeattack.roguelike.util.AccessoriesCompat;
import com.timeattack.roguelike.util.ApotheosisCompat;
import com.timeattack.roguelike.util.EndlessInventoryCompat;
import com.timeattack.roguelike.util.StarterKitParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value="timeattackroguelike")
public class TimeAttackRoguelike {
    public static final String MOD_ID = "timeattackroguelike";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"timeattackroguelike");
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create((ResourceKey)Registries.MENU, (String)"timeattackroguelike");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create((ResourceKey)Registries.ITEM, (String)"timeattackroguelike");
    public static final DeferredHolder<MenuType<?>, MenuType<CarryoverSelectScreenHandler>> CARRYOVER_SCREEN_HANDLER = MENU_TYPES.register("carryover_select", () -> IMenuTypeExtension.create((syncId, playerInventory, buffer) -> new CarryoverSelectScreenHandler(syncId, playerInventory, buffer)));
    public static final DeferredHolder<Item, StartBookItem> START_BOOK = ITEMS.register("start_book", () -> new StartBookItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, ReincarnationItem> REINCARNATION_ITEM = ITEMS.register("reincarnation_item", () -> new ReincarnationItem(new Item.Properties().stacksTo(1)));
    private static final Map<UUID, Long> previousDistances = new ConcurrentHashMap<UUID, Long>();
    private static final Map<UUID, Long> distanceAccumulators = new ConcurrentHashMap<UUID, Long>();
    private static double dayTimeAccumulator = 0.0;
    private static long lastDayTime = 0L;
    private static int bossSpawnCooldown = 0;

    public TimeAttackRoguelike(IEventBus modEventBus) {
        LOGGER.info("Time Attack Roguelike Initializing...");
        MENU_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID);
        registrar.playToClient(OpenKitSelectPayload.TYPE, OpenKitSelectPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> TimeAttackRoguelikeClient.handleOpenKitSelect(payload)));
        registrar.playToClient(OpenMasterBonusSelectPayload.TYPE, OpenMasterBonusSelectPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> TimeAttackRoguelikeClient.handleOpenMasterBonusSelect(payload)));
        registrar.playToClient(OpenStartGameScreenPayload.TYPE, OpenStartGameScreenPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> TimeAttackRoguelikeClient.handleOpenStartGameScreen(payload)));
        registrar.playToClient(SyncRunStatePayload.TYPE, SyncRunStatePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> TimeAttackRoguelikeClient.handleSyncRunState(payload)));
        registrar.playToServer(CarryoverSelectPayload.TYPE, CarryoverSelectPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ServerPlayer serverPlayer;
            Player player = context.player();
            CarryoverData data = CarryoverStorage.load(player.getUUID());
            String kitName = data.getLockedStarterKit();
            ReincarnationHandler.invulnerablePlayers.remove(player.getUUID());
            player.setInvulnerable(false);
            boolean updated = false;
            for (String item : payload.selectedItems()) {
                if (!(data.getAvailablePoints() >= 1.0)) continue;
                data.addExtraStarterItem(kitName, item);
                data.setAvailablePoints(data.getAvailablePoints() - 1.0);
                updated = true;
            }
            if (updated) {
                CarryoverStorage.save(data);
                if (player instanceof ServerPlayer) {
                    serverPlayer = (ServerPlayer)player;
                    ModConfig config = ModConfig.load();
                    RunEndHandler.checkAndGrantMasterAchievement(serverPlayer, data, config);
                }
            }
            if (player instanceof ServerPlayer) {
                serverPlayer = (ServerPlayer)player;
                AbstractContainerMenu patt0$temp = serverPlayer.containerMenu;
                if (patt0$temp instanceof CarryoverSelectScreenHandler) {
                    CarryoverSelectScreenHandler carryoverHandler = (CarryoverSelectScreenHandler)patt0$temp;
                    carryoverHandler.saveCarryoverData();
                }
                serverPlayer.getInventory().clearContent();
                EndlessInventoryCompat.clearEndlessInventory((Player)serverPlayer);
                AccessoriesCompat.clearEquippedAccessories((Player)serverPlayer);
                serverPlayer.getTags().remove("received_rogue_items");
                TimeAttackRoguelike.giveRogueStarterKit(serverPlayer);
                ItemStack reincarnateStack = new ItemStack((ItemLike)REINCARNATION_ITEM.get());
                if (!serverPlayer.getInventory().add(reincarnateStack)) {
                    serverPlayer.drop(reincarnateStack, false);
                }
            }
        }));
        registrar.playToServer(OpenCarryoverScreenPayload.TYPE, OpenCarryoverScreenPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)patt0$temp;
                RunEndHandler.openCarryoverSelectScreen(serverPlayer);
            }
        }));
        registrar.playToServer(StartGamePayload.TYPE, StartGamePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer serverPlayer = (ServerPlayer)player;
            PlayerRunState runState = PlayerRunState.get(serverPlayer.serverLevel());
            if (runState.gameStarted) {
                return;
            }
            ItemStack bookStack = ItemStack.EMPTY;
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); ++i) {
                ItemStack stack = serverPlayer.getInventory().getItem(i);
                if (!stack.is((Item)START_BOOK.get())) continue;
                bookStack = stack;
                break;
            }
            if (!bookStack.isEmpty()) {
                bookStack.shrink(1);
            }
            serverPlayer.getInventory().clearContent();
            runState.gameStarted = true;
            runState.difficulty = payload.difficulty();
            runState.dayCount = 0;
            runState.votedPlayers.clear();
            runState.markDirtyAndSave();
            CarryoverData data = CarryoverStorage.load(player.getUUID());
            data.setLockedStarterKit(payload.selectedKit());
            CarryoverStorage.save(data);
            TimeAttackRoguelike.giveRogueStarterKit(serverPlayer);
            ItemStack reincarnateStack = new ItemStack((ItemLike)REINCARNATION_ITEM.get());
            if (!serverPlayer.getInventory().add(reincarnateStack)) {
                serverPlayer.drop(reincarnateStack, false);
            }
            for (ServerPlayer sp : serverPlayer.getServer().getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer((ServerPlayer)sp, (CustomPacketPayload)new SyncRunStatePayload(true, payload.difficulty()), (CustomPacketPayload[])new CustomPacketPayload[0]);
                sp.sendSystemMessage((Component)Component.literal((String)"\u00a7a\u00a7l\u30b2\u30fc\u30e0\u304c\u958b\u59cb\u3055\u308c\u307e\u3057\u305f\uff01"));
                sp.sendSystemMessage((Component)Component.literal((String)("\u00a7e\u521d\u671f\u30ad\u30c3\u30c8: " + payload.selectedKit() + " | \u96e3\u6613\u5ea6: " + payload.difficulty())));
            }
        }));
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        UUID uuid = player2.getUUID();
        PlayerRunState runState = PlayerRunState.get(player2.serverLevel());
        if (!runState.gameStarted) {
            player2.getInventory().clearContent();
            boolean hasBook = false;
            for (int i = 0; i < player2.getInventory().getContainerSize(); ++i) {
                if (!player2.getInventory().getItem(i).is((Item)START_BOOK.get())) continue;
                hasBook = true;
                break;
            }
            if (!hasBook) {
                ItemStack book = new ItemStack((ItemLike)START_BOOK.get());
                if (!player2.getInventory().add(book)) {
                    player2.drop(book, false);
                }
            }
        } else {
            PacketDistributor.sendToPlayer((ServerPlayer)player2, (CustomPacketPayload)new SyncRunStatePayload(true, runState.difficulty), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        PlayerRunState runState = PlayerRunState.get(player2.serverLevel());
        PlayerRunState.PlayerState state = runState.getOrCreateState(player2.getUUID());
        if (state.isSpectator) {
            player2.setGameMode(GameType.SPECTATOR);
            player2.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u591c\u9593\u306b\u6b7b\u4ea1\u3057\u305f\u305f\u3081\u3001\u671d\u304c\u6765\u308b\u304b\u8ee2\u751f\u3059\u308b\u307e\u3067\u5fa9\u6d3b\u3067\u304d\u307e\u305b\u3093\u3002"));
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingIncomingDamageEvent event) {
        ServerPlayer player;
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof ServerPlayer && ReincarnationHandler.invulnerablePlayers.contains((player = (ServerPlayer)livingEntity).getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof ServerPlayer) {
            boolean isWhiteNight;
            ServerPlayer player = (ServerPlayer)livingEntity;
            ServerLevel level = player.serverLevel();
            PlayerRunState runState = PlayerRunState.get(level);
            PlayerRunState.PlayerState state = runState.getOrCreateState(player.getUUID());
            long time = level.getDayTime() % 24000L;
            boolean isNight = time >= 12000L;
            boolean bl = isWhiteNight = runState.dayCount > 0 && runState.dayCount % 3 == 0;
            if (isNight || isWhiteNight) {
                state.isSpectator = true;
                runState.markDirtyAndSave();
                player.sendSystemMessage(Component.literal("\u00a7c\u591c\u9593\u3067\u6b7b\u4ea1\u3057\u307e\u3057\u305f\u3002\u30b9\u30da\u30af\u30c6\u30a4\u30bf\u30fc\u5316\u3057\u307e\u3059\u3002"));
            }
            boolean allDead = true;
            int onlineSurvivalCount = 0;
            for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
                PlayerRunState.PlayerState spState = runState.getOrCreateState(sp.getUUID());
                if (spState.isSpectator || sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR || sp == player) continue;
                ++onlineSurvivalCount;
            }
            if (onlineSurvivalCount == 0) {
                ReincarnationHandler.executeTotalDefeat(level.getServer());
            }
        }
        if (event.getEntity() instanceof Monster) {
            Monster monster = (Monster)event.getEntity();
            DamageSource source = event.getSource();
            Entity killer = source.getEntity();
            if (killer instanceof ServerPlayer) {
                boolean isBoss;
                ServerPlayer player = (ServerPlayer)killer;
                PlayerRunState runState = PlayerRunState.get(player.serverLevel());
                PlayerRunState.PlayerState state2 = runState.getOrCreateState(player.getUUID());
                boolean bl = isBoss = monster.getTags().contains("timeattack_white_night_boss") || monster.getType() == EntityType.WITHER || monster.getType() == EntityType.ENDER_DRAGON;
                if (isBoss) {
                    ++state2.bossKills;
                    player.sendSystemMessage(Component.literal("\u00a76\u2605 \u30dc\u30b9\u3092\u8a0e\u4f10\u3057\u307e\u3057\u305f\uff01 (\u8ee2\u751f\u6642\u306b\u9ad8\u30dd\u30a4\u30f3\u30c8)"));
                } else {
                    ++state2.mobKills;
                }
                runState.markDirtyAndSave();
            }
        }
    }

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            PlayerRunState runState = PlayerRunState.get(player2.serverLevel());
            PlayerRunState.PlayerState state = runState.getOrCreateState(player2.getUUID());
            int pickedUpCount = event.getOriginalStack().getCount() - event.getCurrentStack().getCount();
            if (pickedUpCount > 0) {
                state.accumulatedItems += pickedUpCount;
                runState.markDirtyAndSave();
            }
        }
    }

    @SubscribeEvent
    public void onXpPickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            PlayerRunState runState = PlayerRunState.get(player2.serverLevel());
            PlayerRunState.PlayerState state = runState.getOrCreateState(player2.getUUID());
            state.accumulatedXp += event.getOrb().getValue();
            runState.markDirtyAndSave();
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        PlayerRunState.PlayerState pState;
        long tickCount;
        boolean isWhiteNight;
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        PlayerRunState runState = PlayerRunState.get(overworld);
        if (!runState.gameStarted) {
            overworld.setDayTime(6000L);
            return;
        }
        if (!ReincarnationHandler.invulnerablePlayers.isEmpty()) {
            overworld.setDayTime(overworld.getDayTime() / 24000L * 24000L + 6000L);
            return;
        }
        long time = overworld.getDayTime() % 24000L;
        boolean isNight = time >= 12000L;
        boolean bl = isWhiteNight = runState.dayCount > 0 && runState.dayCount % 3 == 0;
        if (isWhiteNight && isNight) {
            overworld.setDayTime(overworld.getDayTime() / 24000L * 24000L + 12000L);
            if (++bossSpawnCooldown >= 600) {
                bossSpawnCooldown = 0;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    PlayerRunState.PlayerState pState2 = runState.getOrCreateState(player.getUUID());
                    if (pState2.isSpectator || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;
                    ApotheosisCompat.spawnBossNearPlayer(player, runState.difficulty);
                }
            }
        } else if (!isNight) {
            double bonus = (double)runState.difficulty * 0.5;
            if ((dayTimeAccumulator += bonus) >= 1.0) {
                int toAdd = (int)dayTimeAccumulator;
                dayTimeAccumulator -= (double)toAdd;
                overworld.setDayTime(overworld.getDayTime() + (long)toAdd);
            }
        } else {
            double prob = 1.0 / (1.0 + (double)runState.difficulty * 0.5);
            if (overworld.random.nextDouble() >= prob) {
                overworld.setDayTime(overworld.getDayTime() - 1L);
            }
        }
        if (isNight && !isWhiteNight) {
            ++runState.currentNightTicks;
            runState.markDirtyAndSave();
        } else if (runState.currentNightTicks > 0) {
            runState.currentNightTicks = 0;
            runState.markDirtyAndSave();
        }
        if (isNight && !isWhiteNight && (tickCount = (long)server.getTickCount()) % 100L == 0L) {
            double nightProgress = (double)runState.currentNightTicks / 12000.0;
            int baseCount = 1;
            int extraCount = (int)(nightProgress * 3.0 * (1.0 + (double)runState.difficulty * 0.5));
            int spawnCount = baseCount + extraCount;
            int maxNearby = Math.min(80, 20 + (int)(nightProgress * 20.0) + (int)((double)runState.difficulty * 5.0));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                int currentNearby;
                pState = runState.getOrCreateState(player.getUUID());
                if (pState.isSpectator || player.isSpectator() || !player.isAlive() || (currentNearby = this.countNearbyMonsters(overworld, player.blockPosition(), 48.0)) >= maxNearby) continue;
                for (int i = 0; i < spawnCount; ++i) {
                    this.spawnMonsterNearPlayer(player, overworld, runState, nightProgress);
                }
            }
        }
        long currentDayTime = overworld.getDayTime();
        long currentModTime = currentDayTime % 24000L;
        long lastModTime = lastDayTime % 24000L;
        if (lastModTime >= 23000L && currentModTime < 1000L) {
            ++runState.dayCount;
            server.getPlayerList().broadcastSystemMessage((Component)Component.literal((String)("\u00a76\u00a7l\u671d\u304c\u6765\u307e\u3057\u305f\u3002\u7d4c\u904e\u65e5\u6570: " + runState.dayCount)), false);
            ApotheosisCompat.clearBosses(overworld);
            int totalItems = 0;
            int totalXp = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                pState = runState.getOrCreateState(player.getUUID());
                if (pState.isSpectator) {
                    pState.isSpectator = false;
                    player.setGameMode(GameType.SURVIVAL);
                    BlockPos respawnPos = player.getRespawnPosition();
                    if (respawnPos == null) {
                        respawnPos = overworld.getSharedSpawnPos();
                    }
                    player.teleportTo(overworld, (double)respawnPos.getX() + 0.5, (double)respawnPos.getY() + 0.5, (double)respawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                    player.sendSystemMessage((Component)Component.literal((String)"\u00a7a\u671d\u304c\u6765\u305f\u305f\u3081\u5fa9\u6d3b\u3057\u307e\u3057\u305f\uff01"));
                }
                int itemDebuffLvl = Math.min(3, pState.accumulatedItems / 200);
                int xpDebuffLvl = Math.min(3, pState.accumulatedXp / 1000);
                if (itemDebuffLvl > 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, itemDebuffLvl - 1));
                    player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u7d2f\u7a4d\u7372\u5f97\u30a2\u30a4\u30c6\u30e0\u904e\u591a\u306b\u3088\u308a\u3001\u920d\u5316\u30c7\u30d0\u30d5\u304c\u4ed8\u4e0e\u3055\u308c\u307e\u3057\u305f\u3002"));
                }
                if (xpDebuffLvl > 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, xpDebuffLvl - 1));
                    player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u7d2f\u7a4d\u7372\u5f97\u7d4c\u9a13\u5024\u904e\u591a\u306b\u3088\u308a\u3001\u5f31\u4f53\u5316\u30c7\u30d0\u30d5\u304c\u4ed8\u4e0e\u3055\u308c\u307e\u3057\u305f\u3002"));
                }
                totalItems += pState.accumulatedItems;
                totalXp += pState.accumulatedXp;
            }
            int tier = 0;
            if (totalItems > 4000 || totalXp > 10000) {
                tier = 3;
            } else if (totalItems > 2000 || totalXp > 5000) {
                tier = 2;
            } else if (totalItems > 500 || totalXp > 1000) {
                tier = 1;
            }
            ApotheosisCompat.setWorldTier(overworld, tier);
            runState.markDirtyAndSave();
        }
        lastDayTime = currentDayTime;
        if (server.getTickCount() % 20 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                long prev;
                UUID uuid = player.getUUID();
                ServerStatsCounter stats = player.getStats();
                long currentTotalCm = (long)stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.CROUCH_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.FALL_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.CLIMB_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.WALK_ON_WATER_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.WALK_UNDER_WATER_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.BOAT_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.HORSE_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.MINECART_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.PIG_ONE_CM)) + (long)stats.getValue(Stats.CUSTOM.get(Stats.STRIDER_ONE_CM));
                long diff = currentTotalCm - (prev = previousDistances.getOrDefault(uuid, currentTotalCm).longValue());
                if (diff > 0L) {
                    PlayerRunState.PlayerState state = runState.getOrCreateState(uuid);
                    state.distance += (double)diff / 100.0;
                    runState.markDirtyAndSave();
                }
                previousDistances.put(uuid, currentTotalCm);
            }
        }
    }

    public static void giveRogueStarterKit(ServerPlayer player) {
        String freeItem;
        String accessoryItem;
        List<String> list;
        CarryoverData data = CarryoverStorage.load(player.getUUID());
        String kitName = data.getLockedStarterKit();
        ModConfig config = ModConfig.load();
        if (kitName.isEmpty()) {
            return;
        }
        LinkedHashMap<Object, String> itemsToGive = new LinkedHashMap<Object, String>();
        Map<String, String> initials = StarterKitParser.getInitialKitItems(kitName);
        if (initials.isEmpty() && (list = config.kitInitialItems.get(kitName)) != null) {
            for (int i = 0; i < list.size(); ++i) {
                initials.put(String.valueOf(i), list.get(i));
            }
        }
        if (initials != null && !initials.isEmpty()) {
            itemsToGive.putAll(initials);
        }
        RegistryAccess registries = player.level().registryAccess();
        int extraSlot = 35;
        ArrayList<String> permanentItems = new ArrayList<String>();
        for (String itemSnbt : data.getCarryoverItems()) {
            if (itemSnbt == null || itemSnbt.isEmpty()) continue;
            permanentItems.add(itemSnbt);
        }
        String masterBonusSnbt = data.getMasterBonusItemSnbt();
        if (masterBonusSnbt != null && !masterBonusSnbt.isEmpty()) {
            permanentItems.add(masterBonusSnbt);
        }
        if ((accessoryItem = data.getAccessoryCarryoverItem()) != null && !accessoryItem.isEmpty()) {
            permanentItems.add(accessoryItem);
        }
        if ((freeItem = data.getFreeCarryoverItem()) != null && !freeItem.isEmpty()) {
            permanentItems.add(freeItem);
        }
        for (String string : permanentItems) {
            ItemStack stack = StarterKitParser.parseItem(string, registries);
            if (stack.isEmpty()) continue;
            boolean placed = false;
            if (EndlessInventoryCompat.isAvailable()) {
                placed = EndlessInventoryCompat.addToEndlessInventory((Player)player, stack);
            }
            if (placed) continue;
            itemsToGive.put("extra_" + extraSlot--, string);
        }
        for (Map.Entry entry : itemsToGive.entrySet()) {
            String slotKey = (String)entry.getKey();
            String itemIdOrSnbt = (String)entry.getValue();
            ItemStack stack = StarterKitParser.parseItem(itemIdOrSnbt, registries);
            if (stack.isEmpty()) continue;
            try {
                Inventory inv = player.getInventory();
                if (slotKey.equals("head") || slotKey.equals("helmet")) {
                    inv.armor.set(3, stack);
                    continue;
                }
                if (slotKey.equals("chest") || slotKey.equals("chestplate")) {
                    inv.armor.set(2, stack);
                    continue;
                }
                if (slotKey.equals("legs") || slotKey.equals("leggings")) {
                    inv.armor.set(1, stack);
                    continue;
                }
                if (slotKey.equals("feet") || slotKey.equals("boots")) {
                    inv.armor.set(0, stack);
                    continue;
                }
                if (slotKey.equals("offhand") || slotKey.equals("shield")) {
                    inv.offhand.set(0, stack);
                    continue;
                }
                int slot = Integer.parseInt(slotKey);
                if (slot >= 0 && slot < inv.items.size()) {
                    inv.items.set(slot, stack);
                    continue;
                }
                if (inv.add(stack)) continue;
                player.drop(stack, false);
            }
            catch (NumberFormatException e) {
                if (player.getInventory().add(stack)) continue;
                player.drop(stack, false);
            }
        }
        List<String> armorCarryover = data.getArmorCarryoverItems();
        if (armorCarryover.size() >= 4) {
            Inventory inventory = player.getInventory();
            TimeAttackRoguelike.equipOrDrop(player, (ItemStack)inventory.armor.get(3), armorCarryover.get(0), 3, registries);
            TimeAttackRoguelike.equipOrDrop(player, (ItemStack)inventory.armor.get(2), armorCarryover.get(1), 2, registries);
            TimeAttackRoguelike.equipOrDrop(player, (ItemStack)inventory.armor.get(1), armorCarryover.get(2), 1, registries);
            TimeAttackRoguelike.equipOrDrop(player, (ItemStack)inventory.armor.get(0), armorCarryover.get(3), 0, registries);
        }
        player.addTag("received_rogue_items");
    }

    private static void equipOrDrop(ServerPlayer player, ItemStack currentStack, String newSnbt, int armorSlot, RegistryAccess registries) {
        if (newSnbt == null || newSnbt.isEmpty()) {
            return;
        }
        ItemStack newStack = StarterKitParser.parseItem(newSnbt, registries);
        if (newStack.isEmpty()) {
            return;
        }
        if (!currentStack.isEmpty() && !player.getInventory().add(currentStack)) {
            player.drop(currentStack, false);
        }
        player.getInventory().armor.set(armorSlot, newStack);
    }

    public static void addPointsAndNotify(ServerPlayer player, double points, CarryoverData carryover) {
        double before = carryover.getAvailablePoints();
        carryover.setAvailablePoints(before + points);
        CarryoverStorage.save(carryover);
        player.sendSystemMessage((Component)Component.literal((String)String.format("\u00a7d[\u30dd\u30a4\u30f3\u30c8\u7372\u5f97]\u00a7f \u7372\u5f97: \u00a7a+%.2f\u00a7f \u30dd\u30a4\u30f3\u30c8\uff01 (\u5408\u8a08: \u00a7e%.2f\u00a7f)", points, carryover.getAvailablePoints())));
    }

    @SubscribeEvent
    public void onMobSpawn(FinalizeSpawnEvent event) {
        AttributeInstance dmgAttr;
        AttributeInstance hpAttr;
        Mob mob = event.getEntity();
        if (!(mob instanceof Monster)) {
            return;
        }
        Monster monster = (Monster)mob;
        Level level = monster.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        PlayerRunState runState = PlayerRunState.get(level2);
        if (!runState.gameStarted) {
            return;
        }
        double diff = runState.difficulty;
        int days = runState.dayCount;
        int totalItems = 0;
        int totalXp = 0;
        for (ServerPlayer player : level2.getServer().getPlayerList().getPlayers()) {
            PlayerRunState.PlayerState pState = runState.getOrCreateState(player.getUUID());
            totalItems += pState.accumulatedItems;
            totalXp += pState.accumulatedXp;
        }
        double hpMultiplier = 1.0 + diff * 0.15 + (double)days * 0.05;
        double dmgMultiplier = 1.0 + diff * 0.1 + (double)days * 0.03;
        hpMultiplier += (double)totalItems / 2000.0 * 0.1;
        hpMultiplier += (double)totalXp / 5000.0 * 0.1;
        dmgMultiplier += (double)totalItems / 2000.0 * 0.05;
        dmgMultiplier += (double)totalXp / 5000.0 * 0.05;
        hpMultiplier = Math.min(4.0, hpMultiplier);
        dmgMultiplier = Math.min(3.0, dmgMultiplier);
        if (hpMultiplier > 1.0 && (hpAttr = monster.getAttribute(Attributes.MAX_HEALTH)) != null) {
            hpAttr.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath((String)MOD_ID, (String)"roguelike_mob_hp"), hpMultiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            monster.setHealth(monster.getMaxHealth());
        }
        if (dmgMultiplier > 1.0 && (dmgAttr = monster.getAttribute(Attributes.ATTACK_DAMAGE)) != null) {
            dmgAttr.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath((String)MOD_ID, (String)"roguelike_mob_damage"), dmgMultiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private int countNearbyMonsters(ServerLevel level, BlockPos pos, double radius) {
        AABB aabb = new AABB(pos).inflate(radius);
        List monsters = level.getEntitiesOfClass(Monster.class, aabb);
        return monsters.size();
    }

    private BlockPos findSafeSpawnPosition(ServerLevel level, BlockPos playerPos) {
        RandomSource random = level.random;
        for (int i = 0; i < 20; ++i) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double distance = 20.0 + random.nextDouble() * 20.0;
            int x = playerPos.getX() + (int)(distance * Math.cos(angle));
            int z = playerPos.getZ() + (int)(distance * Math.sin(angle));
            int startY = playerPos.getY() - 10;
            for (int dy = 0; dy < 20; ++dy) {
                int y = startY + dy;
                BlockPos testPos = new BlockPos(x, y, z);
                BlockState feetState = level.getBlockState(testPos);
                BlockState headState = level.getBlockState(testPos.above());
                BlockState groundState = level.getBlockState(testPos.below());
                if (!feetState.isAir() || !headState.isAir() || groundState.isAir() || groundState.is(Blocks.LAVA) || groundState.is(Blocks.WATER)) continue;
                return testPos;
            }
        }
        return null;
    }

    private EntityType<?> getRandomMonsterType(ServerLevel level) {
        ResourceKey dim = level.dimension();
        if (dim == Level.NETHER) {
            EntityType[] netherMonsters = new EntityType[]{EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN, EntityType.WITHER_SKELETON, EntityType.MAGMA_CUBE};
            return netherMonsters[level.random.nextInt(netherMonsters.length)];
        }
        if (dim == Level.END) {
            return EntityType.ENDERMAN;
        }
        EntityType[] overworldMonsters = new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER};
        return overworldMonsters[level.random.nextInt(overworldMonsters.length)];
    }

    private void spawnMonsterNearPlayer(ServerPlayer player, ServerLevel level, PlayerRunState runState, double nightProgress) {
        BlockPos spawnPos = this.findSafeSpawnPosition(level, player.blockPosition());
        if (spawnPos == null) {
            return;
        }
        EntityType<?> type = this.getRandomMonsterType(level);
        Entity entity = type.create((Level)level);
        if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            mob.moveTo((double)spawnPos.getX() + 0.5, (double)spawnPos.getY(), (double)spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
            mob.finalizeSpawn((ServerLevelAccessor)level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
            level.addFreshEntity((Entity)mob);
        }
    }
}

