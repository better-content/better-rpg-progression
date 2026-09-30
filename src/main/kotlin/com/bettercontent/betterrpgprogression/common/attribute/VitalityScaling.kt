package com.bettercontent.betterrpgprogression.common.attribute

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.nbt.CompoundTag

object VitalityScaling {
    @JvmStatic
    fun scale(entity: LivingEntity, original: MobEffectInstance): MobEffectInstance {
        val player = entity as? Player ?: return original
        val adjustment = if (original.effect.isBeneficial) {
            player.getAttributeValue(ModAttributes.BENEFICIAL_EFFECT_DURATION.get())
        } else {
            -player.getAttributeValue(ModAttributes.HARMFUL_EFFECT_DURATION_REDUCTION.get())
        }
        if (adjustment == 0.0 || original.duration <= 1) return original
        val duration = (original.duration * (1.0 + adjustment)).toInt().coerceAtLeast(1)
        return withDuration(original, duration)
    }

    /** Round-trip the whole effect record: nested effects, factor state, curatives and
     * independent particle/icon flags belong to the incoming instance. */
    internal fun withDuration(original: MobEffectInstance, duration: Int): MobEffectInstance {
        val record = original.save(CompoundTag())
        record.putInt("Duration", duration)
        return requireNotNull(MobEffectInstance.load(record)) { "Registered effect failed to reload" }
    }
}
