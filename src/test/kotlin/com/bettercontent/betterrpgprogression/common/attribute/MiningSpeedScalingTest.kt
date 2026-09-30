package com.bettercontent.betterrpgprogression.common.attribute

import kotlin.test.Test
import kotlin.test.assertEquals

class MiningSpeedScalingTest {
    @Test
    fun `multiplies the tool speed already calculated by forge`() {
        assertEquals(7.975f, MiningSpeedScaling.scale(5.5f, 1.45), 0.0001f)
    }

    @Test
    fun `base multiplier leaves speed unchanged`() {
        assertEquals(5.5f, MiningSpeedScaling.scale(5.5f, 1.0), 0.0001f)
    }

    @Test
    fun `invalid multiplier cannot corrupt break speed`() {
        assertEquals(5.5f, MiningSpeedScaling.scale(5.5f, Double.NaN), 0.0001f)
        assertEquals(5.5f, MiningSpeedScaling.scale(5.5f, 0.0), 0.0001f)
    }
}
