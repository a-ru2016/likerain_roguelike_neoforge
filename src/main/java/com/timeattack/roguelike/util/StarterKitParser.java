package com.timeattack.roguelike.util;

import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.RegistryAccess;
import com.timeattack.roguelike.TimeAttackRoguelike;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StarterKitParser {
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("timeattackroguelike");
    private static final Path KITS_DIR = CONFIG_DIR.resolve("kits");

    public static List<String> getAvailableKits() {
        List<String> kits = new ArrayList<>();
        if (Files.exists(CONFIG_DIR)) {
            try {
                kits = Files.list(CONFIG_DIR)
                        .filter(p -> p.toString().endsWith(".txt"))
                        .map(p -> p.getFileName().toString().replace(".txt", ""))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                TimeAttackRoguelike.LOGGER.error("Failed to read config directory for available kits", e);
            }
        }
        return kits;
    }

    public static java.util.Map<String, String> getInitialKitItems(String kitName) {
        return parseFile(CONFIG_DIR.resolve(kitName + ".txt"));
    }

    public static java.util.Map<String, String> getPoolKitItems(String kitName) {
        return parseFile(KITS_DIR.resolve(kitName + ".txt"));
    }

    private static java.util.Map<String, String> parseFile(Path kitFile) {
        java.util.Map<String, String> items = new java.util.LinkedHashMap<>();
        if (Files.exists(kitFile)) {
            try {
                List<String> lines = Files.readAllLines(kitFile);
                for (String line : lines) {
                    if (line == null || !line.contains(":")) continue;
                    String[] parts = line.split(":", 2);
                    if (parts.length < 2) continue;

                    String key = parts[0].trim();
                    if (key.startsWith("'") && key.endsWith("'")) {
                        key = key.substring(1, key.length() - 1);
                    }

                    String value = parts[1].trim();
                    if (value.endsWith(",")) {
                        value = value.substring(0, value.length() - 1).trim();
                    }
                    if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    if (!value.isEmpty()) {
                        items.put(key, value);
                    }
                }
            } catch (IOException e) {
                TimeAttackRoguelike.LOGGER.error("Failed to read kit file: " + kitFile, e);
            }
        }
        return items;
    }

    /**
     * SNBT文字列またはアイテムIDからItemStackを生成する。
     * NeoForge版: StringNbtReader → TagParser、RegistryWrapper → RegistryAccess
     */
    public static ItemStack parseItem(String snbtOrId, RegistryAccess registries) {
        try {
            snbtOrId = snbtOrId.replace("\u2035", "'");
            // Check if it's SNBT (starts with {)
            if (snbtOrId.trim().startsWith("{")) {
                CompoundTag nbt = TagParser.parseTag(snbtOrId);
                ItemStack stack = ItemStack.parseOptional(registries, nbt);
                if (!stack.isEmpty()) {
                    return stack;
                }

                // Fallback: base item without components
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(nbt.getString("id"));
                if (id != null) {
                    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                    if (item != net.minecraft.world.item.Items.AIR) {
                        int count = nbt.contains("count") ? nbt.getInt("count") : 1;
                        return new ItemStack(item, count);
                    }
                }
                return ItemStack.EMPTY;
            } else {
                // Plain item ID
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(snbtOrId);
                if (id != null) {
                    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                    if (item != net.minecraft.world.item.Items.AIR) {
                        return new ItemStack(item);
                    }
                }
            }
        } catch (Exception e) {
            TimeAttackRoguelike.LOGGER.error("Failed to parse item SNBT: " + snbtOrId, e);
        }
        return ItemStack.EMPTY;
    }
}
