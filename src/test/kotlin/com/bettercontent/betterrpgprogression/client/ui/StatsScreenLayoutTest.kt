package com.bettercontent.betterrpgprogression.client.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsScreenLayoutTest {
    // Includes grouped, wrapped effect names and equipped/extra notes on the densest cards.
    private val fullCards = listOf(
        listOf(3, 3, 2), listOf(3, 3, 2), listOf(5, 4, 4, 3, 5, 4), listOf(5, 5, 4, 4), listOf(3, 2, 2, 3)
    ).map { StatsScreenLayoutPolicy.requiredCardHeight(2, it) }

    @Test
    fun `all five complete cards remain visible across GUI scales and smaller windows`() {
        for ((width, height) in listOf(1920 to 1080, 960 to 540, 640 to 360, 480 to 270, 427 to 240, 320 to 180)) {
            val l = StatsScreenLayoutPolicy.calculate(width, height, fullCards)
            assertEquals(3, l.columns)
            assertEquals(2, fullCards.indices.map(l::cardY).distinct().size)
            assertTrue(l.cardWidth >= 329)
            for (i in fullCards.indices) {
                assertTrue(l.cardHeight(i) >= fullCards[i])
                assertTrue(l.cardX(i) >= 0 && l.cardY(i) >= 0)
                assertTrue((l.cardX(i) + l.cardWidth) * l.scale <= width)
                assertTrue(l.cardY(i) + l.cardHeight(i) < l.footerTop)
                assertTrue((l.cardY(i) + l.cardHeight(i)) * l.scale <= height)
                val targetX = l.cardX(i) + l.cardWidth - 19.0
                val targetY = l.controlsY(i) + 9.0
                assertEquals(targetX, l.logical(targetX * l.scale), 1e-5)
                assertEquals(targetY, l.logical(targetY * l.scale), 1e-5)
            }
            assertTrue(l.cardY(3) > l.cardY(0) + l.cardHeight(0))
            assertEquals(l.cardY(3) + l.cardHeight(3), l.cardY(2) + l.cardHeight(2))
        }
    }

    @Test
    fun `the tall card uses both rows without shrinking 720p text below one physical pixel per font pixel`() {
        val l = StatsScreenLayoutPolicy.calculate(640, 360, fullCards)
        assertTrue(l.scale * 2 >= 1f)
        assertTrue(l.cardHeight(2) > l.cardHeight(0))
        val tall = fullCards.toMutableList().apply { this[2] = 800 }
        val extended = StatsScreenLayoutPolicy.calculate(640, 360, tall)
        assertTrue(extended.cardHeight(2) >= 800)
        assertTrue(extended.cardY(2) + extended.cardHeight(2) < extended.footerTop)
    }

    @Test
    fun `full effect labels notes and gaps fit above the allocation controls`() {
        val policy = StatsScreenLayoutPolicy
        val lines = List(12) { if (it == 3) 6 else 4 }
        val l = policy.calculate(640, 360, listOf(policy.requiredCardHeight(3, lines)))
        var effectY = l.cardY(0) + policy.effectsTop(3)
        for (lineCount in lines) {
            effectY += lineCount * policy.LINE_HEIGHT + policy.EFFECT_GAP
            assertTrue(effectY < l.controlsY(0) - 6)
        }
    }

    @Test
    fun `larger viewports use the whole screen instead of a fixed compact panel`() {
        val normal = StatsScreenLayoutPolicy.calculate(960, 540, fullCards)
        val large = StatsScreenLayoutPolicy.calculate(1920, 1080, fullCards)
        assertTrue(large.cardWidth > normal.cardWidth)
        assertTrue(large.cardHeight(0) > normal.cardHeight(0))
        assertEquals(1f, large.scale)
        assertTrue(large.cardX(2) + large.cardWidth >= large.width - 19)
    }

    @Test
    fun `empty and extended definition lists have valid geometry`() {
        for (count in listOf(0, 1, 3, 8, 9, 12)) {
            val l = StatsScreenLayoutPolicy.calculate(640, 360, List(count) { StatsScreenLayoutPolicy.MIN_CARD_HEIGHT })
            assertTrue(l.cardWidth > 0)
            for (i in 0 until count) {
                assertTrue(l.cardHeight(i) >= StatsScreenLayoutPolicy.MIN_CARD_HEIGHT)
                assertTrue(l.cardY(i) + l.cardHeight(i) < l.footerTop)
            }
        }
    }
}
