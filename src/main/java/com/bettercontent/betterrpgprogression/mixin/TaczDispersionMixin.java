package com.bettercontent.betterrpgprogression.mixin;

import com.bettercontent.betterrpgprogression.common.attribute.ControlScaling;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "com.tacz.guns.item.ModernKineticGunScriptAPI", remap = false)
abstract class TaczDispersionMixin {
    @Shadow
    private LivingEntity shooter;

    @ModifyArg(
        method = "doShoot",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;doBulletSpread(Lcom/tacz/guns/entity/shooter/ShooterDataHolder;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;IFFFF)V",
            remap = false
        ),
        index = 6,
        require = 0
    )
    private float better_rpg_progression$reduceDispersion(float inaccuracy) {
        return ControlScaling.scaleDispersion(inaccuracy, shooter);
    }
}
