package com.timeattack.roguelike.mixin;

import com.timeattack.roguelike.event.RunEndHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"))
    private void onEndPortalEnter(BlockState state, Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!world.isClientSide() && entity instanceof ServerPlayer player) {
            if (world.dimension() == net.minecraft.world.level.Level.END) {
                com.timeattack.roguelike.data.PlayerRunState runState =
                        com.timeattack.roguelike.data.PlayerRunState.get((net.minecraft.server.level.ServerLevel) world);
                com.timeattack.roguelike.data.PlayerRunState.PlayerState playerState =
                        runState.getOrCreateState(player.getUUID());
                if (!playerState.cleared && !playerState.pendingClear) {
                    playerState.pendingClear = true;
                    runState.markDirtyAndSave();
                    com.timeattack.roguelike.TimeAttackRoguelike.LOGGER.info(
                            "Player {} entered End exit portal, pending clear.", player.getName().getString());
                }
            }
        }
    }
}
