package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.client.cache.ClientCurveDef
import com.bettercontent.betterrpgprogression.client.cache.ClientEffectDef
import com.bettercontent.betterrpgprogression.common.config.json.CurveDef
import com.bettercontent.betterrpgprogression.common.curve.Curves
import java.util.Locale
import kotlin.math.abs

internal data class StatsEffectReading(val bonus: Double, val next: Double, val percent: Boolean)

/** Show outcomes (less spread, shorter harmful effects), not a positive internal reduction stat. */
internal object StatsEffectPresentation {
    private val reductions = setOf(
        "better_rpg_progression:harmful_effect_duration_reduction",
        "better_rpg_progression:arrow_spread_reduction",
        "better_rpg_progression:recoil_reduction",
        "better_rpg_progression:dispersion_reduction"
    )

    fun read(effect: ClientEffectDef, points: Int, movementBase: Double = 0.1): StatsEffectReading {
        val current = Curves.eval(points, effect.curve.common())
        val next = Curves.eval(points + 1, effect.curve.common()) - current
        val movement = effect.attributeId == "minecraft:generic.movement_speed" &&
            effect.operation == 0 && movementBase > 0.0
        val factor = when {
            effect.attributeId in reductions -> -1.0
            movement -> 1.0 / movementBase
            else -> 1.0
        }
        val resistance = effect.attributeId == "minecraft:generic.knockback_resistance"
        return StatsEffectReading(current * factor, next * factor, movement || resistance || effect.displayAsPercent)
    }

    fun format(value: Double, percent: Boolean): String {
        if (abs(value) < 1e-9) return "—"
        val number = value * if (percent) 100.0 else 1.0
        val decimals = if (abs(number) < 0.01) 4 else 2
        val text = String.format(Locale.ROOT, "%.${decimals}f", number).trimEnd('0').trimEnd('.')
        return "${if (number > 0) "+" else ""}$text${if (percent) "%" else ""}"
    }

    private fun ClientCurveDef.common() = CurveDef(type, cap, k, perPoint, min, max)
}
