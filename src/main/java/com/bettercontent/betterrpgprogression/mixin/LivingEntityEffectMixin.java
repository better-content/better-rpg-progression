package com.bettercontent.betterrpgprogression.mixin;

import com.bettercontent.betterrpgprogression.common.attribute.VitalityScaling;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectMixin {
    @ModifyVariable(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private MobEffectInstance rpgStats$scaleEffectDuration(MobEffectInstance effect) {
        return VitalityScaling.scale((LivingEntity) (Object) this, effect);
    }
}
