package com.bettercontent.betterrpgprogression.common.points

import com.bettercontent.betterrpgprogression.common.data.PlayerStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoAllocationPlanTest {
    private val admitted = mapOf("better_rpg_progression:arms" to -1, "better_rpg_progression:hands" to 2)

    @Test
    fun `plan spends only points assigned to the levels earned`() {
        val stats = PlayerStats().apply { unspentPoints = 5 }
        assertTrue(AutoAllocationPlan.replace(stats, true,
            listOf("better_rpg_progression:hands", "", "better_rpg_progression:arms", "better_rpg_progression:hands", "better_rpg_progression:hands"), admitted))

        assertEquals(mapOf("better_rpg_progression:hands" to 2, "better_rpg_progression:arms" to 1), AutoAllocationPlan.apply(stats, admitted, 1, 5))
        assertEquals(2, stats.unspentPoints)
        assertEquals(mapOf("better_rpg_progression:hands" to 2, "better_rpg_progression:arms" to 1), stats.allocations)
    }

    @Test
    fun `invalid replacement cannot alter an existing plan`() {
        val stats = PlayerStats().apply { autoAllocationPlan += "better_rpg_progression:arms"; autoAllocationEnabled = true }

        assertFalse(AutoAllocationPlan.replace(stats, true, listOf("better_rpg_progression:arms", "missing:stat"), admitted))
        assertTrue(stats.autoAllocationEnabled)
        assertEquals(listOf("better_rpg_progression:arms"), stats.autoAllocationPlan)
    }

    @Test
    fun `death keeps the character plan while wiping Life allocations`() {
        val stats = PlayerStats().apply {
            autoAllocationEnabled = true
            autoAllocationCursor = 1
            autoAllocationPlan += listOf("better_rpg_progression:arms", "better_rpg_progression:hands")
            allocations["better_rpg_progression:arms"] = 9
            unspentPoints = 2
        }

        stats.resetForDeath(0)

        assertTrue(stats.autoAllocationEnabled)
        assertEquals(1, stats.autoAllocationCursor)
        assertEquals(listOf("better_rpg_progression:arms", "better_rpg_progression:hands"), stats.autoAllocationPlan)
        assertTrue(stats.allocations.isEmpty())
    }

    @Test
    fun `a new automatic award does not consume previously saved Life points`() {
        val stats = PlayerStats().apply { unspentPoints = 4 }
        assertTrue(AutoAllocationPlan.replace(stats, true, listOf("better_rpg_progression:arms"), admitted))

        AutoAllocationPlan.apply(stats, admitted, 1, 1)

        assertEquals(3, stats.unspentPoints)
        assertEquals(1, stats.allocations["better_rpg_progression:arms"])
    }
}
