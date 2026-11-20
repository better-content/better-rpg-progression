package com.bettercontent.betterrpgprogression.common.attribute

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ModifierRetentionTest {
    private data class Entry(val owner: String, val identity: Int)

    @Test fun repeatedGatherKeepsOneOwnedModifierWithoutMovingNativeEntries() {
        val nativeRate = Entry("cold_sweat:inventory_items", 0)
        val nativeOther = Entry("another_mod:rate", 1)
        val original = Entry("better_rpg_progression:survivalist_rate", 2)
        val duplicates = mutableListOf(nativeRate, original, nativeOther,
            Entry(original.owner, 3), Entry(original.owner, 4))
        val owned: (Entry) -> Boolean = { it.owner == original.owner }
        retainSingleOwnedModifier(duplicates, owned) { error("duplicate gather must not construct a new modifier") }
        assertEquals(listOf(nativeRate, nativeOther, original), duplicates)
        assertSame(original, duplicates.last())
        retainSingleOwnedModifier(duplicates, owned) { error("idempotent gather must not construct a new modifier") }
        assertEquals(listOf(nativeRate, nativeOther, original), duplicates)
    }

    @Test fun missingModifierIsRegisteredExactlyOnceForEachRateAndBoundaryTrait() {
        for (trait in listOf("survivalist_rate", "survivalist_freezing", "survivalist_burning")) {
            val native = Entry("cold_sweat:inventory_items", 0)
            val list = mutableListOf(native)
            val owner = "better_rpg_progression:$trait"
            var created = 0
            repeat(44) {
                retainSingleOwnedModifier(list, { it.owner == owner }) { Entry(owner, ++created) }
            }
            assertEquals(1, created, trait)
            assertEquals(listOf(native, Entry(owner, 1)), list, trait)
        }
    }

    @Test fun foreignModifiersAndAlreadySingleOwnedModifierRetainIdentityAndOrder() {
        val native = Entry("cold_sweat:inventory_items", 0)
        val existing = Entry("better_rpg_progression:survivalist_rate", 1)
        val tail = Entry("other_mod:rate", 2)
        val list = mutableListOf(native, existing, tail)
        retainSingleOwnedModifier(list, { it.owner == existing.owner }) { error("already present") }
        assertEquals(listOf(native, existing, tail), list)
        assertSame(existing, list[1])
    }
}
