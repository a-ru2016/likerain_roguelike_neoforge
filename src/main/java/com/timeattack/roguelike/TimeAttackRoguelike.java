package com.timeattack.roguelike;

import com.timeattack.roguelike.network.*;
import com.timeattack.roguelike.event.RunEndHandler;
import com.timeattack.roguelike.data.CarryoverData;
import com.timeattack.roguelike.data.CarryoverStorage;
import com.timeattack.roguelike.data.PlayerRunState;
import com.timeattack.roguelike.config.ModConfig;
import com.timeattack.roguelike.util.StarterKitParser;
import com.timeattack.roguelike.gui.CarryoverSelectScreenHandler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod(TimeAttackRoguelike.MOD_ID)
public class TimeAttackRoguelike {
    public static final String MOD_ID = "timeattackroguelike";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ScreenHandlerType → MenuType (DeferredRegister)
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CarryoverSelectScreenHandler>> CARRYOVER_SCREEN_HANDLER =
            MENU_TYPES.register("carryover_select", () ->
                    new MenuType<>(CarryoverSelectScreenHandler::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private static final Map<UUID, Integer> pendingHandouts = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> previousDistances = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> distanceAccumulators = new ConcurrentHashMap<>();
    private static final Set<UUID> pendingMasterBonusSelect = ConcurrentHashMap.newKeySet();

    public TimeAttackRoguelike(IEventBus modEventBus) {
        LOGGER.info("Time Attack Roguelike Initializing...");

        // MenuTypeを登録
        MENU_TYPES.register(modEventBus);

        // パケット登録イベント
        modEventBus.addListener(this::registerPayloads);

        // ゲームイベントをNeoForge EVENT_BUSに登録
        NeoForge.EVENT_BUS.register(this);
    }

    // ========== ネットワーク登録 ==========

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MOD_ID);

        // S2C: サーバー→クライアント（キット選択画面を開く）
        registrar.playToClient(
                OpenKitSelectPayload.TYPE,
                OpenKitSelectPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() ->
                            TimeAttackRoguelikeClient.handleOpenKitSelect(payload));
                }
        );

        // S2C: サーバー→クライアント（達人ボーナス選択画面を開く）
        registrar.playToClient(
                OpenMasterBonusSelectPayload.TYPE,
                OpenMasterBonusSelectPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() ->
                            TimeAttackRoguelikeClient.handleOpenMasterBonusSelect(payload));
                }
        );

        // C2S: クライアント→サーバー
        registrar.playToServer(
                CarryoverSelectPayload.TYPE,
                CarryoverSelectPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        CarryoverData data = CarryoverStorage.load(player.getUUID());
                        String kitName = data.getLockedStarterKit();
                        boolean updated = false;
                        for (String item : payload.selectedItems()) {
                            if (data.getAvailablePoints() >= 1.0) {
                                data.addExtraStarterItem(kitName, item);
                                data.setAvailablePoints(data.getAvailablePoints() - 1.0);
                                updated = true;
                            }
                        }
                        if (updated) {
                            CarryoverStorage.save(data);
                            if (player instanceof ServerPlayer serverPlayer) {
                                ModConfig config = ModConfig.load();
                                RunEndHandler.checkAndGrantMasterAchievement(serverPlayer, data, config);
                                RunEndHandler.openCarryoverSelectScreen(serverPlayer);
                            }
                        }
                    });
                }
        );

        registrar.playToServer(
                OpenCarryoverScreenPayload.TYPE,
                OpenCarryoverScreenPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            RunEndHandler.openCarryoverSelectScreen(serverPlayer);
                        }
                    });
                }
        );

        registrar.playToServer(
                KitSelectedPayload.TYPE,
                KitSelectedPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        CarryoverData data = CarryoverStorage.load(player.getUUID());
                        data.setLockedStarterKit(payload.selectedKit());
                        CarryoverStorage.save(data);

                        if (player instanceof ServerPlayer serverPlayer) {
                            if (hasMasterBonusToSelect(data)) {
                                pendingMasterBonusSelect.add(player.getUUID());
                                openMasterBonusSelectScreen(serverPlayer, data);
                            } else {
                                giveRogueStarterKit(player);
                            }
                        }
                    });
                }
        );

        registrar.playToServer(
                MasterBonusSelectedPayload.TYPE,
                MasterBonusSelectedPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        UUID uuid = player.getUUID();
                        pendingMasterBonusSelect.remove(uuid);

                        CarryoverData data = CarryoverStorage.load(uuid);
                        if (!payload.selectedItemLine().isEmpty()) {
                            data.setMasterBonusItemSnbt(payload.selectedItemLine());
                            data.setMasterBonusSourceKit(payload.selectedKit());
                            CarryoverStorage.save(data);
                            player.sendSystemMessage(Component.literal(
                                    "§6§l★ §e" + payload.selectedKit() + " §fの達人ボーナスアイテムをセットしました"));
                        }
                        giveRogueStarterKit(player);
                    });
                }
        );
    }

    // ========== ゲームイベントハンドラ ==========

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        boolean alreadyReceived = player.getTags().contains("received_rogue_items");

        if (!alreadyReceived) {
            CarryoverData data = CarryoverStorage.load(uuid);
            data.setLockedStarterKit("");
            data.setMasterBonusItemSnbt("");
            data.setMasterBonusSourceKit("");
            CarryoverStorage.save(data);
            pendingHandouts.put(uuid, 20);
        }
    }

    /**
     * プレイヤーがディメンション変更後（End→Overworldへ戻った時）に呼ばれる。
     * Fabric版のServerPlayerEntityMixin#onSpawnに相当する処理。
     */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerRunState runState = PlayerRunState.get(player.serverLevel());
        PlayerRunState.PlayerState state = runState.getOrCreateState(player.getUUID());

        if (state.startTick == -1) {
            state.startTick = player.serverLevel().getGameTime();
            runState.markDirtyAndSave();
        }

        if (state.pendingClear) {
            state.pendingClear = false;
            CarryoverData carryover = CarryoverStorage.load(player.getUUID());
            RunEndHandler.onRunComplete(player, carryover, state);
            runState.markDirtyAndSave();
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        // プレイヤー死亡時ポイント減算
        if (event.getEntity() instanceof ServerPlayer player) {
            CarryoverData data = CarryoverStorage.load(player.getUUID());
            double beforePoints = data.getAvailablePoints();
            double deduction = Math.min(beforePoints, 0.5);
            data.setAvailablePoints(beforePoints - deduction);
            CarryoverStorage.save(data);
            if (deduction > 0) {
                player.sendSystemMessage(Component.literal(String.format("§c%.2f - %.2f", beforePoints, deduction)));
            }
        }

        // mob killでポイント付与
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Monster) {
            net.minecraft.world.damagesource.DamageSource source = event.getSource();
            if (source.getEntity() instanceof ServerPlayer player) {
                addPointsAndNotify(player, 0.02);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        net.minecraft.server.MinecraftServer server = event.getServer();

        // 移動距離ポイント (毎20tick)
        if (server.getTickCount() % 20 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                var stats = player.getStats();

                long currentTotalCm =
                        (long) stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.WALK_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.SPRINT_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.CROUCH_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.SWIM_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.FALL_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.CLIMB_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.FLY_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.WALK_ON_WATER_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.WALK_UNDER_WATER_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.BOAT_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.HORSE_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MINECART_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PIG_ONE_CM)) +
                        stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.STRIDER_ONE_CM));

                long prev = previousDistances.getOrDefault(uuid, currentTotalCm);
                long diff = currentTotalCm - prev;
                if (diff > 0) {
                    long accum = distanceAccumulators.getOrDefault(uuid, 0L) + diff;
                    if (accum >= 50000) {
                        long sets = accum / 50000;
                        accum %= 50000;
                        distanceAccumulators.put(uuid, accum);
                        addPointsAndNotify(player, sets * 0.1);
                    } else {
                        distanceAccumulators.put(uuid, accum);
                    }
                }
                previousDistances.put(uuid, currentTotalCm);
            }
        }

        // 遅延アイテム配布カウントダウン
        for (Map.Entry<UUID, Integer> entry : pendingHandouts.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = server.getPlayerList().getPlayer(uuid);

            if (player != null && player.containerMenu != player.inventoryMenu) {
                continue;
            }

            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                pendingHandouts.remove(uuid);
                if (player != null) {
                    giveRogueStarterKit(player);
                }
            } else {
                pendingHandouts.put(uuid, ticksLeft);
            }
        }
    }

    // ========== ユーティリティメソッド ==========

    public static void addPointsAndNotify(ServerPlayer player, double basePoints) {
        addPointsAndNotify(player, basePoints, null);
    }

    public static void addPointsAndNotify(ServerPlayer player, double basePoints, CarryoverData data) {
        int playerCount = player.getServer().getPlayerCount();
        ModConfig config = ModConfig.load();
        double multiplier;
        if (playerCount <= 1) {
            multiplier = config.pointMultiplierSolo;
        } else {
            int index = playerCount - 2;
            if (index >= 0 && index < config.pointMultipliersMulti.size()) {
                multiplier = config.pointMultipliersMulti.get(index);
            } else {
                multiplier = config.pointMultiplierMin;
            }
        }

        boolean isJackpot = Math.random() < 0.002;
        double jackpotMultiplier = isJackpot ? 100.0 : 1.0;

        double earned = basePoints * multiplier * jackpotMultiplier;

        if (data == null) {
            data = CarryoverStorage.load(player.getUUID());
        }
        double beforePoints = data.getAvailablePoints();
        double afterPoints = beforePoints + earned;
        data.setAvailablePoints(afterPoints);
        CarryoverStorage.save(data);

        if (isJackpot) {
            player.sendSystemMessage(Component.literal("§6§lJACKPOT!! §eポイント獲得量 10倍！ (x10.0)"), true);

            ServerLevel world = player.serverLevel();
            world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(),
                    100, 0.7, 0.7, 0.7, 0.5);
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
                    50, 0.7, 0.7, 0.7, 0.2);
            world.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(),
                    80, 0.5, 0.5, 0.5, 1.0);
            world.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.1, 0.1, 0.1, 0.0);
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 0.5, 0.5, 2.0);

            for (int i = 0; i < 5; i++) {
                float pitch = 1.0f + (i * 0.2f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, pitch);
            }
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 2.0f);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
        }

        player.sendSystemMessage(
                Component.literal(String.format("§b%.2f + %.2f%s", beforePoints, earned, isJackpot ? " §6§l(JACKPOT!)" : "")));
    }

    private static boolean hasMasterBonusToSelect(CarryoverData data) {
        return !data.getMasterKits().isEmpty();
    }

    public static void openMasterBonusSelectScreen(ServerPlayer player, CarryoverData data) {
        ModConfig config = ModConfig.load();
        List<String> masterKitNames = new ArrayList<>(data.getMasterKits());
        List<List<String>> masterKitItemLines = new ArrayList<>();

        for (String kitName : masterKitNames) {
            java.util.Map<String, String> poolItems = StarterKitParser.getPoolKitItems(kitName);
            if (poolItems.isEmpty()) {
                List<String> configPool = config.kitPoolItems.getOrDefault(kitName, new ArrayList<>());
                for (int i = 0; i < configPool.size(); i++) {
                    poolItems.put(String.valueOf(i), configPool.get(i));
                }
            }

            List<String> carryoverItems = data.getCarryoverItems();
            Set<String> carryoverSet = new HashSet<>(carryoverItems);

            List<String> eligibleLines = new ArrayList<>();
            for (String line : poolItems.values()) {
                if (!carryoverSet.contains(line)) {
                    eligibleLines.add(line);
                }
            }
            masterKitItemLines.add(eligibleLines);
        }

        PacketDistributor.sendToPlayer(player, new OpenMasterBonusSelectPayload(masterKitNames, masterKitItemLines));
        player.sendSystemMessage(Component.literal("§6§l★ 達人ボーナス: §f引き継ぐアイテムを選択してください"));
    }

    public static void giveRogueStarterKit(Player player) {
        if (player.level().isClientSide()) return;

        CarryoverData data = CarryoverStorage.load(player.getUUID());

        String kitName = data.getLockedStarterKit();
        ModConfig config = ModConfig.load();
        if (kitName.isEmpty()) {
            List<String> kits = StarterKitParser.getAvailableKits();
            if (kits.isEmpty()) {
                kits = new ArrayList<>(config.kitInitialItems.keySet());
            }
            if (kits.size() > 1) {
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new OpenKitSelectPayload(kits));
                }
                return;
            } else if (!kits.isEmpty()) {
                kitName = kits.get(0);
                data.setLockedStarterKit(kitName);
                CarryoverStorage.save(data);
            } else {
                kitName = "Default";
            }
        }

        if (player.getTags().contains("received_rogue_items")) {
            return;
        }

        java.util.Map<String, String> itemsToGive = new java.util.LinkedHashMap<>();

        java.util.Map<String, String> initials = StarterKitParser.getInitialKitItems(kitName);
        if (initials.isEmpty()) {
            List<String> list = config.kitInitialItems.get(kitName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    initials.put(String.valueOf(i), list.get(i));
                }
            }
        }

        if (initials != null && !initials.isEmpty()) {
            itemsToGive.putAll(initials);
        } else if (config.kitInitialItems.containsKey("Default")) {
            List<String> list = config.kitInitialItems.get("Default");
            for (int i = 0; i < list.size(); i++) {
                itemsToGive.put(String.valueOf(i), list.get(i));
            }
        }

        RegistryAccess registries = player.level().registryAccess();
        int extraSlot = 35;

        // 獲得した永続アイテム（追加スターター、持ち越しアイテム、達人ボーナス）の収集
        List<String> permanentItems = new ArrayList<>();
        permanentItems.addAll(data.getExtraStarterItems(kitName));
        for (String itemSnbt : data.getCarryoverItems()) {
            if (itemSnbt != null && !itemSnbt.isEmpty()) {
                permanentItems.add(itemSnbt);
            }
        }
        String masterBonusSnbt = data.getMasterBonusItemSnbt();
        if (masterBonusSnbt != null && !masterBonusSnbt.isEmpty()) {
            permanentItems.add(masterBonusSnbt);
            LOGGER.info("Adding master bonus item from kit '{}': {}", data.getMasterBonusSourceKit(), masterBonusSnbt);
        }

        // 獲得した永続アイテムの配布処理（Endless Inventory への追加を優先）
        for (String itemIdOrSnbt : permanentItems) {
            ItemStack stack = StarterKitParser.parseItem(itemIdOrSnbt, registries);
            if (!stack.isEmpty()) {
                boolean placed = false;
                if (com.timeattack.roguelike.util.EndlessInventoryCompat.isAvailable()) {
                    placed = com.timeattack.roguelike.util.EndlessInventoryCompat.addToEndlessInventory(player, stack);
                    if (placed) {
                        LOGGER.info("Added permanent item to Endless Inventory for player {}: {}", player.getName().getString(), stack);
                    }
                }
                if (!placed) {
                    itemsToGive.put("extra_" + extraSlot--, itemIdOrSnbt);
                }
            }
        }

        for (java.util.Map.Entry<String, String> entry : itemsToGive.entrySet()) {
            String slotKey = entry.getKey();
            String itemIdOrSnbt = entry.getValue();

            ItemStack stack = StarterKitParser.parseItem(itemIdOrSnbt, registries);
            if (!stack.isEmpty()) {
                boolean insertedDirectly = false;
                try {
                    var inv = player.getInventory();
                    if (slotKey.equals("head") || slotKey.equals("helmet")) {
                        inv.armor.set(3, stack); insertedDirectly = true;
                    } else if (slotKey.equals("chest") || slotKey.equals("chestplate")) {
                        inv.armor.set(2, stack); insertedDirectly = true;
                    } else if (slotKey.equals("legs") || slotKey.equals("leggings")) {
                        inv.armor.set(1, stack); insertedDirectly = true;
                    } else if (slotKey.equals("feet") || slotKey.equals("boots")) {
                        inv.armor.set(0, stack); insertedDirectly = true;
                    } else if (slotKey.equals("offhand") || slotKey.equals("shield")) {
                        inv.offhand.set(0, stack); insertedDirectly = true;
                    } else if (slotKey.startsWith("extra_")) {
                        if (!inv.add(stack)) {
                            player.drop(stack, false);
                        }
                        insertedDirectly = true;
                    } else {
                        int slot = Integer.parseInt(slotKey);
                        if (slot >= 0 && slot < inv.items.size()) {
                            inv.items.set(slot, stack); insertedDirectly = true;
                        } else {
                            if (!inv.add(stack)) { player.drop(stack, false); }
                            insertedDirectly = true;
                        }
                    }
                } catch (NumberFormatException e) {
                    if (!player.getInventory().add(stack)) { player.drop(stack, false); }
                    insertedDirectly = true;
                }

                if (!insertedDirectly && !stack.isEmpty()) {
                    player.drop(stack, false);
                }
            } else {
                LOGGER.error("Failed to parse and give item {} for kit {}", itemIdOrSnbt, kitName);
            }
        }
        player.addTag("received_rogue_items");
        LOGGER.info("Gave rogue starter kit '{}' to {} (after delay)", kitName, player.getName().getString());
    }
}
