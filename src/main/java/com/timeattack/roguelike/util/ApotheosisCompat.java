/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.timeattack.roguelike.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApotheosisCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApotheosisCompat.class);
    private static boolean checked = false;
    private static boolean available = false;
    private static Class<?> bossRegistryClass;
    private static Object bossRegistryInstance;
    private static Method getRandomBossMethod;
    private static Method createBossMethod;
    private static Class<?> worldTierClass;
    private static Method setWorldTierMethod;

    private static void init() {
        if (checked) {
            return;
        }
        checked = true;
        try {
            bossRegistryClass = Class.forName("dev.shadowsoffire.apotheosis.adventure.boss.BossRegistry");
            Field instanceField = bossRegistryClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            bossRegistryInstance = instanceField.get(null);
            for (Method m : bossRegistryClass.getMethods()) {
                if (!m.getName().equals("getRandomItem") || m.getParameterCount() != 1) continue;
                getRandomBossMethod = m;
                break;
            }
            Class<?> bossItemClass = Class.forName("dev.shadowsoffire.apotheosis.adventure.boss.BossItem");
            for (Method m : bossItemClass.getMethods()) {
                if (!m.getName().equals("createBoss") && !m.getName().equals("spawn")) continue;
                createBossMethod = m;
                break;
            }
            available = bossRegistryInstance != null && getRandomBossMethod != null && createBossMethod != null;
            LOGGER.info("Apotheosis Boss compatibility initialized. Available: {}", available);
        }
        catch (Exception e) {
            LOGGER.info("Apotheosis Boss mod not detected or failed to initialize compat: " + e.getMessage());
            available = false;
        }
        try {
            worldTierClass = Class.forName("dev.shadowsoffire.apotheosis.tiers.WorldTier");
            for (Method m : worldTierClass.getMethods()) {
                if (!m.getName().equals("setTier") && !m.getName().equals("setWorldTier")) continue;
                setWorldTierMethod = m;
                break;
            }
            LOGGER.info("Apotheosis WorldTier compatibility initialized.");
        }
        catch (Exception e) {
            LOGGER.info("Apotheosis WorldTier not detected: " + e.getMessage());
        }
    }

    public static boolean isAvailable() {
        ApotheosisCompat.init();
        return available;
    }

    public static void setWorldTier(ServerLevel level, int tier) {
        ApotheosisCompat.init();
        if (worldTierClass != null && setWorldTierMethod != null) {
            try {
                Class<?>[] paramTypes = setWorldTierMethod.getParameterTypes();
                if (paramTypes.length == 2) {
                    if (paramTypes[1] == Integer.TYPE || paramTypes[1] == Integer.class) {
                        setWorldTierMethod.invoke(null, level, tier);
                        LOGGER.info("Set Apotheosis WorldTier to: {}", tier);
                    } else if (paramTypes[1].isEnum()) {
                        Object[] constants = paramTypes[1].getEnumConstants();
                        if (tier >= 0 && tier < constants.length) {
                            setWorldTierMethod.invoke(null, level, constants[tier]);
                            LOGGER.info("Set Apotheosis WorldTier to: {}", constants[tier]);
                        }
                    }
                }
            }
            catch (Exception e) {
                LOGGER.error("Failed to set Apotheosis WorldTier", (Throwable)e);
            }
        }
    }

    public static void spawnBossNearPlayer(ServerPlayer player, double difficulty) {
        ApotheosisCompat.init();
        ServerLevel level = player.serverLevel();
        double angle = level.random.nextDouble() * 2.0 * Math.PI;
        double dist = 20.0 + level.random.nextDouble() * 20.0;
        int x = (int)(player.getX() + dist * Math.cos(angle));
        int z = (int)(player.getZ() + dist * Math.sin(angle));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (ApotheosisCompat.isAvailable()) {
            try {
                RandomSource randomSource = level.getRandom();
                Object bossItem = getRandomBossMethod.invoke(bossRegistryInstance, randomSource);
                if (bossItem != null) {
                    Entity bossEntity = null;
                    Class<?>[] paramTypes = createBossMethod.getParameterTypes();
                    if (paramTypes.length == 3) {
                        bossEntity = (Entity)createBossMethod.invoke(bossItem, level, pos, randomSource);
                    } else if (paramTypes.length == 4) {
                        bossEntity = (Entity)createBossMethod.invoke(bossItem, level, pos, randomSource, Float.valueOf((float)difficulty));
                    }
                    if (bossEntity != null) {
                        bossEntity.addTag("timeattack_white_night_boss");
                        LOGGER.info("Spawned Apotheosis boss: {} at {}", bossEntity.getName().getString(), pos);
                        return;
                    }
                }
            }
            catch (Exception e) {
                LOGGER.error("Failed to spawn Apotheosis boss, falling back to vanilla boss", (Throwable)e);
            }
        }
        ApotheosisCompat.spawnFallbackBoss(level, pos, difficulty);
    }

    private static void spawnFallbackBoss(ServerLevel level, BlockPos pos, double difficulty) {
        EntityType[] types = new EntityType[]{EntityType.WITHER_SKELETON, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.PIGLIN_BRUTE};
        EntityType type = types[level.random.nextInt(types.length)];
        Entity entity = type.create((Level)level);
        if (entity instanceof Mob) {
            AttributeInstance dmgAttr;
            Mob mob = (Mob)entity;
            mob.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
            mob.addTag("timeattack_white_night_boss");
            double hpMult = 3.0 + difficulty * 2.0;
            double dmgMult = 1.5 + difficulty * 0.5;
            AttributeInstance hpAttr = mob.getAttribute(Attributes.MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath((String)"timeattackroguelike", (String)"boss_hp"), hpMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                mob.setHealth(mob.getMaxHealth());
            }
            if ((dmgAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE)) != null) {
                dmgAttr.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath((String)"timeattackroguelike", (String)"boss_damage"), dmgMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100000, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100000, 0));
            level.addFreshEntity((Entity)mob);
            LOGGER.info("Spawned Fallback boss: {} at {}", mob.getType().getDescriptionId(), pos);
        }
    }

    public static void clearBosses(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!entity.getTags().contains("timeattack_white_night_boss")) continue;
            entity.discard();
        }
        LOGGER.info("Cleared all white night bosses.");
    }
}

