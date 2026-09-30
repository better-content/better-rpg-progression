package com.bettercontent.betterrpgprogression.common.curve

import com.bettercontent.betterrpgprogression.common.config.json.CurveDef
import kotlin.math.exp
import kotlin.math.max

object Curves {
    fun eval(points: Int, def: CurveDef): Double {
        val p = max(0, points).toDouble()
        val raw = when (def.type.lowercase()) {
            "hyperbola" -> {
                val k = if (def.k <= 0.0) 1.0 else def.k
                if (p <= 0.0) 0.0 else def.cap * (p / (p + k))
            }
            "exp", "exponential" -> {
                val k = if (def.k <= 0.0) 1.0 else def.k
                def.cap * (1.0 - exp(-p / k))
            }
            "linear" -> def.perPoint * p
            else -> 0.0
        }
        return raw.coerceIn(def.min, def.max)
    }
}
