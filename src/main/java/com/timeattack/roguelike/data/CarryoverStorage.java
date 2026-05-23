/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.neoforged.fml.loading.FMLPaths
 */
package com.timeattack.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.timeattack.roguelike.data.CarryoverData;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.UUID;
import net.neoforged.fml.loading.FMLPaths;

public class CarryoverStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORAGE_DIR = FMLPaths.CONFIGDIR.get().resolve("timeattackroguelike").resolve("carryover");

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static CarryoverData load(UUID playerUuid) {
        Path file = STORAGE_DIR.resolve(playerUuid.toString() + ".json");
        if (!Files.exists(file, new LinkOption[0])) return new CarryoverData(playerUuid);
        try (BufferedReader reader = Files.newBufferedReader(file);){
            CarryoverData data = (CarryoverData)GSON.fromJson((Reader)reader, CarryoverData.class);
            if (data == null) return new CarryoverData(playerUuid);
            data.migrateIfNeeded();
            CarryoverData carryoverData = data;
            return carryoverData;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return new CarryoverData(playerUuid);
    }

    public static void save(CarryoverData data) {
        try {
            if (!Files.exists(STORAGE_DIR, new LinkOption[0])) {
                Files.createDirectories(STORAGE_DIR, new FileAttribute[0]);
            }
            Path file = STORAGE_DIR.resolve(data.getPlayerUuid().toString() + ".json");
            try (BufferedWriter writer = Files.newBufferedWriter(file, new OpenOption[0]);){
                GSON.toJson(data, (Appendable)writer);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

