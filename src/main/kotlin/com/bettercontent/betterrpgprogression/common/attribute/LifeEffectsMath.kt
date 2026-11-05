package com.bettercontent.betterrpgprogression.common.attribute

/** Conditional Life effects share server and UI semantics; never manufacture free actions. */
object LifeEffectsMath {
    const val AIR_SLASH_BASE_MULTIPLIER = 1.5

    fun airAttackCost(cost: Float, reduction: Double): Float {
        if (!cost.isFinite() || cost <= 0f) return cost
        val fraction = reduction.takeIf { it.isFinite() }?.coerceIn(0.0, 0.95) ?: 0.0
        return (cost.toDouble() * (1.0 - fraction)).coerceAtLeast(0.001).toFloat()
    }

    /** The extra multiplier is relative to Epic Fight's existing 1.5x, not another 1.8x. */
    fun airSlashDamage(damage: Float, bonus: Double): Float {
        if (!damage.isFinite() || damage <= 0f) return damage
        val increment = bonus.takeIf { it.isFinite() }?.coerceIn(0.0, 0.30) ?: 0.0
        return (damage.toDouble() * (AIR_SLASH_BASE_MULTIPLIER + increment) / AIR_SLASH_BASE_MULTIPLIER)
            .coerceAtMost(Float.MAX_VALUE.toDouble()).toFloat()
    }

}
