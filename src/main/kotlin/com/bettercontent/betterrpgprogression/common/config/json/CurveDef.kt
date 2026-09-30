package com.bettercontent.betterrpgprogression.common.config.json

data class CurveDef(
    val type: String = "hyperbola",
    val cap: Double = 0.0,
    val k: Double = 1.0,
    val perPoint: Double = 0.0,
    val min: Double = Double.NEGATIVE_INFINITY,
    val max: Double = Double.POSITIVE_INFINITY
)
