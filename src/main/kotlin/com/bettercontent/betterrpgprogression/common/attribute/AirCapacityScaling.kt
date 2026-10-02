package com.bettercontent.betterrpgprogression.common.attribute

import net.minecraft.world.entity.player.Player
import kotlin.math.roundToInt

object AirCapacityScaling {
    fun maxAir(base: Int, multiplier: Double): Int =
        (base * multiplier.coerceIn(1.0, 2.0)).roundToInt()

    @JvmStatic
    fun maxAir(base: Int, player: Player): Int =
        if (player.attributes == null) base
        else player.getAttribute(ModAttributes.AIR_CAPACITY.get())?.let { maxAir(base, it.value) } ?: base
}
