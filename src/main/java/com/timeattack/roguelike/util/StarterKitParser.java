/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.TagParser
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.fml.loading.FMLPaths
 */
package com.timeattack.roguelike.util;

import com.timeattack.roguelike.TimeAttackRoguelike;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.loading.FMLPaths;

public class StarterKitParser {
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("timeattackroguelike");
    private static final Path KITS_DIR = CONFIG_DIR.resolve("kits");

    public static List<String> getAvailableKits() {
        List<String> kits = new ArrayList<String>();
        if (Files.exists(CONFIG_DIR, new LinkOption[0])) {
            try {
                kits = Files.list(CONFIG_DIR).filter(p -> p.toString().endsWith(".txt")).map(p -> p.getFileName().toString().replace(".txt", "")).collect(Collectors.toList());
            }
            catch (IOException e) {
                TimeAttackRoguelike.LOGGER.error("Failed to read config directory for available kits", (Throwable)e);
            }
        }
        return kits;
    }

    public static Map<String, String> getInitialKitItems(String kitName) {
        return StarterKitParser.parseFile(CONFIG_DIR.resolve(kitName + ".txt"));
    }

    public static Map<String, String> getPoolKitItems(String kitName) {
        return StarterKitParser.parseFile(KITS_DIR.resolve(kitName + ".txt"));
    }

    private static Map<String, String> parseFile(Path kitFile) {
        LinkedHashMap<String, String> items = new LinkedHashMap<String, String>();
        if (Files.exists(kitFile, new LinkOption[0])) {
            try {
                List<String> lines = Files.readAllLines(kitFile);
                for (String line : lines) {
                    String value;
                    String[] parts;
                    if (line == null || !line.contains(":") || (parts = line.split(":", 2)).length < 2) continue;
                    String key = parts[0].trim();
                    if (key.startsWith("'") && key.endsWith("'")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    if ((value = parts[1].trim()).endsWith(",")) {
                        value = value.substring(0, value.length() - 1).trim();
                    }
                    if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (value.isEmpty()) continue;
                    items.put(key, value);
                }
            }
            catch (IOException e) {
                TimeAttackRoguelike.LOGGER.error("Failed to read kit file: " + String.valueOf(kitFile), (Throwable)e);
            }
        }
        return items;
    }

    public static ItemStack parseItem(String snbtOrId, RegistryAccess registries) {
        try {
            Item item;
            snbtOrId = snbtOrId.replace("\u2035", "'");
            if (snbtOrId.trim().startsWith("{")) {
                Item item2;
                CompoundTag nbt = TagParser.parseTag((String)snbtOrId);
                ItemStack stack = ItemStack.parseOptional((HolderLookup.Provider)registries, (CompoundTag)nbt);
                if (!stack.isEmpty()) {
                    return stack;
                }
                ResourceLocation id = ResourceLocation.tryParse((String)nbt.getString("id"));
                if (id != null && (item2 = (Item)BuiltInRegistries.ITEM.get(id)) != Items.AIR) {
                    int count = nbt.contains("count") ? nbt.getInt("count") : 1;
                    return new ItemStack((ItemLike)item2, count);
                }
                return ItemStack.EMPTY;
            }
            ResourceLocation id = ResourceLocation.tryParse((String)snbtOrId);
            if (id != null && (item = (Item)BuiltInRegistries.ITEM.get(id)) != Items.AIR) {
                return new ItemStack((ItemLike)item);
            }
        }
        catch (Exception e) {
            TimeAttackRoguelike.LOGGER.error("Failed to parse item SNBT: " + snbtOrId, (Throwable)e);
        }
        return ItemStack.EMPTY;
    }
}

