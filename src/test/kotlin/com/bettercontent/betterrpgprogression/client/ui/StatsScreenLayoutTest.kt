package com.bettercontent.betterrpgprogression.client.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsScreenLayoutTest {
    @Test
    fun `all eight attributes fit in a two by four grid at default scale three dimensions`() {
        val layout = StatsScreenLayoutPolicy.calculate(427, 240, 8)

        assertEquals(layout.attributeX(0), layout.attributeX(2))
        assertEquals(layout.attributeX(1), layout.attributeX(3))
        assertTrue(layout.attributeX(0) < layout.attributeX(1))
        assertEquals(4, (0 until 8).map(layout::attributeY).distinct().size)
        assertTrue(layout.attributeY(7) + layout.attributeTileHeight <= layout.propertyHeaderTop)
        assertTrue(layout.propertyViewportHeight >= 22, "at least one complete property row remains visible")
        assertTrue(
            StatsScreenLayoutPolicy.CONTROL_TOP + StatsScreenLayoutPolicy.CONTROL_SIZE <= StatsScreenLayoutPolicy.DETAIL_TOP,
            "top-line controls must end before next-point detail text begins"
        )
        assertTrue(
            StatsScreenLayoutPolicy.BADGE_Y + StatsScreenLayoutPolicy.BADGE_SIZE <= layout.attributeTileHeight,
            "the full aspect badge must fit inside its row"
        )
        assertTrue(
            StatsScreenLayoutPolicy.BADGE_X + StatsScreenLayoutPolicy.BADGE_SIZE +
                StatsScreenLayoutPolicy.BADGE_TEXT_GAP < layout.attributeTileWidth - 62,
            "attribute text must begin after the badge and before its controls"
        )
        assertTrue(layout.footerTop < layout.panelY + layout.panelHeight)
    }

    @Test
    fun `larger screens expand properties without spreading the attribute grid`() {
        val compact = StatsScreenLayoutPolicy.calculate(427, 240, 8)
        val large = StatsScreenLayoutPolicy.calculate(640, 360, 8)

        assertEquals(compact.attributeTileHeight, large.attributeTileHeight)
        assertTrue(large.attributeTileWidth > compact.attributeTileWidth)
        assertTrue(large.propertyViewportHeight > compact.propertyViewportHeight)
    }
}
