package com.bettercontent.betterrpgprogression.mixin;

import com.bettercontent.betterrpgprogression.common.attribute.EfficiencyScaling;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Player.class)
abstract class PlayerMixin {
    @ModifyArg(
        method = "causeFoodExhaustion",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"),
        index = 0
    )
    private float better_rpg_progression$scaleActionExhaustion(float amount) {
        return EfficiencyScaling.scaleHunger(amount, (Player) (Object) this);
    }
}
