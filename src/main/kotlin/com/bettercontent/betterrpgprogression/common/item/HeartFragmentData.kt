package com.bettercontent.betterrpgprogression.common.item

import com.bettercontent.betterrpgprogression.common.config.HeartFragmentConfig
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/** Death-reward math. Counts are never stored on ordinary fragment stacks. */
object HeartFragmentData {
    const val DEFAULT_ENTITLEMENT_SCALE: Int = HeartFragmentConfig.DEFAULT_ENTITLEMENT_SCALE
    const val LEVELS_PER_DOUBLING: Int = 4
    const val LP_PER_FRAGMENT_PER_TICK: Int = 1
    /** Keeps decimal arithmetic and playerdata bounded while covering any practical XP level. */
    const val MAX_EXACT_CARRIER_LEVEL: Int = 4096

    fun fragmentsForLevel(level: Int): Long {
        val count = fragmentsForLevelBig(level)
        require(count <= BigInteger.valueOf(Long.MAX_VALUE)) { "Heart fragment entitlement exceeds legacy long carrier at level $level" }
        return count.toLong()
    }

    internal fun fragmentsForLevel(level: Int, scale: Int): Long {
        val count = fragmentsForLevelBig(level, scale)
        require(count <= BigInteger.valueOf(Long.MAX_VALUE)) { "Heart fragment entitlement exceeds legacy long carrier at level $level" }
        return count.toLong()
    }

    fun fragmentsForLevelBig(level: Int): BigInteger = fragmentsForLevelBig(level, HeartFragmentConfig.entitlementScale())

    internal fun fragmentsForLevelBig(level: Int, scale: Int): BigInteger {
        require(scale > 0) { "Heart fragment entitlement scale must be positive" }
        if (level <= 0) return BigInteger.ZERO
        require(level <= MAX_EXACT_CARRIER_LEVEL) { "Heart fragment entitlement level $level exceeds supported exact carrier range" }

        val exponent = level / LEVELS_PER_DOUBLING
        val remainder = level % LEVELS_PER_DOUBLING
        val precision = maxOf(80, (exponent * 0.302).toInt() + 80)
        val context = MathContext(precision, RoundingMode.HALF_EVEN)
        val two = BigDecimal.valueOf(2)
        val whole = BigDecimal(BigInteger.TWO.pow(exponent))
        val rootTwo = two.sqrt(context)
        val fractional = when (remainder) {
            0 -> BigDecimal.ONE
            1 -> rootTwo.sqrt(context)
            2 -> rootTwo
            else -> rootTwo.multiply(rootTwo.sqrt(context), context)
        }
        val value = whole.multiply(fractional, context)
            .subtract(BigDecimal.ONE, context)
            .multiply(BigDecimal.valueOf(scale.toLong()), context)
        return value.setScale(0, RoundingMode.CEILING).toBigIntegerExact().max(BigInteger.ONE)
    }

    fun lpPerTick(installedFragments: Long): Long {
        require(installedFragments >= 0) { "Installed heart fragments cannot be negative" }
        return Math.multiplyExact(installedFragments, LP_PER_FRAGMENT_PER_TICK.toLong())
    }
}
