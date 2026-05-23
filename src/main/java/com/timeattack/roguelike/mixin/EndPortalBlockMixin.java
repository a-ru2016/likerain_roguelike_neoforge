/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.EndPortalBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.timeattack.roguelike.mixin;

import com.timeattack.roguelike.TimeAttackRoguelike;
import com.timeattack.roguelike.data.PlayerRunState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EndPortalBlock.class})
public class EndPortalBlockMixin {
    @Inject(method={"entityInside"}, at={@At(value="HEAD")})
    private void onEndPortalEnter(BlockState state, Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!world.isClientSide() && entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            if (world.dimension() == Level.END) {
                PlayerRunState runState = PlayerRunState.get((ServerLevel)world);
                PlayerRunState.PlayerState playerState = runState.getOrCreateState(player.getUUID());
                if (!playerState.cleared && !playerState.pendingClear) {
                    playerState.pendingClear = true;
                    runState.markDirtyAndSave();
                    TimeAttackRoguelike.LOGGER.info("Player {} entered End exit portal, pending clear.", player.getName().getString());
                }
            }
        }
    }
}

