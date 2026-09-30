package com.bettercontent.betterrpgprogression.common.attribute

object MiningSpeedScaling {
    fun scale(speed: Float, multiplier: Double): Float {
        if (!speed.isFinite() || !multiplier.isFinite() || multiplier <= 0.0) return speed
        val scaled = speed.toDouble() * multiplier
        return scaled.coerceAtMost(Float.MAX_VALUE.toDouble()).toFloat()
    }
}
