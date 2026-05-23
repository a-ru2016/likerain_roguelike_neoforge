package com.timeattack.roguelike.challenge;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ChallengeRegistry {
    private static final Map<ResourceLocation, Supplier<Challenge>> CHALLENGES = new HashMap<>();

    public static void register(ResourceLocation id, Supplier<Challenge> factory) {
        CHALLENGES.put(id, factory);
    }

    public static Map<ResourceLocation, Supplier<Challenge>> getChallenges() {
        return CHALLENGES;
    }
}
