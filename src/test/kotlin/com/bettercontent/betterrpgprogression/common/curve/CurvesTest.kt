package com.bettercontent.betterrpgprogression.common.curve

import com.bettercontent.betterrpgprogression.common.config.json.CurveDef
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals

class CurvesTest {
    @Test
    fun `Life aspect curve reaches half its cap at twenty points`() {
        val curve = CurveDef(type = "hyperbola", cap = 8.0, k = 20.0, min = 0.0, max = 8.0)

        assertEquals(0.0, Curves.eval(0, curve))
        assertEquals(4.0, Curves.eval(20, curve))
        assertEquals(6.0, Curves.eval(60, curve))
    }

    @Test
    fun `hyperbola clamps negative points and approaches cap`() {
        val curve = CurveDef(type = "hyperbola", cap = 10.0, k = 5.0, min = 0.0, max = 10.0)

        assertEquals(0.0, Curves.eval(-3, curve))
        assertEquals(5.0, Curves.eval(5, curve))
        assertEquals(8.0, Curves.eval(20, curve))
    }

    @Test
    fun `exponential uses positive fallback k and bounds output`() {
        val curve = CurveDef(type = "exp", cap = 100.0, k = 0.0, min = 0.0, max = 100.0)
        val expectedAtOnePoint = 100.0 * (1.0 - exp(-1.0))
        val expectedAtTwoPoints = 100.0 * (1.0 - exp(-2.0))

        assertEquals(expectedAtOnePoint, Curves.eval(1, curve))
        assertEquals(expectedAtTwoPoints, Curves.eval(2, curve))
    }

    @Test
    fun `linear scales by points and clamps to min max`() {
        val curve = CurveDef(type = "linear", perPoint = 2.5, min = 0.0, max = 10.0)

        assertEquals(7.5, Curves.eval(3, curve))
        assertEquals(10.0, Curves.eval(12, curve))
    }
}
