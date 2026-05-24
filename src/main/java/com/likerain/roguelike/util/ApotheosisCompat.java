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
package com.likerain.roguelike.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

        if (trySpawnModdedBoss(level, pos, player, difficulty)) {
            return;
        }

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
                        bossEntity.addTag("likerain_white_night_boss");
                        bossEntity.setGlowingTag(true);
                        ApotheosisCompat.notifyBossSpawn(level, bossEntity, pos);
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
            mob.addTag("likerain_white_night_boss");
            double hpMult = 0.2 + difficulty * 2.31;
            double dmgMult = 0.2 + difficulty * 0.644;
            AttributeInstance hpAttr = mob.getAttribute(Attributes.MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath((String)"likerain_roguelike", (String)"boss_hp"), hpMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                mob.setHealth(mob.getMaxHealth());
            }
            if ((dmgAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE)) != null) {
                dmgAttr.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath((String)"likerain_roguelike", (String)"boss_damage"), dmgMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100000, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100000, 0));
            mob.setGlowingTag(true);
            level.addFreshEntity((Entity)mob);
            ApotheosisCompat.notifyBossSpawn(level, mob, pos);
            LOGGER.info("Spawned Fallback boss: {} at {}", mob.getType().getDescriptionId(), pos);
        }
    }

    private static void notifyBossSpawn(ServerLevel level, Entity boss, BlockPos pos) {
        for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
            double distance = sp.distanceTo(boss);
            sp.sendSystemMessage(Component.literal(
                String.format("§c[白夜のボス] %s が出現しました！ (座標: X:%d, Y:%d, Z:%d | 距離: %.0fブロック)", 
                    boss.getDisplayName().getString(), pos.getX(), pos.getY(), pos.getZ(), distance)
            ));
            sp.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.0f);
        }
    }

    public static void clearBosses(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!entity.getTags().contains("likerain_white_night_boss")) continue;
            entity.discard();
        }
        LOGGER.info("Cleared all white night bosses.");
    }

    private static boolean trySpawnModdedBoss(ServerLevel level, BlockPos pos, ServerPlayer player, double difficulty) {
        java.util.List<ResourceLocation> possibleBosses = new java.util.ArrayList<>();

        ResourceLocation testBlockFactorys = ResourceLocation.fromNamespaceAndPath("block_factorys_bosses", "yeti");
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(testBlockFactorys)) {
            possibleBosses.add(ResourceLocation.fromNamespaceAndPath("block_factorys_bosses", "yeti"));
            possibleBosses.add(ResourceLocation.fromNamespaceAndPath("block_factorys_bosses", "infernal_dragon"));
            possibleBosses.add(ResourceLocation.fromNamespaceAndPath("block_factorys_bosses", "underworld_knight"));
            possibleBosses.add(ResourceLocation.fromNamespaceAndPath("block_factorys_bosses", "sandworm"));
        }

        ResourceLocation testMowzies = ResourceLocation.fromNamespaceAndPath("mowziesmobs", "ferrous_wroughtnaut");
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(testMowzies)) {
            possibleBosses.add(ResourceLocation.fromNamespaceAndPath("mowziesmobs", "ferrous_wroughtnaut"));
            possibleBosses.add(ResourceLocation.fromNamespaceAndPath("mowziesmobs", "frostmaw"));
            possibleBosses.add(ResourceLocation.fromNamespaceAndPath("mowziesmobs", "umvuthi"));
            possibleBosses.add(ResourceLocation.fromNamespaceAndPath("mowziesmobs", "naga"));
        }

        if (possibleBosses.isEmpty()) {
            return false;
        }

        try {
            ResourceLocation select = possibleBosses.get(level.random.nextInt(possibleBosses.size()));
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(select);

            if (entityType != null) {
                Entity bossEntity = entityType.create(level);
                if (bossEntity instanceof Mob) {
                    Mob mob = (Mob) bossEntity;
                    mob.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);

                    if (select.getNamespace().equals("block_factorys_bosses")) {
                        initializeBlockFactorysBossState(mob);
                    } else if (select.getNamespace().equals("mowziesmobs")) {
                        initializeMowziesMobsBossState(mob, player);
                    }

                    mob.addTag("likerain_white_night_boss");

                    double hpMult = 0.05 + difficulty * 0.605;
                    double dmgMult = 0.1 + difficulty * 0.3;

                    net.minecraft.world.entity.ai.attributes.AttributeInstance hpAttr = mob.getAttribute(Attributes.MAX_HEALTH);
                    if (hpAttr != null) {
                        hpAttr.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("likerain_roguelike", "boss_hp"), hpMult - 1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                        mob.setHealth(mob.getMaxHealth());
                    }
                    net.minecraft.world.entity.ai.attributes.AttributeInstance dmgAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (dmgAttr != null) {
                        dmgAttr.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath("likerain_roguelike", "boss_damage"), dmgMult - 1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }

                    mob.setGlowingTag(true);

                    level.addFreshEntity(mob);

                    notifyBossSpawn(level, mob, pos);
                    LOGGER.info("Spawned modded boss: {} at {}", mob.getType().getDescriptionId(), pos);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to spawn modded boss, falling back", e);
        }
        return false;
    }

    private static void initializeMowziesMobsBossState(Mob bossEntity, ServerPlayer player) {
        try {
            Class<?> entityClass = bossEntity.getClass();
            String className = entityClass.getName();

            try {
                Method setAlwaysActive = entityClass.getMethod("setAlwaysActive", boolean.class);
                setAlwaysActive.invoke(bossEntity, true);
                LOGGER.info("Called setAlwaysActive(true) on {}", className);
            } catch (NoSuchMethodException e) {
            }

            if (className.contains("EntityFrostmaw")) {
                try {
                    Method setHasCrystal = entityClass.getMethod("setHasCrystal", boolean.class);
                    setHasCrystal.invoke(bossEntity, true);
                    LOGGER.info("Called setHasCrystal(true) on Frostmaw");
                } catch (NoSuchMethodException e) {
                }
            }

            if (className.contains("EntityUmvuthi")) {
                try {
                    Method setAngry = entityClass.getMethod("setAngry", boolean.class);
                    setAngry.invoke(bossEntity, true);
                    LOGGER.info("Called setAngry(true) on Umvuthi");
                } catch (NoSuchMethodException e) {
                }

                if (player != null) {
                    bossEntity.setTarget(player);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error setting Mowzie's Mobs boss variables via reflection", e);
        }
    }

    private static void initializeBlockFactorysBossState(Mob bossEntity) {
        try {
            Class<?> entityClass = bossEntity.getClass();
            String className = entityClass.getName();

            Field bossPhaseField = null;
            Class<?> current = entityClass;
            while (current != null && current != Object.class) {
                try {
                    bossPhaseField = current.getDeclaredField("DATA_BOSS_PHASE");
                    break;
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }

            if (bossPhaseField != null) {
                bossPhaseField.setAccessible(true);
                Object accessorObj = bossPhaseField.get(null);
                if (accessorObj instanceof EntityDataAccessor) {
                    EntityDataAccessor<Integer> dataBossPhase = (EntityDataAccessor<Integer>) accessorObj;
                    if (className.contains("InfernalDragonEntity")) {
                        bossEntity.getEntityData().set(dataBossPhase, 2);
                        LOGGER.info("Set DATA_BOSS_PHASE to 2 for Infernal Dragon");
                    } else {
                        bossEntity.getEntityData().set(dataBossPhase, 0);
                        LOGGER.info("Set DATA_BOSS_PHASE to 0");
                    }
                }
            }

            try {
                Field enragedField = entityClass.getField("DATA_IS_ENRAGED");
                enragedField.setAccessible(true);
                Object accessorObj = enragedField.get(null);
                if (accessorObj instanceof EntityDataAccessor) {
                    EntityDataAccessor<Integer> dataIsEnraged = (EntityDataAccessor<Integer>) accessorObj;
                    bossEntity.getEntityData().set(dataIsEnraged, 2);
                    LOGGER.info("Set DATA_IS_ENRAGED to 2 for Yeti");
                }
            } catch (NoSuchFieldException e) {
            }

            try {
                Field cinematicField = entityClass.getField("DATA_CINEMATIC");
                cinematicField.setAccessible(true);
                Object accessorObj = cinematicField.get(null);
                if (accessorObj instanceof EntityDataAccessor) {
                    EntityDataAccessor<Boolean> dataCinematic = (EntityDataAccessor<Boolean>) accessorObj;
                    bossEntity.getEntityData().set(dataCinematic, false);
                    LOGGER.info("Set DATA_CINEMATIC to false for Underworld Knight");
                }
            } catch (NoSuchFieldException e) {
            }

            try {
                Field hadSpawnerField = entityClass.getField("DATA_HAD_SPAWNER");
                hadSpawnerField.setAccessible(true);
                Object accessorObj1 = hadSpawnerField.get(null);
                if (accessorObj1 instanceof EntityDataAccessor) {
                    EntityDataAccessor<Boolean> dataHadSpawner = (EntityDataAccessor<Boolean>) accessorObj1;
                    bossEntity.getEntityData().set(dataHadSpawner, false);
                    LOGGER.info("Set DATA_HAD_SPAWNER to false for Infernal Dragon");
                }

                Field circlingField = entityClass.getField("DATA_IS_CIRCLING");
                circlingField.setAccessible(true);
                Object accessorObj2 = circlingField.get(null);
                if (accessorObj2 instanceof EntityDataAccessor) {
                    EntityDataAccessor<Boolean> dataIsCircling = (EntityDataAccessor<Boolean>) accessorObj2;
                    bossEntity.getEntityData().set(dataIsCircling, false);
                    LOGGER.info("Set DATA_IS_CIRCLING to false for Infernal Dragon");
                }
            } catch (NoSuchFieldException e) {
            }

        } catch (Exception e) {
            LOGGER.error("Error setting boss variables via reflection", e);
        }
    }
}

