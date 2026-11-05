package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.client.cache.ClientCurveDef
import com.bettercontent.betterrpgprogression.client.cache.ClientEffectDef
import com.bettercontent.betterrpgprogression.common.config.json.CurveDef
import com.bettercontent.betterrpgprogression.common.curve.Curves
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

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

    /** Goety converts its range attribute to an integer before using it. The raw curve is
     * shown separately so sub-block progress is visible without promising extra cast range. */
    fun effectiveSpellRange(effect: ClientEffectDef, points: Int, committed: Int, liveAttribute: Double): StatsEffectReading {
        val raw = read(effect, points)
        val committedBonus = read(effect, committed).bonus
        val baseline = liveAttribute - committedBonus
        val baseRange = floor(baseline + 1e-9)
        val currentRange = floor(baseline + raw.bonus + 1e-9)
        val nextRange = floor(baseline + raw.bonus + raw.next + 1e-9)
        return StatsEffectReading(currentRange - baseRange, nextRange - currentRange, false)
    }

    /** The first future point at which Goety's integer range actually changes. */
    fun nextSpellRangePoint(effect: ClientEffectDef, points: Int, committed: Int,
                            liveAttribute: Double, cap: Int = 20): Int? {
        val current = effectiveSpellRange(effect, points, committed, liveAttribute).bonus
        return ((points + 1)..cap).firstOrNull {
            effectiveSpellRange(effect, it, committed, liveAttribute).bonus > current
        }
    }

    /** Show actual equipment-adjusted attribute values without suggesting they include draft points. */
    fun formatEquipped(value: Double?): String? {
        if (value == null || !value.isFinite()) return null
        return String.format(Locale.ROOT, "%.3f", value).trimEnd('0').trimEnd('.').let {
            if (it == "-0") "0" else it
        }
    }

    /** Related integrations appear together, including unavailable rows. */
    fun group(attributeId: String): String = when {
        attributeId in setOf("better_rpg_progression:air_attack_stamina_reduction",
            "better_rpg_progression:air_slash_damage_bonus") -> "epicfight"
        attributeId in setOf("better_rpg_progression:heat_drift_reduction",
            "better_rpg_progression:cold_drift_reduction",
            "better_rpg_progression:freezing_boundary_offset",
            "better_rpg_progression:burning_boundary_offset") -> "cold_sweat"
        attributeId.startsWith("epicfight:") -> "epicfight"
        attributeId.startsWith("tconstruct:") -> "tconstruct"
        attributeId.startsWith("ars_nouveau:") -> "ars_nouveau"
        attributeId.startsWith("goety:") -> "goety"
        attributeId.startsWith("parcool:") -> "parcool"
        attributeId.startsWith("cold_sweat:") -> "cold_sweat"
        else -> "life"
    }

    fun format(value: Double, percent: Boolean): String {
        if (abs(value) < 1e-9) return "—"
        val number = value * if (percent) 100.0 else 1.0
        // Small next-point increments must not round to zero while the curve is still increasing.
        val decimals = when {
            abs(number) >= 0.01 -> 2
            abs(number) >= 0.0001 -> 4
            else -> 8
        }
        val text = String.format(Locale.ROOT, "%.${decimals}f", number).trimEnd('0').trimEnd('.')
        return "${if (number > 0) "+" else ""}$text${if (percent) "%" else ""}"
    }

    private fun ClientCurveDef.common() = CurveDef(type, cap, k, perPoint, min, max)
}
