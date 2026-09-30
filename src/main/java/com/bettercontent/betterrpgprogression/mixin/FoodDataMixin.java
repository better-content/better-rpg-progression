package com.bettercontent.betterrpgprogression.mixin;

import com.bettercontent.betterrpgprogression.common.attribute.EfficiencyScaling;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
abstract class FoodDataMixin {
    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V")
    )
    private void better_rpg_progression$scaleRegenerationExhaustion(FoodData foodData, float amount, Player player) {
        foodData.addExhaustion(EfficiencyScaling.scaleHunger(amount, player));
    }
}
