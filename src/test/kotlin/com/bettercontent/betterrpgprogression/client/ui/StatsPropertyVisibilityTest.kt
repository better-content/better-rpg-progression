package com.bettercontent.betterrpgprogression.client.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsPropertyVisibilityTest {
    @Test
    fun `completely unchanged zero properties are hidden`() {
        assertFalse(StatsPropertyVisibility.isVisible(StatsPropertyTotals(0.0, 0.0)))
        assertFalse(StatsPropertyVisibility.isVisible(StatsPropertyTotals(5e-10, -5e-10)))
    }

    @Test
    fun `committed properties and transitions involving zero remain visible`() {
        assertTrue(StatsPropertyVisibility.isVisible(StatsPropertyTotals(0.25, 0.25)))
        assertTrue(StatsPropertyVisibility.isVisible(StatsPropertyTotals(0.0, 0.25)))
        assertTrue(StatsPropertyVisibility.isVisible(StatsPropertyTotals(0.25, 0.0)))
    }

    @Test
    fun `pending comparisons use the same epsilon in both directions`() {
        assertFalse(StatsPropertyVisibility.hasPendingChange(StatsPropertyTotals(1.0, 1.0 + 5e-10)))
        assertEquals(0, StatsPropertyVisibility.direction(StatsPropertyTotals(1.0, 1.0 - 5e-10)))
        assertEquals(1, StatsPropertyVisibility.direction(StatsPropertyTotals(1.0, 1.0 + 2e-9)))
        assertEquals(-1, StatsPropertyVisibility.direction(StatsPropertyTotals(1.0, 1.0 - 2e-9)))
    }
}
