package com.bettercontent.betterrpgprogression.mixin;

import com.bettercontent.betterrpgprogression.common.attribute.AirCapacityScaling;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityAirSupplyMixin {
    @Inject(method = "getMaxAirSupply", at = @At("RETURN"), cancellable = true)
    private void better_rpg_progression$increasePlayerAirCapacity(CallbackInfoReturnable<Integer> callback) {
        if ((Object) this instanceof Player player) {
            callback.setReturnValue(AirCapacityScaling.maxAir(callback.getReturnValueI(), player));
        }
    }
}
