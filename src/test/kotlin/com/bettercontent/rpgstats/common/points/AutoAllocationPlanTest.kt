package com.bettercontent.rpgstats.common.points

import com.bettercontent.rpgstats.common.data.PlayerStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoAllocationPlanTest {
    private val admitted = mapOf("rpg_stats:impact" to -1, "rpg_stats:tempo" to 2)

    @Test
    fun `plan spends only points assigned to the levels earned`() {
        val stats = PlayerStats().apply { unspentPoints = 5 }
        assertTrue(AutoAllocationPlan.replace(stats, true,
            listOf("rpg_stats:tempo", "", "rpg_stats:impact", "rpg_stats:tempo", "rpg_stats:tempo"), admitted))

        assertEquals(mapOf("rpg_stats:tempo" to 2, "rpg_stats:impact" to 1), AutoAllocationPlan.apply(stats, admitted, 1, 5))
        assertEquals(2, stats.unspentPoints)
        assertEquals(mapOf("rpg_stats:tempo" to 2, "rpg_stats:impact" to 1), stats.allocations)
    }

    @Test
    fun `invalid replacement cannot alter an existing plan`() {
        val stats = PlayerStats().apply { autoAllocationPlan += "rpg_stats:impact"; autoAllocationEnabled = true }

        assertFalse(AutoAllocationPlan.replace(stats, true, listOf("rpg_stats:impact", "missing:stat"), admitted))
        assertTrue(stats.autoAllocationEnabled)
        assertEquals(listOf("rpg_stats:impact"), stats.autoAllocationPlan)
    }

    @Test
    fun `death keeps the character plan while wiping Life allocations`() {
        val stats = PlayerStats().apply {
            autoAllocationEnabled = true
            autoAllocationCursor = 1
            autoAllocationPlan += listOf("rpg_stats:impact", "rpg_stats:tempo")
            allocations["rpg_stats:impact"] = 9
            unspentPoints = 2
        }

        stats.resetForDeath(0)

        assertTrue(stats.autoAllocationEnabled)
        assertEquals(1, stats.autoAllocationCursor)
        assertEquals(listOf("rpg_stats:impact", "rpg_stats:tempo"), stats.autoAllocationPlan)
        assertTrue(stats.allocations.isEmpty())
    }

    @Test
    fun `a new automatic award does not consume previously saved Life points`() {
        val stats = PlayerStats().apply { unspentPoints = 4 }
        assertTrue(AutoAllocationPlan.replace(stats, true, listOf("rpg_stats:impact"), admitted))

        AutoAllocationPlan.apply(stats, admitted, 1, 1)

        assertEquals(3, stats.unspentPoints)
        assertEquals(1, stats.allocations["rpg_stats:impact"])
    }
}
