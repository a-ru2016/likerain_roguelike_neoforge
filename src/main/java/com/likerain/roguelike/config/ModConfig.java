/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.neoforged.fml.loading.FMLPaths
 */
package com.likerain.roguelike.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("likerain_roguelike").resolve("config.json");
    public Map<String, List<String>> kitInitialItems = new HashMap<String, List<String>>();
    public Map<String, List<String>> kitPoolItems = new HashMap<String, List<String>>();
    public double pointMultiplierSolo = 2.0;
    public List<Double> pointMultipliersMulti = new ArrayList<Double>(List.of(Double.valueOf(1.5), Double.valueOf(1.4), Double.valueOf(1.3)));
    public double pointMultiplierMin = 1.0;

    public ModConfig() {
        ArrayList<String> defaultItems = new ArrayList<String>();
        defaultItems.add("minecraft:wooden_pickaxe");
        defaultItems.add("minecraft:bread");
        this.kitInitialItems.put("Default", defaultItems);
        ArrayList<String> defaultPool = new ArrayList<String>();
        defaultPool.add("minecraft:stone_pickaxe");
        defaultPool.add("minecraft:iron_pickaxe");
        defaultPool.add("minecraft:stone_sword");
        defaultPool.add("minecraft:bow");
        defaultPool.add("minecraft:arrow");
        defaultPool.add("minecraft:golden_apple");
        defaultPool.add("minecraft:iron_ingot");
        defaultPool.add("minecraft:torch");
        this.kitPoolItems.put("Default", defaultPool);
    }

    public static ModConfig load() {
        if (Files.exists(CONFIG_FILE, new LinkOption[0])) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE)) {
                return GSON.fromJson((Reader)reader, ModConfig.class);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModConfig config = new ModConfig();
        ModConfig.save(config);
        return config;
    }

    public static void save(ModConfig config) {
        try {
            if (!Files.exists(CONFIG_FILE.getParent(), new LinkOption[0])) {
                Files.createDirectories(CONFIG_FILE.getParent(), new FileAttribute[0]);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE, new OpenOption[0]);){
                GSON.toJson(config, (Appendable)writer);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

