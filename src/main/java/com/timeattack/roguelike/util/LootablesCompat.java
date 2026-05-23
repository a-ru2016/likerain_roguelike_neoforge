/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.timeattack.roguelike.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LootablesCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(LootablesCompat.class);
    private static boolean checked = false;
    private static boolean available = false;
    private static Class<?> lootablesDataClass;
    private static Object dataInstance;
    private static Field keyMapField;
    private static Field usesMapField;
    private static Method markDirtyMethod;

    private static void init() {
        if (checked) {
            return;
        }
        checked = true;
        try {
            lootablesDataClass = Class.forName("me.fzzyhmstrs.lootables.data.LootablesData");
            try {
                Field instanceField = lootablesDataClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                dataInstance = instanceField.get(null);
            }
            catch (Exception e) {
                for (Field f : lootablesDataClass.getDeclaredFields()) {
                    if (f.getType() != lootablesDataClass || !Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    dataInstance = f.get(null);
                    break;
                }
            }
            Class<?> targetClass = lootablesDataClass;
            Class<?> usageDataClass = null;
            try {
                usageDataClass = Class.forName("me.fzzyhmstrs.lootables.data.LootablesData$UsageData");
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (usageDataClass != null) {
                Object usageInstance = null;
                for (Field f : lootablesDataClass.getDeclaredFields()) {
                    if (f.getType() != usageDataClass) continue;
                    f.setAccessible(true);
                    usageInstance = f.get(dataInstance);
                    break;
                }
                if (usageInstance != null) {
                    dataInstance = usageInstance;
                    targetClass = usageDataClass;
                }
            }
            for (Field f : targetClass.getDeclaredFields()) {
                if (!f.getName().equals("keyMap") && !f.getName().equals("usesMap")) continue;
                f.setAccessible(true);
                if (f.getName().equals("keyMap")) {
                    keyMapField = f;
                }
                if (!f.getName().equals("usesMap")) continue;
                usesMapField = f;
            }
            try {
                markDirtyMethod = lootablesDataClass.getMethod("markDirty", new Class[0]);
            }
            catch (Exception e) {
                try {
                    markDirtyMethod = lootablesDataClass.getMethod("setDirty", new Class[0]);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            available = keyMapField != null || usesMapField != null;
            LOGGER.info("Lootables compatibility initialized. Available: {}", available);
        }
        catch (Exception e) {
            LOGGER.info("Lootables mod not detected or failed to initialize compat: " + e.getMessage());
            available = false;
        }
    }

    public static boolean isAvailable() {
        LootablesCompat.init();
        return available;
    }

    public static void resetUsageData(UUID playerUuid) {
        if (!LootablesCompat.isAvailable()) {
            return;
        }
        try {
            Map map;
            boolean changed = false;
            if (keyMapField != null && (map = (Map)keyMapField.get(dataInstance)) != null) {
                if (map.containsKey(playerUuid)) {
                    map.remove(playerUuid);
                    changed = true;
                }
                if (map.containsKey(playerUuid.toString())) {
                    map.remove(playerUuid.toString());
                    changed = true;
                }
            }
            if (usesMapField != null && (map = (Map)usesMapField.get(dataInstance)) != null) {
                if (map.containsKey(playerUuid)) {
                    map.remove(playerUuid);
                    changed = true;
                }
                if (map.containsKey(playerUuid.toString())) {
                    map.remove(playerUuid.toString());
                    changed = true;
                }
            }
            if (changed && markDirtyMethod != null) {
                try {
                    markDirtyMethod.invoke(dataInstance, new Object[0]);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            LOGGER.info("Reset Lootables usage data for player: {}", playerUuid);
        }
        catch (Exception e) {
            LOGGER.error("Failed to reset Lootables usage data", (Throwable)e);
        }
    }

    static {
        dataInstance = null;
        keyMapField = null;
        usesMapField = null;
        markDirtyMethod = null;
    }
}

