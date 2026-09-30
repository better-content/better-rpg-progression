package com.bettercontent.betterrpgprogression.common.attribute

import kotlin.test.Test
import kotlin.test.assertEquals

class EfficiencyScalingTest {
    @Test
    fun `efficiency divides exhaustion without ever amplifying it`() {
        assertEquals(4.0f, EfficiencyScaling.scale(4.0f, 0.0), 0.0001f)
        assertEquals(4.0f, EfficiencyScaling.scale(4.0f, 1.0), 0.0001f)
        assertEquals(2.0f, EfficiencyScaling.scale(4.0f, 2.0), 0.0001f)
        assertEquals(4.0f / 3.0f, EfficiencyScaling.scale(4.0f, 3.0), 0.0001f)
    }

    @Test
    fun `half cap Endurance yields one hundred fifty percent efficiency`() {
        val bonusAtTwentyPoints = 20.0 / (20.0 + 20.0)
        val efficiency = 1.0 + bonusAtTwentyPoints
        assertEquals(1.5, efficiency, 0.0001)
        assertEquals(2.0f, EfficiencyScaling.scale(3.0f, efficiency), 0.0001f)
    }
}
