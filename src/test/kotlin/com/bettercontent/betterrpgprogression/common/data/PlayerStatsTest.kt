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
    fun `death clone retains plan preferences but wipes Life state on the new instance`() {
        for (enabled in listOf(false, true)) {
            val original = PlayerStats().apply {
                autoAllocationEnabled = enabled
                autoAllocationCursor = 1
                autoAllocationPlan += listOf("better_rpg_progression:arms", "", "better_rpg_progression:hands")
                lifePeakLevel = 42
                unspentPoints = 3
                lifeAllocationEpisode = "old-life"
                allocations["better_rpg_progression:arms"] = 8
            }
            val replacement = PlayerStats()

            replacement.copyForRespawnFrom(original, wasDeath = true, currentLevel = 7)

            assertEquals(enabled, replacement.autoAllocationEnabled)
            assertEquals(1, replacement.autoAllocationCursor)
            assertEquals(original.autoAllocationPlan, replacement.autoAllocationPlan)
            assertEquals(7, replacement.lifePeakLevel)
            assertEquals(0, replacement.unspentPoints)
            assertEquals(null, replacement.lifeAllocationEpisode)
            assertTrue(replacement.allocations.isEmpty())
            replacement.autoAllocationPlan.clear()
            assertEquals(3, original.autoAllocationPlan.size)
            assertEquals(8, original.allocations["better_rpg_progression:arms"])
        }
    }

    @Test
    fun `non death clone preserves Life ledger and copies mutable collections`() {
        val original = PlayerStats().apply {
            autoAllocationEnabled = true
            autoAllocationCursor = 2
            autoAllocationPlan += "better_rpg_progression:arms"
            lifePeakLevel = 42
            unspentPoints = 3
            lifeAllocationEpisode = "same-life"
            allocations["better_rpg_progression:arms"] = 8
        }
        val replacement = PlayerStats()

        replacement.copyForRespawnFrom(original, wasDeath = false, currentLevel = 7)

        assertEquals(original.serializeNBT(), replacement.serializeNBT())
        replacement.allocations.clear()
        replacement.autoAllocationPlan.clear()
        assertEquals(8, original.allocations["better_rpg_progression:arms"])
        assertEquals(1, original.autoAllocationPlan.size)
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
