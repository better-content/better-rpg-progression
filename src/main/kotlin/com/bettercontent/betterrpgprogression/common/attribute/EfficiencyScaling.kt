package com.bettercontent.betterrpgprogression.common.attribute

import net.minecraft.world.entity.player.Player
import kotlin.math.max

object EfficiencyScaling {
    fun scale(amount: Float, efficiency: Double): Float =
        (amount / max(1.0, efficiency)).toFloat()

    @JvmStatic
    fun scaleHunger(amount: Float, player: Player): Float =
        scale(amount, player.getAttributeValue(ModAttributes.HUNGER_EFFICIENCY.get()))

    @JvmStatic
    fun scaleThirst(amount: Float, player: Player): Float =
        scale(amount, player.getAttributeValue(ModAttributes.THIRST_EFFICIENCY.get()))

    @JvmStatic
    fun restoreMirroredHungerScale(amount: Float, player: Player): Float =
        (amount * max(1.0, player.getAttributeValue(ModAttributes.HUNGER_EFFICIENCY.get()))).toFloat()
}
