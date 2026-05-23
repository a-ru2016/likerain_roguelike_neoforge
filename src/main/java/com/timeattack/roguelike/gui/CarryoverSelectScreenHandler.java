package com.timeattack.roguelike.gui;

import com.timeattack.roguelike.TimeAttackRoguelike;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;

public class CarryoverSelectScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private double points;
    private final String kitName;

    private final java.util.Map<Integer, String> slotSources;

    public CarryoverSelectScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(54), 0.0, new java.util.HashMap<>(), "");
    }

    public CarryoverSelectScreenHandler(int syncId, Inventory playerInventory, Container inventory, double points,
                                        java.util.Map<Integer, String> slotSources, String kitName) {
        super(TimeAttackRoguelike.CARRYOVER_SCREEN_HANDLER.get(), syncId);
        this.points = points;
        this.slotSources = slotSources;
        this.kitName = kitName != null ? kitName : "";
        checkContainerSize(inventory, 54);
        this.inventory = inventory;
        inventory.startOpen(playerInventory.player);

        // Inventory slots (6x9)
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }

        // Player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < inventory.getContainerSize()) {
            if (slotIndex < 45) {
                Slot slot = getSlot(slotIndex);
                if (slot.hasItem()) {
                    if (!player.level().isClientSide()) {
                        if (points >= 1.0 && slotSources.containsKey(slotIndex)) {
                            String sourceLine = slotSources.get(slotIndex);

                            com.timeattack.roguelike.data.CarryoverData data =
                                    com.timeattack.roguelike.data.CarryoverStorage.load(player.getUUID());
                            data.addExtraStarterItem(kitName, sourceLine);
                            points -= 1.0;
                            data.setAvailablePoints(points);
                            com.timeattack.roguelike.data.CarryoverStorage.save(data);

                            slot.set(ItemStack.EMPTY);

                            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                                com.timeattack.roguelike.config.ModConfig config =
                                        com.timeattack.roguelike.config.ModConfig.load();
                                com.timeattack.roguelike.event.RunEndHandler.checkAndGrantMasterAchievement(serverPlayer, data, config);
                            }
                        }
                    }
                }
                return;
            } else {
                Slot slot = getSlot(slotIndex);
                if (slot.hasItem() && slot.getItem().is(net.minecraft.world.item.Items.BARRIER)) {
                    if (!player.level().isClientSide()) {
                        com.timeattack.roguelike.data.CarryoverData data =
                                com.timeattack.roguelike.data.CarryoverStorage.load(player.getUUID());
                        int unlocked = data.getUnlockedCarryoverSlots();
                        if (slotIndex == 45 + unlocked) {
                            if (points >= 5.0) {
                                points -= 5.0;
                                data.setAvailablePoints(points);
                                data.setUnlockedCarryoverSlots(unlocked + 1);
                                com.timeattack.roguelike.data.CarryoverStorage.save(data);
                                slot.set(ItemStack.EMPTY);
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a引き継ぎ枠をアンロックしました！"));
                                this.points = points;
                            } else {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cポイントが足りません (5ポイント必要です)"));
                            }
                        } else {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c左から順にアンロックしてください"));
                        }
                    }
                    return;
                }
            }
        }
        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);

        if (!player.level().isClientSide()) {
            com.timeattack.roguelike.data.CarryoverData data =
                    com.timeattack.roguelike.data.CarryoverStorage.load(player.getUUID());
            java.util.List<String> carryoverItems = new java.util.ArrayList<>();
            int unlocked = data.getUnlockedCarryoverSlots();

            net.minecraft.core.RegistryAccess registries = player.level().registryAccess();
            for (int i = 0; i < 9; i++) {
                if (i < unlocked) {
                    ItemStack stack = this.inventory.getItem(45 + i);
                    if (!stack.isEmpty()) {
                        try {
                            net.minecraft.nbt.Tag nbt = stack.save(registries);
                            carryoverItems.add(nbt.toString());
                        } catch (Exception e) {
                            TimeAttackRoguelike.LOGGER.error("Failed to save carryover item", e);
                            carryoverItems.add("");
                        }
                    } else {
                        carryoverItems.add("");
                    }
                }
            }
            data.setCarryoverItems(carryoverItems);
            com.timeattack.roguelike.data.CarryoverStorage.save(data);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
