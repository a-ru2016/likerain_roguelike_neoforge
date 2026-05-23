package com.timeattack.roguelike.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Optional;

public class EndlessInventoryCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndlessInventoryCompat.class);

    private static boolean checked = false;
    private static boolean available = false;

    private static Class<?> serverLevelEndInvClass;
    private static Class<?> endlessInventoryClass;
    private static Method getEndInvForPlayerMethod;
    private static Method addItemMethod;
    private static Method setChangedMethod;

    private static void init() {
        if (checked) return;
        checked = true;
        try {
            serverLevelEndInvClass = Class.forName("com.kwwsyk.endinv.common.ServerLevelEndInv");
            endlessInventoryClass = Class.forName("com.kwwsyk.endinv.common.EndlessInventory");
            
            getEndInvForPlayerMethod = serverLevelEndInvClass.getMethod("getEndInvForPlayer", Player.class);
            addItemMethod = endlessInventoryClass.getMethod("addItem", ItemStack.class);
            setChangedMethod = endlessInventoryClass.getMethod("setChanged");
            
            available = true;
            LOGGER.info("Endless Inventory mod detected and compatibility initialized successfully.");
        } catch (Exception e) {
            LOGGER.info("Endless Inventory mod not detected or failed to initialize compat: " + e.getMessage());
            available = false;
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    public static boolean addToEndlessInventory(Player player, ItemStack stack) {
        if (!isAvailable() || stack.isEmpty()) return false;
        try {
            Optional<?> optInv = (Optional<?>) getEndInvForPlayerMethod.invoke(null, player);
            if (optInv != null && optInv.isPresent()) {
                Object endlessInventory = optInv.get();
                ItemStack remain = (ItemStack) addItemMethod.invoke(endlessInventory, stack);
                setChangedMethod.invoke(endlessInventory);
                
                if (remain == null || remain.isEmpty()) {
                    return true;
                } else {
                    stack.setCount(remain.getCount());
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to add item to Endless Inventory", e);
        }
        return false;
    }
}
