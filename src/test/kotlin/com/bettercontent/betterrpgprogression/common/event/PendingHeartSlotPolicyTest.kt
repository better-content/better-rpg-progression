package com.bettercontent.betterrpgprogression.common.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingHeartSlotPolicyTest {
    @Test
    fun `heart uses highest numbered empty slot without moving occupied slots`() {
        val empty = MutableList(36) { false }
        empty[0] = true
        empty[17] = true
        empty[35] = true

        assertEquals(35, PendingHeartSlotPolicy.lastEmptySlot(empty))
    }

    @Test
    fun `full inventory has no insertion slot`() {
        assertNull(PendingHeartSlotPolicy.lastEmptySlot(List(36) { false }))
    }
}
