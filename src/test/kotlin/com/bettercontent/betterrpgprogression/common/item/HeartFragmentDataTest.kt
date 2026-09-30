package com.bettercontent.betterrpgprogression.common.item

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HeartFragmentDataTest {
    @Test fun `final death curve is zero at zero and strictly increases for reachable positive levels`() {
        assertEquals(0, HeartFragmentData.fragmentsForLevel(0))
        val values = (1..30).map(HeartFragmentData::fragmentsForLevel)
        values.zipWithNext().forEach { (lower, higher) -> assertTrue(higher > lower) }
        assertEquals(listOf(1L, 2L, 3L, 4L, 6L), values.take(5))
        assertEquals(124, HeartFragmentData.fragmentsForLevel(20))
        assertEquals(BigInteger("147573952589676412924"), HeartFragmentData.fragmentsForLevelBig(260))
        assertEquals(62, HeartFragmentData.fragmentsForLevel(20, 2))
        assertEquals(721, HeartFragmentData.fragmentsForLevel(30))
    }

    @Test fun `overflow is explicit instead of a silent fragment clamp`() {
        assertFailsWith<IllegalArgumentException> { HeartFragmentData.fragmentsForLevelBig(Int.MAX_VALUE) }
    }

    @Test fun `installed fragment output is linear`() {
        assertEquals(0, HeartFragmentData.lpPerTick(0))
        assertEquals(32, HeartFragmentData.lpPerTick(32))
    }

    @Test fun `developed deaths outperform equal XP shallow deaths including earlier LP`() {
        // Accounting model from blood-economics.md: earn XP evenly over one hour, with
        // shallow deaths evenly spaced throughout it; compare LP available through the
        // end of the following hour. This captures the extra production time from earlier
        // shallow payouts. Each reward remains installed for the rest of that two-hour window.
        val ticksPerHour = 20L * 60 * 60

        fun lpInWindow(levels: List<Int>): Long {
            val shallowDeathInterval = ticksPerHour / levels.size
            return levels.mapIndexed { index, level ->
                val fragments = HeartFragmentData.fragmentsForLevel(level)
                val ticksAvailable = ticksPerHour + (ticksPerHour - (index + 1) * shallowDeathInterval)
                HeartFragmentData.lpPerTick(fragments) * ticksAvailable
            }.sum()
        }

        // Vanilla cumulative XP: L20 = 550 = 10 * L5. L30 = 1395;
        // 25 * L5 spends 1375 XP, leaving 20 XP unused.
        val developedL20 = lpInWindow(listOf(20))
        val repeatedL5ForL20Xp = lpInWindow(List(10) { 5 })
        assertEquals(8_928_000L, developedL20)
        assertEquals(6_264_000L, repeatedL5ForL20Xp)
        assertTrue(developedL20 > repeatedL5ForL20Xp)

        val developedL30 = lpInWindow(listOf(30))
        val repeatedL5ForL30Xp = lpInWindow(List(25) { 5 })
        assertEquals(51_912_000L, developedL30)
        assertEquals(15_984_000L, repeatedL5ForL30Xp)
        assertTrue(developedL30 > repeatedL5ForL30Xp)
    }
}
