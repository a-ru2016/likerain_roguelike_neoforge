/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.level.dimension.DimensionType
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.likerain.roguelike.mixin;

import com.likerain.roguelike.data.PlayerRunState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Monster.class})
public class MonsterMixin {
    @Inject(method={"isDarkEnoughToSpawn"}, at={@At(value="HEAD")}, cancellable=true)
    private static void onIsDarkEnoughToSpawn(ServerLevelAccessor level, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        long time;
        ServerLevel serverLevel;
        PlayerRunState runState;
        if (level instanceof ServerLevel && (runState = PlayerRunState.get(serverLevel = (ServerLevel)level)) != null && runState.gameStarted && (time = serverLevel.getDayTime() % 24000L) >= 12000L) {
            DimensionType dimensionType = level.dimensionType();
            int i = dimensionType.monsterSpawnLightTest().sample(random);
            boolean canSpawn = i >= 15 || level.getBrightness(LightLayer.BLOCK, pos) <= i;
            cir.setReturnValue(canSpawn);
        }
    }
}

