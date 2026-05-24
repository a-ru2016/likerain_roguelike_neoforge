/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 */
package com.likerain.roguelike.gui;

import com.likerain.roguelike.LikerainRoguelike;
import com.likerain.roguelike.config.ModConfig;
import com.likerain.roguelike.data.CarryoverData;
import com.likerain.roguelike.data.CarryoverStorage;
import com.likerain.roguelike.event.RunEndHandler;
import com.likerain.roguelike.util.AccessoriesCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CarryoverSelectScreenHandler
extends AbstractContainerMenu {
    private final Container inventory;
    private double points;
    private final String kitName;
    private final Map<Integer, String> slotSources;
    public final Player player;
    private final boolean openedByMenu;

    public CarryoverSelectScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, (Container)new SimpleContainer(54), 0.0, new HashMap<Integer, String>(), "", false);
    }

    public CarryoverSelectScreenHandler(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(syncId, playerInventory, (Container)new SimpleContainer(54), buffer.readDouble(), CarryoverSelectScreenHandler.readSlotSources(buffer), buffer.readUtf(), buffer.readBoolean());
    }

    private static Map<Integer, String> readSlotSources(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readInt();
        HashMap<Integer, String> map = new HashMap<Integer, String>(size);
        for (int i = 0; i < size; ++i) {
            map.put(buffer.readInt(), buffer.readUtf());
        }
        return map;
    }

    public double getPoints() {
        return this.points;
    }

    public String getKitName() {
        return this.kitName;
    }

    public Map<Integer, String> getSlotSources() {
        return this.slotSources;
    }

    public boolean isOpenedByMenu() {
        return this.openedByMenu;
    }

    public CarryoverSelectScreenHandler(int syncId, Inventory playerInventory, Container inventory, double points, Map<Integer, String> slotSources, String kitName, boolean openedByMenu) {
        super((MenuType)LikerainRoguelike.CARRYOVER_SCREEN_HANDLER.get(), syncId);
        int j;
        int i;
        this.points = points;
        this.slotSources = slotSources;
        this.kitName = kitName != null ? kitName : "";
        this.player = playerInventory.player;
        this.openedByMenu = openedByMenu;
        CarryoverSelectScreenHandler.checkContainerSize((Container)inventory, (int)54);
        this.inventory = inventory;
        inventory.startOpen(playerInventory.player);
        for (i = 0; i < 6; ++i) {
            for (j = 0; j < 9; ++j) {
                int index = j + i * 9;
                this.addSlot(new CarryoverSlot(inventory, index, 8 + j * 18, 18 + i * 18, index, this));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                this.addSlot(new Slot((Container)playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot((Container)playerInventory, i, 8 + i * 18, 198));
        }
    }

    public ItemStack quickMoveStack(Player player, int invSlot) {
        if (this.openedByMenu) {
            return ItemStack.EMPTY;
        }
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (invSlot < 54) {
                if (!this.moveItemStackTo(itemStack2, 54, 90, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 36, 54, false)) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (this.openedByMenu) {
            if (slotIndex >= 0 && slotIndex < 54) {
                // Allow clicking on carryover slots but handle specifically if needed
                // For now, let it pass to custom logic but block vanilla pick up
            } else {
                return; // Block interaction with player inventory when opened by menu
            }
        }
        if (slotIndex >= 0 && slotIndex < this.inventory.getContainerSize()) {
            Slot slot = this.getSlot(slotIndex);
            ItemStack clickedItem = slot.getItem();
            if (!clickedItem.isEmpty() && clickedItem.is(Items.BARRIER)) {
                if (!player.level().isClientSide()) {
                    CarryoverData data = CarryoverStorage.load(player.getUUID());
                    if (slotIndex >= 36 && slotIndex <= 39) {
                        if (this.points >= 1.0) {
                            this.points -= 1.0;
                            data.setAvailablePoints(this.points);
                            data.unlockArmorSlot(slotIndex - 36);
                            CarryoverStorage.save(data);
                            slot.set(ItemStack.EMPTY);
                            player.sendSystemMessage((Component)Component.literal((String)"\u00a7a\u9632\u5177\u5f15\u304d\u7d99\u304e\u67a0\u3092\u30a2\u30f3\u30ed\u30c3\u30af\u3057\u307e\u3057\u305f\uff01"));
                        } else {
                            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u30dd\u30a4\u30f3\u30c8\u304c\u8db3\u308a\u307e\u305b\u3093 (1\u30dd\u30a4\u30f3\u30c8\u5fc5\u8981\u3067\u3059)"));
                        }
                    } else if (slotIndex == 40) {
                        if (this.points >= 1.0) {
                            this.points -= 1.0;
                            data.setAvailablePoints(this.points);
                            data.unlockAccessorySlot();
                            CarryoverStorage.save(data);
                            slot.set(ItemStack.EMPTY);
                            player.sendSystemMessage((Component)Component.literal((String)"\u00a7a\u30a2\u30af\u30bb\u30b5\u30ea\u30fc\u5f15\u304d\u7d99\u304e\u67a0\u3092\u30a2\u30f3\u30ed\u30c3\u30af\u3057\u307e\u3057\u305f\uff01"));
                        } else {
                            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u30dd\u30a4\u30f3\u30c8\u304c\u8db3\u308a\u307e\u305b\u3093 (1\u30dd\u30a4\u30f3\u30c8\u5fc5\u8981\u3067\u3059)"));
                        }
                    } else if (slotIndex >= 45 && slotIndex <= 53) {
                        int unlocked = data.getUnlockedCarryoverSlots();
                        if (slotIndex == 45 + unlocked) {
                            if (this.points >= 1.0) {
                                this.points -= 1.0;
                                data.setAvailablePoints(this.points);
                                data.setUnlockedCarryoverSlots(unlocked + 1);
                                CarryoverStorage.save(data);
                                slot.set(ItemStack.EMPTY);
                                player.sendSystemMessage((Component)Component.literal((String)"\u00a7a\u5f15\u304d\u7d99\u304e\u67a0\u3092\u30a2\u30f3\u30ed\u30c3\u30af\u3057\u307e\u3057\u305f\uff01"));
                            } else {
                                player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u30dd\u30a4\u30f3\u30c8\u304c\u8db3\u308a\u307e\u305b\u3093 (1\u30dd\u30a4\u30f3\u30c8\u5fc5\u8981\u3067\u3059)"));
                            }
                        } else {
                            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u5de6\u304b\u3089\u9806\u306b\u30a2\u30f3\u30ed\u30c3\u30af\u3057\u3066\u304f\u3060\u3055\u3044"));
                        }
                    }
                }
                return;
            }
            if (slotIndex < 36) {
                if (slot.hasItem() && !player.level().isClientSide() && this.slotSources.containsKey(slotIndex)) {
                    String sourceLine = this.slotSources.get(slotIndex);
                    CarryoverData data = CarryoverStorage.load(player.getUUID());
                    boolean alreadyUnlocked = data.getExtraStarterItems(this.kitName).contains(sourceLine);
                    if (alreadyUnlocked || this.points >= 1.0) {
                        if (!alreadyUnlocked) {
                            data.addExtraStarterItem(this.kitName, sourceLine);
                            this.points -= 1.0;
                            data.setAvailablePoints(this.points);
                            CarryoverStorage.save(data);
                            if (player instanceof ServerPlayer) {
                                ServerPlayer serverPlayer = (ServerPlayer)player;
                                ModConfig config = ModConfig.load();
                                RunEndHandler.checkAndGrantMasterAchievement(serverPlayer, data, config);
                            }
                        }
                        ItemStack purchasedItem = clickedItem.copy();
                        purchasedItem.remove(DataComponents.LORE);
                        if (this.openedByMenu) {
                            // If opened by menu, we should probably not let them "take" it into cursor
                            // but maybe just add to their inventory if there is space?
                            // However, the request says "prevent taking items", so let's just block it or handle it safely.
                            if (player.getInventory().add(purchasedItem)) {
                                slot.set(ItemStack.EMPTY);
                            }
                        } else {
                            slot.set(ItemStack.EMPTY);
                            this.setCarried(purchasedItem);
                        }
                        this.broadcastChanges();
                    }
                }
                return;
            }
        }
        if (this.openedByMenu && actionType != ClickType.QUICK_MOVE) {
            // Block normal clicks that would pick up items
            return;
        }
        super.clicked(slotIndex, button, actionType, player);
    }

    public void saveCarryoverData() {
        if (!this.player.level().isClientSide()) {
            CarryoverData data = CarryoverStorage.load(this.player.getUUID());
            RegistryAccess registries = this.player.level().registryAccess();
            ArrayList<String> armorItems = new ArrayList<String>();
            for (int i = 0; i < 4; ++i) {
                if (data.isArmorSlotUnlocked(i)) {
                    ItemStack stack = this.inventory.getItem(36 + i);
                    if (!stack.isEmpty()) {
                        try {
                            Tag nbt = stack.save((HolderLookup.Provider)registries);
                            armorItems.add(nbt.toString());
                        }
                        catch (Exception e) {
                            LikerainRoguelike.LOGGER.error("Failed to save armor carryover item", (Throwable)e);
                            armorItems.add("");
                        }
                        continue;
                    }
                    armorItems.add("");
                    continue;
                }
                armorItems.add("");
            }
            data.setArmorCarryoverItems(armorItems);
            if (data.isAccessorySlotUnlocked()) {
                ItemStack stack = this.inventory.getItem(40);
                if (!stack.isEmpty()) {
                    try {
                        data.setAccessoryCarryoverItem(stack.save((HolderLookup.Provider)registries).toString());
                    }
                    catch (Exception e) {
                        LikerainRoguelike.LOGGER.error("Failed to save accessory carryover item", (Throwable)e);
                        data.setAccessoryCarryoverItem("");
                    }
                } else {
                    data.setAccessoryCarryoverItem("");
                }
            } else {
                data.setAccessoryCarryoverItem("");
            }
            ItemStack freeStack = this.inventory.getItem(41);
            if (!freeStack.isEmpty()) {
                try {
                    data.setFreeCarryoverItem(freeStack.save((HolderLookup.Provider)registries).toString());
                }
                catch (Exception e) {
                    LikerainRoguelike.LOGGER.error("Failed to save free carryover item", (Throwable)e);
                    data.setFreeCarryoverItem("");
                }
            } else {
                data.setFreeCarryoverItem("");
            }
            ArrayList<String> carryoverItems = new ArrayList<String>();
            int unlocked = data.getUnlockedCarryoverSlots();
            for (int i = 0; i < 9; ++i) {
                if (i < unlocked) {
                    ItemStack stack = this.inventory.getItem(45 + i);
                    if (!stack.isEmpty()) {
                        try {
                            Tag nbt = stack.save((HolderLookup.Provider)registries);
                            carryoverItems.add(nbt.toString());
                        }
                        catch (Exception e) {
                            LikerainRoguelike.LOGGER.error("Failed to save carryover item", (Throwable)e);
                            carryoverItems.add("");
                        }
                        continue;
                    }
                    carryoverItems.add("");
                    continue;
                }
                carryoverItems.add("");
            }
            data.setCarryoverItems(carryoverItems);
            CarryoverStorage.save(data);
        }
    }

    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
        this.saveCarryoverData();
    }

    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    public static class CarryoverSlot
    extends Slot {
        private final int customIndex;
        private final CarryoverSelectScreenHandler handler;

        public CarryoverSlot(Container container, int index, int x, int y, int customIndex, CarryoverSelectScreenHandler handler) {
            super(container, index, x, y);
            this.customIndex = customIndex;
            this.handler = handler;
        }

        public boolean mayPlace(ItemStack stack) {
            if (this.customIndex < 36) {
                return false;
            }
            ItemStack current = this.getItem();
            if (!current.isEmpty() && current.is(Items.BARRIER)) {
                return false;
            }
            if (this.customIndex >= 36 && this.customIndex <= 39) {
                EquipmentSlot slotType = this.handler.player.getEquipmentSlotForItem(stack);
                int expectedPart = this.customIndex - 36;
                if (expectedPart == 0) {
                    return slotType == EquipmentSlot.HEAD;
                }
                if (expectedPart == 1) {
                    return slotType == EquipmentSlot.CHEST;
                }
                if (expectedPart == 2) {
                    return slotType == EquipmentSlot.LEGS;
                }
                if (expectedPart == 3) {
                    return slotType == EquipmentSlot.FEET;
                }
                return false;
            }
            if (this.customIndex == 40) {
                return AccessoriesCompat.isAccessory(stack);
            }
            return this.customIndex < 42 || this.customIndex > 44;
        }
    }
}

