package com.bettercontent.betterrpgprogression.common.attribute

import net.minecraft.world.entity.LivingEntity

object ControlScaling {
    private const val MAX_REDUCTION = 0.4

    fun factor(reduction: Double): Double = 1.0 - reduction.coerceIn(0.0, MAX_REDUCTION)

    @JvmStatic
    fun scaleRecoil(value: Float, entity: LivingEntity): Float =
        scale(value, entity.getAttributeValue(ModAttributes.RECOIL_REDUCTION.get()))

    @JvmStatic
    fun scaleDispersion(value: Float, entity: LivingEntity): Float =
        scale(value, entity.getAttributeValue(ModAttributes.DISPERSION_REDUCTION.get()))

    internal fun scale(value: Float, reduction: Double): Float {
        if (!value.isFinite()) return value
        return (value * factor(reduction)).toFloat()
    }
}
