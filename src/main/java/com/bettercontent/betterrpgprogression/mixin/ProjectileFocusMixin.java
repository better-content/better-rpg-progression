package com.bettercontent.betterrpgprogression.mixin;

import com.bettercontent.betterrpgprogression.common.attribute.ModAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Projectile.class)
abstract class ProjectileFocusMixin {
    @ModifyVariable(method = "shoot(DDDFF)V", at = @At("HEAD"), argsOnly = true, index = 7)
    private float better_rpg_progression$focusArrowSpeed(float speed) {
        if ((Object) this instanceof AbstractArrow arrow && arrow.getOwner() instanceof Player player) {
            double bonus = player.getAttributeValue(ModAttributes.INSTANCE.getARROW_SPEED_BONUS().get());
            // A little more speed also extends the arrow's useful flight distance.
            return (float) (speed * (1.0 + Math.max(0.0, Math.min(0.08, bonus))));
        }
        return speed;
    }

    @ModifyVariable(method = "shoot(DDDFF)V", at = @At("HEAD"), argsOnly = true, index = 8)
    private float better_rpg_progression$focusArrowSpread(float spread) {
        if ((Object) this instanceof AbstractArrow arrow && arrow.getOwner() instanceof Player player) {
            double reduction = player.getAttributeValue(ModAttributes.INSTANCE.getARROW_SPREAD_REDUCTION().get());
            return (float) (spread * (1.0 - Math.max(0.0, Math.min(0.35, reduction))));
        }
        return spread;
    }
}
