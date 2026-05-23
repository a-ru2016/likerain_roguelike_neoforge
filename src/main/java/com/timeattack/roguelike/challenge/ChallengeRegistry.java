/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package com.timeattack.roguelike.challenge;

import com.timeattack.roguelike.challenge.Challenge;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public class ChallengeRegistry {
    private static final Map<ResourceLocation, Supplier<Challenge>> CHALLENGES = new HashMap<ResourceLocation, Supplier<Challenge>>();

    public static void register(ResourceLocation id, Supplier<Challenge> factory) {
        CHALLENGES.put(id, factory);
    }

    public static Map<ResourceLocation, Supplier<Challenge>> getChallenges() {
        return CHALLENGES;
    }
}

