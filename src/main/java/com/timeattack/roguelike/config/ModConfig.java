package com.timeattack.roguelike.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("timeattackroguelike").resolve("config.json");

    public Map<String, List<String>> kitInitialItems = new HashMap<>();
    public Map<String, List<String>> kitPoolItems = new HashMap<>();

    public double pointMultiplierSolo = 2.0;
    public List<Double> pointMultipliersMulti = new ArrayList<>(List.of(1.5, 1.4, 1.3));
    public double pointMultiplierMin = 1.0;

    public ModConfig() {
        // Default example kit initial items
        List<String> defaultItems = new ArrayList<>();
        defaultItems.add("minecraft:wooden_pickaxe");
        defaultItems.add("minecraft:bread");
        kitInitialItems.put("Default", defaultItems);

        // Default example kit pool items
        List<String> defaultPool = new ArrayList<>();
        defaultPool.add("minecraft:stone_pickaxe");
        defaultPool.add("minecraft:iron_pickaxe");
        defaultPool.add("minecraft:stone_sword");
        defaultPool.add("minecraft:bow");
        defaultPool.add("minecraft:arrow");
        defaultPool.add("minecraft:golden_apple");
        defaultPool.add("minecraft:iron_ingot");
        defaultPool.add("minecraft:torch");
        kitPoolItems.put("Default", defaultPool);
    }

    public static ModConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModConfig config = new ModConfig();
        save(config);
        return config;
    }

    public static void save(ModConfig config) {
        try {
            if (!Files.exists(CONFIG_FILE.getParent())) {
                Files.createDirectories(CONFIG_FILE.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
