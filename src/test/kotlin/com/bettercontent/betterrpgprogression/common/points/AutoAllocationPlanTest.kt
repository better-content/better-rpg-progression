package com.bettercontent.betterrpgprogression.common.points

import com.bettercontent.betterrpgprogression.common.data.PlayerStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoAllocationPlanTest {
    private val admitted = mapOf("better_rpg_progression:power" to -1, "better_rpg_progression:motion" to 2)

    @Test
    fun `plan spends only points assigned to the levels earned`() {
        val stats = PlayerStats().apply { unspentPoints = 5 }
        assertTrue(AutoAllocationPlan.replace(stats, true,
            listOf("better_rpg_progression:motion", "", "better_rpg_progression:power", "better_rpg_progression:motion", "better_rpg_progression:motion"), admitted))
        assertEquals(mapOf("better_rpg_progression:motion" to 2, "better_rpg_progression:power" to 1),
            AutoAllocationPlan.apply(stats, admitted, 1, 5))
        assertEquals(2, stats.unspentPoints)
        assertEquals(mapOf("better_rpg_progression:motion" to 2, "better_rpg_progression:power" to 1), stats.allocations)
    }

    @Test
    fun `invalid replacement cannot alter an existing plan`() {
        val stats = PlayerStats().apply { autoAllocationPlan += "better_rpg_progression:power"; autoAllocationEnabled = true }
        assertFalse(AutoAllocationPlan.replace(stats, true, listOf("better_rpg_progression:power", "missing:stat"), admitted))
        assertTrue(stats.autoAllocationEnabled)
        assertEquals(listOf("better_rpg_progression:power"), stats.autoAllocationPlan)
    }

    @Test
    fun `retired build and body part IDs are rejected instead of silently migrated`() {
        val stats = PlayerStats().apply { autoAllocationPlan += "better_rpg_progression:power" }
        assertFalse(AutoAllocationPlan.replace(stats, true, listOf("better_rpg_progression:vanguard"), admitted))
        assertFalse(AutoAllocationPlan.replace(stats, true, listOf("better_rpg_progression:arms"), admitted))
        assertEquals(listOf("better_rpg_progression:power"), stats.autoAllocationPlan)
    }

    @Test
    fun `death keeps the new character plan while wiping Life allocations`() {
        val stats = PlayerStats().apply {
            autoAllocationEnabled = true
            autoAllocationCursor = 1
            autoAllocationPlan += listOf("better_rpg_progression:power", "better_rpg_progression:motion")
            allocations["better_rpg_progression:power"] = 9
            unspentPoints = 2
        }
        stats.resetForDeath(0)
        assertTrue(stats.autoAllocationEnabled)
        assertEquals(1, stats.autoAllocationCursor)
        assertEquals(listOf("better_rpg_progression:power", "better_rpg_progression:motion"), stats.autoAllocationPlan)
        assertTrue(stats.allocations.isEmpty())
    }

    @Test
    fun `a new automatic award does not consume previously saved Life points`() {
        val stats = PlayerStats().apply { unspentPoints = 4 }
        assertTrue(AutoAllocationPlan.replace(stats, true, listOf("better_rpg_progression:power"), admitted))
        AutoAllocationPlan.apply(stats, admitted, 1, 1)
        assertEquals(3, stats.unspentPoints)
        assertEquals(1, stats.allocations["better_rpg_progression:power"])
    }

    @Test
    fun `old saved preference cannot spend or remap a point on new level`() {
        val stats = PlayerStats().apply {
            unspentPoints = 2; autoAllocationEnabled = true
            autoAllocationPlan += listOf("better_rpg_progression:vanguard", "better_rpg_progression:power")
        }
        assertEquals(mapOf("better_rpg_progression:power" to 1), AutoAllocationPlan.apply(stats, admitted, 1, 2))
        assertEquals(1, stats.unspentPoints)
        assertFalse(stats.allocations.containsKey("better_rpg_progression:vanguard"))
    }
}
