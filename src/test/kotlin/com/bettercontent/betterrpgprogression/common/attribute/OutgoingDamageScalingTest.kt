package com.bettercontent.betterrpgprogression.common.attribute

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutgoingDamageScalingTest {
    @Test
    fun `percentage multiplier preserves a base damage amount without flat injection`() {
        assertEquals(1.0f, OutgoingDamageScaling.multiplier(0.0))
        assertEquals(1.2307693f, OutgoingDamageScaling.multiplier(0.3), 0.00001f)
        assertEquals(12.307693f, OutgoingDamageScaling.scaleAmount(10.0f, OutgoingDamageScaling.multiplier(0.3)), 0.00001f)
    }

    @Test
    fun `negative values cannot make outgoing damage negative`() {
        assertEquals(0.0f, OutgoingDamageScaling.multiplier(-2.0))
    }

    @Test
    fun `progression is monotonic with diminishing marginal gain and bounded`() {
        val first = OutgoingDamageScaling.multiplier(0.1)
        val second = OutgoingDamageScaling.multiplier(0.2)
        val third = OutgoingDamageScaling.multiplier(0.3)
        assertTrue(first < second && second < third)
        assertTrue(second - first > third - second)
        assertTrue(OutgoingDamageScaling.multiplier(2.0) < 2.0f)
    }
}
