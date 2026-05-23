package com.timeattack.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class CarryoverStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORAGE_DIR = FMLPaths.CONFIGDIR.get().resolve("timeattackroguelike").resolve("carryover");

    public static CarryoverData load(UUID playerUuid) {
        Path file = STORAGE_DIR.resolve(playerUuid.toString() + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                CarryoverData data = GSON.fromJson(reader, CarryoverData.class);
                if (data != null) {
                    data.migrateIfNeeded();
                    return data;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new CarryoverData(playerUuid);
    }

    public static void save(CarryoverData data) {
        try {
            if (!Files.exists(STORAGE_DIR)) {
                Files.createDirectories(STORAGE_DIR);
            }
            Path file = STORAGE_DIR.resolve(data.getPlayerUuid().toString() + ".json");
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
