package com.bettercontent.betterrpgprogression.common.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerStatsTest {
    @Test
    fun `death erases the entire current Life ledger`() {
        val stats = PlayerStats().apply {
            lifePeakLevel = 42
            unspentPoints = 3
            allocations["better_rpg_progression:arms"] = 8
            allocations["better_rpg_progression:eyes"] = 5
        }

        stats.resetForDeath(7)

        assertEquals(7, stats.lifePeakLevel)
        assertEquals(0, stats.unspentPoints)
        assertTrue(stats.allocations.isEmpty())
    }

    @Test
    fun `auto plan serializes separately from Life points`() {
        val stats = PlayerStats().apply {
            autoAllocationEnabled = true
            autoAllocationCursor = 1
            autoAllocationPlan += listOf("better_rpg_progression:arms", "better_rpg_progression:hands")
        }
        val restored = PlayerStats().apply { deserializeNBT(stats.serializeNBT()) }

        assertTrue(restored.autoAllocationEnabled)
        assertEquals(1, restored.autoAllocationCursor)
        assertEquals(listOf("better_rpg_progression:arms", "better_rpg_progression:hands"), restored.autoAllocationPlan)
        assertEquals(0, restored.unspentPoints)
    }
}
