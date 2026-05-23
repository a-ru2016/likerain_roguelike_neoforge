/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.likerain.roguelike.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndlessInventoryCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndlessInventoryCompat.class);
    private static boolean checked = false;
    private static boolean available = false;
    private static Class<?> serverLevelEndInvClass;
    private static Class<?> endlessInventoryClass;
    private static Method getEndInvForPlayerMethod;
    private static Method addItemMethod;
    private static Method setChangedMethod;
    private static Method clearContentMethod;
    private static Method snapshotItemsMethod;

    private static void init() {
        if (checked) {
            return;
        }
        checked = true;
        try {
            serverLevelEndInvClass = Class.forName("com.kwwsyk.endinv.common.ServerLevelEndInv");
            endlessInventoryClass = Class.forName("com.kwwsyk.endinv.common.EndlessInventory");
            getEndInvForPlayerMethod = serverLevelEndInvClass.getMethod("getEndInvForPlayer", Player.class);
            addItemMethod = endlessInventoryClass.getMethod("addItem", ItemStack.class);
            setChangedMethod = endlessInventoryClass.getMethod("setChanged", new Class[0]);
            try {
                clearContentMethod = endlessInventoryClass.getMethod("clearContent", new Class[0]);
            }
            catch (NoSuchMethodException e) {
                try {
                    clearContentMethod = endlessInventoryClass.getMethod("clear", new Class[0]);
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    // empty catch block
                }
            }
            try {
                snapshotItemsMethod = endlessInventoryClass.getMethod("snapshotItems", new Class[0]);
            }
            catch (NoSuchMethodException e) {
                // empty catch block
            }
            available = true;
            LOGGER.info("Endless Inventory mod detected and compatibility initialized successfully.");
        }
        catch (Exception e) {
            LOGGER.info("Endless Inventory mod not detected or failed to initialize compat: " + e.getMessage());
            available = false;
        }
    }

    public static boolean isAvailable() {
        EndlessInventoryCompat.init();
        return available;
    }

    public static boolean addToEndlessInventory(Player player, ItemStack stack) {
        if (!EndlessInventoryCompat.isAvailable() || stack.isEmpty()) {
            return false;
        }
        try {
            Optional optInv = (Optional)getEndInvForPlayerMethod.invoke(null, player);
            if (optInv != null && optInv.isPresent()) {
                Object endlessInventory = optInv.get();
                ItemStack remain = (ItemStack)addItemMethod.invoke(endlessInventory, stack);
                setChangedMethod.invoke(endlessInventory, new Object[0]);
                if (remain == null || remain.isEmpty()) {
                    return true;
                }
                stack.setCount(remain.getCount());
                return false;
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to add item to Endless Inventory", (Throwable)e);
        }
        return false;
    }

    public static List<ItemStack> getEndlessInventoryItems(Player player) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        if (!EndlessInventoryCompat.isAvailable()) {
            return items;
        }
        try {
            Optional optInv = (Optional)getEndInvForPlayerMethod.invoke(null, player);
            if (optInv != null && optInv.isPresent()) {
                Object endlessInventory = optInv.get();
                if (snapshotItemsMethod != null) {
                    Object res = snapshotItemsMethod.invoke(endlessInventory, new Object[0]);
                    if (res instanceof List) {
                        for (Object obj : (List)res) {
                            ItemStack stack;
                            if (!(obj instanceof ItemStack) || (stack = (ItemStack)obj).isEmpty()) continue;
                            items.add(stack);
                        }
                    }
                } else if (endlessInventory instanceof Container) {
                    Container container = (Container)endlessInventory;
                    for (int i = 0; i < container.getContainerSize(); ++i) {
                        ItemStack stack = container.getItem(i);
                        if (stack.isEmpty()) continue;
                        items.add(stack.copy());
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to get Endless Inventory items", (Throwable)e);
        }
        return items;
    }

    public static void clearEndlessInventory(Player player) {
        if (!EndlessInventoryCompat.isAvailable()) {
            return;
        }
        try {
            Optional optInv = (Optional)getEndInvForPlayerMethod.invoke(null, player);
            if (optInv != null && optInv.isPresent()) {
                Object endlessInventory = optInv.get();
                if (clearContentMethod != null) {
                    clearContentMethod.invoke(endlessInventory, new Object[0]);
                } else if (endlessInventory instanceof Container) {
                    Container container = (Container)endlessInventory;
                    container.clearContent();
                }
                setChangedMethod.invoke(endlessInventory, new Object[0]);
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to clear Endless Inventory", (Throwable)e);
        }
    }
}

