package com.timeattack.roguelike.event;

import com.timeattack.roguelike.TimeAttackRoguelike;
import com.timeattack.roguelike.data.CarryoverData;
import com.timeattack.roguelike.data.CarryoverStorage;
import com.timeattack.roguelike.data.PlayerRunState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import com.timeattack.roguelike.gui.CarryoverSelectScreenHandler;
import com.timeattack.roguelike.util.StarterKitParser;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.timeattack.roguelike.config.ModConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunEndHandler {

    /**
     * ランクリア処理。
     * NeoForge版: Fabric版のServerPlayerEntityMixin#onSpawnから呼び出していた処理を
     * PlayerEvent.PlayerChangedDimensionEvent等から呼び出す。
     */
    public static void onRunComplete(ServerPlayer player, CarryoverData carryover, PlayerRunState.PlayerState state) {
        long currentTick = player.serverLevel().getGameTime();
        long clearTime = currentTick - state.startTick;
        if (state.startTick == -1) clearTime = 0;

        state.cleared = true;
        long bestTime = carryover.getBestClearTime();
        if (clearTime < bestTime) {
            carryover.setBestClearTime(clearTime);
        }
        CarryoverStorage.save(carryover);

        ModConfig config = ModConfig.load();

        if (carryover.getLockedStarterKit().isEmpty()) {
            List<String> kits = StarterKitParser.getAvailableKits();
            if (kits.isEmpty()) {
                kits = new java.util.ArrayList<>(config.kitInitialItems.keySet());
            }
            if (!kits.isEmpty()) {
                carryover.setLockedStarterKit(kits.get(0));
            }
        }

        player.sendSystemMessage(Component.literal("§aクリア！ §fタイム: " + formatTime(clearTime)));
        TimeAttackRoguelike.addPointsAndNotify(player, 4.0, carryover);

        checkAndGrantMasterAchievement(player, carryover, config);
        openCarryoverSelectScreen(player);

        TimeAttackRoguelike.LOGGER.info("Player {} cleared.", player.getName().getString());
    }

    /**
     * 達人実績チェック
     */
    public static void checkAndGrantMasterAchievement(ServerPlayer player, CarryoverData carryover, ModConfig config) {
        String kitName = carryover.getLockedStarterKit();
        if (kitName.isEmpty()) return;

        if (carryover.hasMasterKit(kitName)) return;

        java.util.Map<String, String> poolItems = StarterKitParser.getPoolKitItems(kitName);
        if (poolItems.isEmpty()) {
            List<String> configPool = config.kitPoolItems.getOrDefault(kitName, new java.util.ArrayList<>());
            for (int i = 0; i < configPool.size(); i++) {
                poolItems.put(String.valueOf(i), configPool.get(i));
            }
        }

        if (poolItems.isEmpty()) return;

        List<String> earned = carryover.getExtraStarterItems(kitName);
        List<String> poolValues = new java.util.ArrayList<>(poolItems.values());

        boolean allEarned = poolValues.stream().allMatch(line -> earned.contains(line));

        if (allEarned) {
            carryover.addMasterKit(kitName);
            CarryoverStorage.save(carryover);

            // 公式Advancement付与
            if (player.getServer() != null) {
                net.minecraft.advancements.AdvancementHolder masterAdv = player.getServer()
                        .getAdvancements()
                        .get(ResourceLocation.fromNamespaceAndPath(TimeAttackRoguelike.MOD_ID, "master"));
                if (masterAdv != null) {
                    net.minecraft.server.PlayerAdvancements tracker = player.getAdvancements();
                    if (!tracker.getOrStartProgress(masterAdv).isDone()) {
                        tracker.award(masterAdv, "master");
                    }
                }
            }

            // タイトル表示
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    Component.literal("★ " + kitName + " の達人 ★")
                            .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GOLD).withBold(true))
            ));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.literal("全ての永続強化を習得しました！")
                            .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.YELLOW))
            ));

            // サーバー全体チャット告知
            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(
                            "§6§l【達人実績解除】§r §e" + player.getName().getString()
                                    + "§f が §6" + kitName + "§f の達人になりました！ §7(全永続強化習得)"
                    ), false
            );
            player.sendSystemMessage(Component.literal(
                    "§7次回ワールド入場時、" + kitName + " のアイテムを1つ別キットに持ち込めます"
            ));
            TimeAttackRoguelike.LOGGER.info("Player {} earned Master achievement for kit: {}",
                    player.getName().getString(), kitName);
        } else {
            CarryoverStorage.save(carryover);
        }
    }

    public static void openCarryoverSelectScreen(ServerPlayer player) {
        CarryoverData carryover = CarryoverStorage.load(player.getUUID());
        double finalSlots = carryover.getAvailablePoints();

        ModConfig config = ModConfig.load();
        String kitName = carryover.getLockedStarterKit();
        if (!kitName.isEmpty()) {
            Set<String> alreadyUnlocked = new HashSet<>();

            List<String> initials = config.kitInitialItems.get(kitName);
            if (initials != null) alreadyUnlocked.addAll(initials);
            alreadyUnlocked.addAll(carryover.getExtraStarterItems(kitName));

            List<String> kitItems = new java.util.ArrayList<>(StarterKitParser.getPoolKitItems(kitName).values());
            if (kitItems.isEmpty()) {
                kitItems = config.kitPoolItems.getOrDefault(kitName, new java.util.ArrayList<>());
            }

            SimpleContainer inv = new SimpleContainer(54);
            java.util.Map<Integer, String> slotSources = new java.util.HashMap<>();
            int slotIdx = 0;
            RegistryAccess registries = player.serverLevel().registryAccess();

            for (String line : kitItems) {
                if (slotIdx >= 45) break;
                if (alreadyUnlocked.contains(line)) continue;

                ItemStack stack = StarterKitParser.parseItem(line, registries);
                if (!stack.isEmpty()) {
                    inv.setItem(slotIdx, stack);
                    slotSources.put(slotIdx, line);
                    slotIdx++;
                }
            }

            int unlockedSlots = carryover.getUnlockedCarryoverSlots();
            List<String> carryoverItems = carryover.getCarryoverItems();
            for (int i = 0; i < 9; i++) {
                int invIdx = 45 + i;
                if (i < unlockedSlots) {
                    if (i < carryoverItems.size() && !carryoverItems.get(i).isEmpty()) {
                        ItemStack stack = StarterKitParser.parseItem(carryoverItems.get(i), registries);
                        inv.setItem(invIdx, stack);
                    }
                } else {
                    ItemStack barrier = new ItemStack(net.minecraft.world.item.Items.BARRIER);
                    barrier.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                            Component.literal("§cロックされた引き継ぎ枠 (クリックで5ポイント消費)"));
                    inv.setItem(invIdx, barrier);
                }
            }

            player.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return Component.literal(String.format("アイテム選択 (残り: %.2f点)", finalSlots));
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                        int syncId, net.minecraft.world.entity.player.Inventory playerInv,
                        net.minecraft.world.entity.player.Player playerEntity) {
                    return new CarryoverSelectScreenHandler(syncId, playerInv, inv, finalSlots, slotSources, kitName);
                }
            });
        } else {
            player.sendSystemMessage(Component.literal("§cスターターキットがロックされていません"));
        }
    }

    private static String formatTime(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
