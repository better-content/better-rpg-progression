package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.client.cache.ClientStatsSnapshot
import kotlin.test.*

class StatsAllocationDraftTest {
    @Test
    fun `draft spends only available points and never refunds committed points`() {
        val source = ClientStatsSnapshot(unspent = 2, allocations = mapOf("arms" to 4))
        val draft = StatsAllocationDraft(source)
        assertFalse(draft.remove("arms"))
        assertTrue(draft.add("arms", -1))
        assertTrue(draft.add("eyes", -1))
        assertFalse(draft.add("eyes", -1))
        assertEquals(0, draft.unspent)
        assertEquals(2, draft.spent)
        assertEquals(1, draft.pending("arms"))
        assertTrue(draft.remove("arms"))
        assertFalse(draft.remove("arms"))
        assertEquals(1, draft.unspent)
        assertEquals(4, source.allocations["arms"])
        draft.reset()
        assertEquals(source.allocations, draft.allocations)
        assertEquals(2, draft.unspent)
        assertEquals(0, draft.spent)
    }

    @Test
    fun `capped and exhausted attributes cannot be incremented`() {
        val draft = StatsAllocationDraft(ClientStatsSnapshot(unspent = 1, allocations = mapOf("arms" to 4)))
        assertFalse(draft.add("arms", 4))
        assertTrue(draft.add("arms", 5))
        assertFalse(draft.add("arms", 5))
        assertFalse(draft.add("eyes", -1))
        assertTrue(draft.remove("arms"))
        assertTrue(draft.add("eyes", -1))
    }
}
