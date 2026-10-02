package com.bettercontent.betterrpgprogression.common.attribute

import com.bettercontent.betterrpgprogression.common.curve.Curves
import com.bettercontent.betterrpgprogression.common.config.json.CurveDef
import kotlin.test.Test
import kotlin.test.assertEquals

class AirCapacityScalingTest {
    @Test
    fun `lungs raise underwater air capacity with diminishing returns`() {
        val curve = CurveDef(cap = 1.0, k = 20.0, min = 0.0, max = 1.0)
        assertEquals(300, AirCapacityScaling.maxAir(300, 1.0 + Curves.eval(0, curve)))
        assertEquals(450, AirCapacityScaling.maxAir(300, 1.0 + Curves.eval(20, curve)))
        assertEquals(600, AirCapacityScaling.maxAir(300, 2.0))
    }
}
