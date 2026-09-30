package com.bettercontent.betterrpgprogression.common.attribute

import kotlin.test.Test
import kotlin.test.assertEquals

class ControlScalingTest {
    @Test
    fun `Control reduction is bounded and preserves non finite sentinels`() {
        assertEquals(10.0f, ControlScaling.scale(10.0f, -1.0))
        assertEquals(8.0f, ControlScaling.scale(10.0f, 0.2))
        assertEquals(6.0f, ControlScaling.scale(10.0f, 0.4))
        assertEquals(6.0f, ControlScaling.scale(10.0f, 1.0))
        assertEquals(Float.POSITIVE_INFINITY, ControlScaling.scale(Float.POSITIVE_INFINITY, 0.4))
    }
}
