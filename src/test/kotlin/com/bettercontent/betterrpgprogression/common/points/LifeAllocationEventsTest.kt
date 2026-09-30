package com.bettercontent.betterrpgprogression.common.points

import com.bettercontent.betterrpgprogression.common.data.PlayerStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LifeAllocationEventsTest {
    @Test
    fun `availability begins exactly one episode until it is spent`() {
        val stats = PlayerStats()

        assertEquals("player:life:40", LifeAllocationEvents.beginEpisode(stats, "player:life:40"))
        assertNull(LifeAllocationEvents.beginEpisode(stats, "player:life:41"))
        assertEquals("player:life:40", stats.lifeAllocationEpisode)
    }
}
