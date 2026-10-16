package com.bettercontent.betterrpgprogression.client.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsScreenLayoutTest {
    @Test
    fun `all eight complete cards remain visible across GUI scales and smaller windows`() {
        for ((width, height) in listOf(1920 to 1080, 960 to 540, 640 to 360, 480 to 270, 427 to 240, 320 to 180)) {
            val l = StatsScreenLayoutPolicy.calculate(width, height, 8)
            assertEquals(4, l.columns)
            assertEquals(2, (0 until 8).map(l::cardY).distinct().size)
            assertTrue(l.cardWidth >= 184)
            assertTrue(l.cardHeight >= StatsScreenLayoutPolicy.MIN_CARD_HEIGHT)
            for (i in 0 until 8) {
                assertTrue(l.cardX(i) >= 0 && l.cardY(i) >= 0)
                assertTrue((l.cardX(i) + l.cardWidth) * l.scale <= width)
                assertTrue(l.cardY(i) + l.cardHeight < l.footerTop)
                assertTrue((l.cardY(i) + l.cardHeight) * l.scale <= height)
                // Five always-visible effects end before the allocation controls.
                val spacing = minOf(18, (l.cardHeight - 110) / 5).coerceAtLeast(10)
                assertTrue(77 + spacing * 4 + 9 <= l.cardHeight - 32)
                val targetX = l.cardX(i) + l.cardWidth - 19.0
                val targetY = l.cardY(i) + l.cardHeight - 17.0
                assertEquals(targetX, l.logical(targetX * l.scale), 1e-5)
                assertEquals(targetY, l.logical(targetY * l.scale), 1e-5)
            }
        }
    }

    @Test
    fun `larger viewports use the whole screen instead of a fixed compact panel`() {
        val normal = StatsScreenLayoutPolicy.calculate(960, 540, 8)
        val large = StatsScreenLayoutPolicy.calculate(1920, 1080, 8)
        assertTrue(large.cardWidth > normal.cardWidth)
        assertTrue(large.cardHeight > normal.cardHeight)
        assertEquals(1f, large.scale)
        assertTrue(large.cardX(3) + large.cardWidth >= large.width - 19)
    }

    @Test
    fun `empty and extended definition lists have valid geometry`() {
        for (count in listOf(0, 1, 3, 9, 12)) {
            val l = StatsScreenLayoutPolicy.calculate(640, 360, count)
            assertTrue(l.cardWidth > 0 && l.cardHeight >= 166)
            if (count > 0) assertTrue(l.cardY(count - 1) + l.cardHeight < l.footerTop)
        }
    }
}
