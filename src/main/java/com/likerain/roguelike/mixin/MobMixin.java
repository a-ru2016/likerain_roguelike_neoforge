package com.likerain.roguelike.mixin;

import com.likerain.roguelike.data.PlayerRunState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void onIsSunBurnTick(CallbackInfoReturnable<Boolean> cir) {
        Mob mob = (Mob) (Object) this;
        Level level = mob.level();
        if (level instanceof ServerLevel serverLevel) {
            PlayerRunState runState = PlayerRunState.get(serverLevel);
            if (runState != null && runState.gameStarted) {
                boolean isWhiteNight = runState.dayCount > 0 && runState.dayCount % 3 == 0;
                if (isWhiteNight) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
