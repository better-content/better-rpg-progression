package com.bettercontent.betterrpgprogression.common.network.packets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AllocationPolicyTest {
    private val uncapped = mapOf("better_rpg_progression:power" to -1, "better_rpg_progression:motion" to -1)

    @Test
    fun `spends only newly committed points`() {
        val decision = AllocationPolicy.apply(
            current = mapOf("better_rpg_progression:power" to 3), unspentPoints = 4,
            requested = mapOf("better_rpg_progression:power" to 5, "better_rpg_progression:motion" to 1),
            maxPointsById = uncapped
        )
        assertEquals(AllocationDecision(
            allocations = mapOf("better_rpg_progression:power" to 5, "better_rpg_progression:motion" to 1),
            unspentPoints = 1
        ), decision)
    }

    @Test
    fun `rejects reducing or omitting a committed allocation`() {
        val current = mapOf("better_rpg_progression:power" to 3)
        assertNull(AllocationPolicy.apply(current, 4, mapOf("better_rpg_progression:power" to 2), uncapped))
        assertNull(AllocationPolicy.apply(current, 4, emptyMap(), uncapped))
    }

    @Test
    fun `rejects overspending and overflow sized totals`() {
        assertNull(AllocationPolicy.apply(emptyMap(), 2, mapOf("better_rpg_progression:power" to 3), uncapped))
        assertNull(AllocationPolicy.apply(
            current = emptyMap(), unspentPoints = Int.MAX_VALUE,
            requested = mapOf("better_rpg_progression:power" to Int.MAX_VALUE,
                "better_rpg_progression:motion" to Int.MAX_VALUE), maxPointsById = uncapped
        ))
    }

    @Test
    fun `preserves saved inactive allocations but rejects their use as requested IDs`() {
        val decision = AllocationPolicy.apply(
            current = mapOf("better_rpg_progression:arms" to 7), unspentPoints = 2,
            requested = mapOf("better_rpg_progression:motion" to 2), maxPointsById = uncapped
        )
        assertEquals(mapOf("better_rpg_progression:arms" to 7, "better_rpg_progression:motion" to 2), decision?.allocations)
        assertEquals(0, decision?.unspentPoints)
        assertNull(AllocationPolicy.apply(emptyMap(), 2, mapOf("better_rpg_progression:arms" to 1), uncapped))
        assertNull(AllocationPolicy.apply(
            current = mapOf("better_rpg_progression:arms" to 7), unspentPoints = 2,
            requested = mapOf("better_rpg_progression:arms" to 7, "better_rpg_progression:motion" to 2),
            maxPointsById = uncapped))
    }

    @Test
    fun `mixed stale and valid request rejects atomically without partial grant`() {
        assertNull(AllocationPolicy.apply(
            current = mapOf("better_rpg_progression:power" to 1), unspentPoints = 5,
            requested = mapOf("better_rpg_progression:power" to 2,
                "better_rpg_progression:vanguard" to 2), maxPointsById = uncapped))
        assertNull(AllocationPolicy.apply(emptyMap(), 5,
            mapOf("better_rpg_progression:power" to 1, "" to -1), uncapped))
    }

    @Test
    fun `optional datapack caps apply only to new investment`() {
        val cap = mapOf("better_rpg_progression:power" to 5)
        assertNull(AllocationPolicy.apply(emptyMap(), 6, mapOf("better_rpg_progression:power" to 6), cap))
        assertEquals(6, AllocationPolicy.apply(
            current = mapOf("better_rpg_progression:power" to 6), unspentPoints = 1,
            requested = mapOf("better_rpg_progression:power" to 6), maxPointsById = cap
        )?.allocations?.get("better_rpg_progression:power"))
    }
}
