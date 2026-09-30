package com.bettercontent.betterrpgprogression.common.block.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class HeartBlockEmissionTest {
    @Test
    fun `each installed fragment requests one LP per tick`() {
        var requested = 0
        assertEquals(9, HeartBlockEmission.fill(9) { request -> requested += request; request })
        assertEquals(9, requested)
    }

    @Test
    fun `full altar stops without spending or duplicating investment`() {
        var calls = 0
        assertEquals(0, HeartBlockEmission.fill(12) { calls += 1; 0 })
        assertEquals(1, calls)
    }

    @Test
    fun `int-shaped altar API does not silently truncate large installed count`() {
        val requests = mutableListOf<Int>()
        val total = HeartBlockEmission.fill(Int.MAX_VALUE.toLong() + 3) { request ->
            requests += request
            request
        }

        assertEquals(listOf(Int.MAX_VALUE, 3), requests)
        assertEquals(Int.MAX_VALUE.toLong() + 3, total)
    }
}
