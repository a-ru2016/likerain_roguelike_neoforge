/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.advancements.AdvancementHolder
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
 *  net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
 *  net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.PlayerAdvancements
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.component.ItemLore
 *  net.minecraft.world.level.ItemLike
 */
package com.likerain.roguelike.event;

import com.likerain.roguelike.LikerainRoguelike;
import com.likerain.roguelike.config.ModConfig;
import com.likerain.roguelike.data.CarryoverData;
import com.likerain.roguelike.data.CarryoverStorage;
import com.likerain.roguelike.data.PlayerRunState;
import com.likerain.roguelike.gui.CarryoverSelectScreenHandler;
import com.likerain.roguelike.util.StarterKitParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ItemLike;

public class RunEndHandler {
    public static void onRunComplete(ServerPlayer player, CarryoverData carryover, PlayerRunState.PlayerState state) {
        long currentTick = player.serverLevel().getGameTime();
        long clearTime = currentTick - state.startTick;
        if (state.startTick == -1L) {
            clearTime = 0L;
        }
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
                kits = new ArrayList<String>(config.kitInitialItems.keySet());
            }
            if (!kits.isEmpty()) {
                carryover.setLockedStarterKit(kits.get(0));
            }
        }
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a\u30af\u30ea\u30a2\uff01 \u00a7f\u30bf\u30a4\u30e0: " + RunEndHandler.formatTime(clearTime))));
        LikerainRoguelike.addPointsAndNotify(player, 4.0, carryover);
        RunEndHandler.checkAndGrantMasterAchievement(player, carryover, config);
        RunEndHandler.openCarryoverSelectScreen(player);
        LikerainRoguelike.LOGGER.info("Player {} cleared.", player.getName().getString());
    }

    public static void checkAndGrantMasterAchievement(ServerPlayer player, CarryoverData carryover, ModConfig config) {
        String kitName = carryover.getLockedStarterKit();
        if (kitName.isEmpty()) {
            return;
        }
        if (carryover.hasMasterKit(kitName)) {
            return;
        }
        Map<String, String> poolItems = StarterKitParser.getPoolKitItems(kitName);
        if (poolItems.isEmpty()) {
            List configPool = config.kitPoolItems.getOrDefault(kitName, new ArrayList());
            for (int i = 0; i < configPool.size(); ++i) {
                poolItems.put(String.valueOf(i), (String)configPool.get(i));
            }
        }
        if (poolItems.isEmpty()) {
            return;
        }
        List<String> earned = carryover.getExtraStarterItems(kitName);
        ArrayList<String> poolValues = new ArrayList<String>(poolItems.values());
        boolean allEarned = poolValues.stream().allMatch(line -> earned.contains(line));
        if (allEarned) {
            PlayerAdvancements tracker;
            AdvancementHolder masterAdv;
            carryover.addMasterKit(kitName);
            CarryoverStorage.save(carryover);
            if (player.getServer() != null && (masterAdv = player.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath((String)"likerain_roguelike", (String)"master"))) != null && !(tracker = player.getAdvancements()).getOrStartProgress(masterAdv).isDone()) {
                tracker.award(masterAdv, "master");
            }
            player.connection.send((Packet)new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send((Packet)new ClientboundSetTitleTextPacket((Component)Component.literal((String)("\u2605 " + kitName + " \u306e\u9054\u4eba \u2605")).withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(Boolean.valueOf(true)))));
            player.connection.send((Packet)new ClientboundSetSubtitleTextPacket((Component)Component.literal((String)"\u5168\u3066\u306e\u6c38\u7d9a\u5f37\u5316\u3092\u7fd2\u5f97\u3057\u307e\u3057\u305f\uff01").withStyle(style -> style.withColor(ChatFormatting.YELLOW))));
            player.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)("\u00a76\u00a7l\u3010\u9054\u4eba\u5b9f\u7e3e\u89e3\u9664\u3011\u00a7r \u00a7e" + player.getName().getString() + "\u00a7f \u304c \u00a76" + kitName + "\u00a7f \u306e\u9054\u4eba\u306b\u306a\u308a\u307e\u3057\u305f\uff01 \u00a77(\u5168\u6c38\u7d9a\u5f37\u5316\u7fd2\u5f97)")), false);
            player.sendSystemMessage((Component)Component.literal((String)("\u00a77\u6b21\u56de\u30ef\u30fc\u30eb\u30c9\u5165\u5834\u6642\u3001" + kitName + " \u306e\u30a2\u30a4\u30c6\u30e0\u30921\u3064\u5225\u30ad\u30c3\u30c8\u306b\u6301\u3061\u8fbc\u3081\u307e\u3059")));
            LikerainRoguelike.LOGGER.info("Player {} earned Master achievement for kit: {}", player.getName().getString(), kitName);
        } else {
            CarryoverStorage.save(carryover);
        }
    }

    public static void openCarryoverSelectScreen(ServerPlayer player) {
        RunEndHandler.openCarryoverSelectScreen(player, false);
    }

    public static void openCarryoverSelectScreen(ServerPlayer player, final boolean openedByMenu) {
        CarryoverData carryover = CarryoverStorage.load(player.getUUID());
        final double finalSlots = carryover.getAvailablePoints();
        ModConfig config = ModConfig.load();
        final String kitName = carryover.getLockedStarterKit();
        if (!kitName.isEmpty()) {
            ItemStack stack;
            HashSet<String> alreadyUnlocked = new HashSet<String>();
            List<String> initials = config.kitInitialItems.get(kitName);
            if (initials != null) {
                alreadyUnlocked.addAll(initials);
            }
            alreadyUnlocked.addAll(carryover.getExtraStarterItems(kitName));
            List<String> kitItems = new ArrayList<String>(StarterKitParser.getPoolKitItems(kitName).values());
            if (kitItems.isEmpty()) {
                kitItems = config.kitPoolItems.getOrDefault(kitName, new ArrayList());
            }
            final SimpleContainer inv = new SimpleContainer(54);
            final HashMap<Integer, String> slotSources = new HashMap<Integer, String>();
            int slotIdx = 0;
            RegistryAccess registries = player.serverLevel().registryAccess();
            for (String line : kitItems) {
                if (slotIdx >= 36) break;
                ItemStack stack2 = StarterKitParser.parseItem(line, registries);
                if (stack2.isEmpty()) continue;
                if (alreadyUnlocked.contains(line)) {
                    ItemLore lore = new ItemLore(List.of(Component.literal((String)"\u00a7a\u30a2\u30f3\u30ed\u30c3\u30af\u6e08\u307f (0\u30dd\u30a4\u30f3\u30c8\u3067\u5f15\u304d\u7d99\u304e\u53ef\u80fd)")));
                    stack2.set(DataComponents.LORE, lore);
                }
                inv.setItem(slotIdx, stack2);
                slotSources.put(slotIdx, line);
                ++slotIdx;
            }
            List<String> armorItems = carryover.getArmorCarryoverItems();
            for (int i = 0; i < 4; ++i) {
                int invIdx = 36 + i;
                if (carryover.isArmorSlotUnlocked(i)) {
                    if (i >= armorItems.size() || armorItems.get(i).isEmpty()) continue;
                    stack = StarterKitParser.parseItem(armorItems.get(i), registries);
                    inv.setItem(invIdx, stack);
                    continue;
                }
                ItemStack barrier = new ItemStack((ItemLike)Items.BARRIER);
                String part = i == 0 ? "\u982d" : (i == 1 ? "\u80f8" : (i == 2 ? "\u811a" : "\u8db3"));
                barrier.set(DataComponents.CUSTOM_NAME, Component.literal((String)("\u00a7c\u30ed\u30c3\u30af\u3055\u308c\u305f\u9632\u5177\u67a0 (" + part + ") (\u30af\u30ea\u30c3\u30af\u30671\u30dd\u30a4\u30f3\u30c8\u6d88\u8cbb)")));
                inv.setItem(invIdx, barrier);
            }
            int accIdx = 40;
            if (carryover.isAccessorySlotUnlocked()) {
                String accSnbt = carryover.getAccessoryCarryoverItem();
                if (!accSnbt.isEmpty()) {
                    stack = StarterKitParser.parseItem(accSnbt, registries);
                    inv.setItem(accIdx, stack);
                }
            } else {
                ItemStack barrier = new ItemStack((ItemLike)Items.BARRIER);
                barrier.set(DataComponents.CUSTOM_NAME, Component.literal((String)"\u00a7c\u30ed\u30c3\u30af\u3055\u308c\u305f\u30a2\u30af\u30bb\u30b5\u30ea\u30fc\u67a0 (\u30af\u30ea\u30c3\u30af\u30671\u30dd\u30a4\u30f3\u30c8\u6d88\u8cbb)"));
                inv.setItem(accIdx, barrier);
            }
            int freeIdx = 41;
            String freeSnbt = carryover.getFreeCarryoverItem();
            if (!freeSnbt.isEmpty()) {
                ItemStack stack3 = StarterKitParser.parseItem(freeSnbt, registries);
                inv.setItem(freeIdx, stack3);
            }
            for (int i = 42; i <= 44; ++i) {
                ItemStack barrier = new ItemStack((ItemLike)Items.BARRIER);
                barrier.set(DataComponents.CUSTOM_NAME, Component.literal((String)"\u00a77\u672a\u4f7f\u7528\u30b9\u30ed\u30c3\u30c8"));
                inv.setItem(i, barrier);
            }
            int unlockedSlots = carryover.getUnlockedCarryoverSlots();
            List<String> carryoverItems = carryover.getCarryoverItems();
            for (int i = 0; i < 9; ++i) {
                int invIdx = 45 + i;
                if (i < unlockedSlots) {
                    if (i >= carryoverItems.size() || carryoverItems.get(i).isEmpty()) continue;
                    ItemStack stack4 = StarterKitParser.parseItem(carryoverItems.get(i), registries);
                    inv.setItem(invIdx, stack4);
                    continue;
                }
                ItemStack barrier = new ItemStack((ItemLike)Items.BARRIER);
                barrier.set(DataComponents.CUSTOM_NAME, Component.literal((String)("\u00a7c\u30ed\u30c3\u30af\u3055\u308c\u305f\u5f15\u304d\u7d99\u304e\u67a0 (\u67a0 " + (i + 1) + ") (\u30af\u30ea\u30c3\u30af\u30671\u30dd\u30a4\u30f3\u30c8\u6d88\u8cbb)")));
                inv.setItem(invIdx, barrier);
            }

            // Sync current equipment to player inventory slots in the menu
            Inventory playerInv = player.getInventory();
            final SimpleContainer finalInv = inv;
            player.openMenu(new MenuProvider(){

                public Component getDisplayName() {
                    return Component.literal((String)"\u30a2\u30a4\u30c6\u30e0\u9078\u629e");
                }

                public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player playerEntity) {
                    // Update player inventory slots with current equipment for visual aid
                    for (int i = 0; i < 4; i++) {
                        playerInventory.setItem(36 + i, playerInv.getArmor(i));
                    }
                    playerInventory.setItem(40, playerInv.offhand.get(0));
                    
                    return new CarryoverSelectScreenHandler(syncId, playerInventory, (Container)finalInv, finalSlots, slotSources, kitName, openedByMenu);
                }

                public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
                    buffer.writeDouble(finalSlots);
                    buffer.writeInt(slotSources.size());
                    for (Map.Entry entry : slotSources.entrySet()) {
                        buffer.writeInt(((Integer)entry.getKey()).intValue());
                        buffer.writeUtf((String)entry.getValue());
                    }
                    buffer.writeUtf(kitName);
                    buffer.writeBoolean(openedByMenu);
                }
            });
        } else {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u30b9\u30bf\u30fc\u30bf\u30fc\u30ad\u30c3\u30c8\u304c\u30ed\u30c3\u30af\u3055\u308c\u3066\u3044\u307e\u305b\u3093"));
        }
    }

    private static String formatTime(long ticks) {
        long seconds = ticks / 20L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        return String.format("%02d:%02d:%02d", hours, minutes %= 60L, seconds %= 60L);
    }
}

