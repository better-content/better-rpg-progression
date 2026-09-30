package com.bettercontent.betterrpgprogression.common.network.packets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AllocationPolicyTest {
    private val uncapped = mapOf(
        "better_rpg_progression:impact" to -1,
        "better_rpg_progression:work" to -1
    )

    @Test
    fun `spends only newly committed points`() {
        val decision = AllocationPolicy.apply(
            current = mapOf("better_rpg_progression:impact" to 3),
            unspentPoints = 4,
            requested = mapOf("better_rpg_progression:impact" to 5, "better_rpg_progression:work" to 1),
            maxPointsById = uncapped
        )

        assertEquals(
            AllocationDecision(
                allocations = mapOf("better_rpg_progression:impact" to 5, "better_rpg_progression:work" to 1),
                unspentPoints = 1
            ),
            decision
        )
    }

    @Test
    fun `rejects reducing or omitting a committed allocation`() {
        val current = mapOf("better_rpg_progression:impact" to 3)

        assertNull(AllocationPolicy.apply(current, 4, mapOf("better_rpg_progression:impact" to 2), uncapped))
        assertNull(AllocationPolicy.apply(current, 4, emptyMap(), uncapped))
    }

    @Test
    fun `rejects overspending and overflow sized totals`() {
        assertNull(
            AllocationPolicy.apply(
                current = emptyMap(),
                unspentPoints = 2,
                requested = mapOf("better_rpg_progression:impact" to 3),
                maxPointsById = uncapped
            )
        )
        assertNull(
            AllocationPolicy.apply(
                current = emptyMap(),
                unspentPoints = Int.MAX_VALUE,
                requested = mapOf(
                    "better_rpg_progression:impact" to Int.MAX_VALUE,
                    "better_rpg_progression:work" to Int.MAX_VALUE
                ),
                maxPointsById = uncapped
            )
        )
    }

    @Test
    fun `preserves allocations whose definitions are missing`() {
        val decision = AllocationPolicy.apply(
            current = mapOf("removed_pack:old_stat" to 7),
            unspentPoints = 2,
            requested = mapOf("better_rpg_progression:work" to 2),
            maxPointsById = uncapped
        )

        assertEquals(
            mapOf("removed_pack:old_stat" to 7, "better_rpg_progression:work" to 2),
            decision?.allocations
        )
        assertEquals(0, decision?.unspentPoints)
    }

    @Test
    fun `optional datapack caps apply only to new investment`() {
        val cap = mapOf("better_rpg_progression:impact" to 5)

        assertNull(AllocationPolicy.apply(emptyMap(), 6, mapOf("better_rpg_progression:impact" to 6), cap))
        assertEquals(
            6,
            AllocationPolicy.apply(
                current = mapOf("better_rpg_progression:impact" to 6),
                unspentPoints = 1,
                requested = mapOf("better_rpg_progression:impact" to 6),
                maxPointsById = cap
            )?.allocations?.get("better_rpg_progression:impact")
        )
    }
}
