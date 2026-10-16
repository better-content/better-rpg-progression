package com.bettercontent.betterrpgprogression.client.ui

import kotlin.math.min

/** A readable canvas independent of the global inventory GUI scale. No global option is changed. */
internal data class StatsScreenLayout(
    val scale: Float,
    val width: Int,
    val height: Int,
    val columns: Int,
    val cardWidth: Int,
    val cardHeight: Int,
    val gridTop: Int,
    val footerTop: Int
) {
    fun cardX(index: Int) = StatsScreenLayoutPolicy.MARGIN + (index % columns) * (cardWidth + StatsScreenLayoutPolicy.GAP)
    fun cardY(index: Int) = gridTop + (index / columns) * (cardHeight + StatsScreenLayoutPolicy.GAP)
    fun logical(coordinate: Double) = coordinate / scale
}

internal object StatsScreenLayoutPolicy {
    const val MARGIN = 16
    const val GAP = 10
    const val MIN_WIDTH = 800
    const val MIN_HEIGHT = 500
    const val GRID_TOP = 76
    const val FOOTER_HEIGHT = 64
    const val MIN_CARD_HEIGHT = 166
    const val CONTROL_SIZE = 18

    fun calculate(screenWidth: Int, screenHeight: Int, attributeCount: Int): StatsScreenLayout {
        val columns = minOf(4, attributeCount.coerceAtLeast(1))
        val rows = (attributeCount.coerceAtLeast(1) + columns - 1) / columns
        val minimumHeight = maxOf(MIN_HEIGHT, GRID_TOP + FOOTER_HEIGHT + MARGIN +
            rows * MIN_CARD_HEIGHT + (rows - 1) * GAP)
        val scale = min(1f, min(screenWidth.coerceAtLeast(1).toFloat() / MIN_WIDTH,
            screenHeight.coerceAtLeast(1).toFloat() / minimumHeight))
        val width = (screenWidth / scale).toInt()
        val height = (screenHeight / scale).toInt()
        val footerTop = height - FOOTER_HEIGHT
        return StatsScreenLayout(scale, width, height, columns,
            (width - MARGIN * 2 - (columns - 1) * GAP) / columns,
            (footerTop - MARGIN - GRID_TOP - (rows - 1) * GAP) / rows,
            GRID_TOP, footerTop)
    }
}

/** Draft points never refund committed allocations and never mint additional points. */
internal class StatsAllocationDraft(snapshot: com.bettercontent.betterrpgprogression.client.cache.ClientStatsSnapshot) {
    private val original = snapshot.allocations.toMap()
    private val initialUnspent = snapshot.unspent
    val allocations = original.toMutableMap()
    var unspent = initialUnspent
        private set
    val spent get() = initialUnspent - unspent
    fun points(id: String) = allocations[id] ?: 0
    fun pending(id: String) = points(id) - (original[id] ?: 0)
    fun canAdd(id: String, cap: Int) = unspent > 0 && (cap < 0 || points(id) < cap)
    fun add(id: String, cap: Int): Boolean {
        if (!canAdd(id, cap)) return false
        allocations[id] = points(id) + 1
        unspent--
        return true
    }
    fun remove(id: String): Boolean {
        if (pending(id) <= 0) return false
        val next = points(id) - 1
        if (next == 0) allocations.remove(id) else allocations[id] = next
        unspent++
        return true
    }
    fun reset() {
        allocations.clear()
        allocations.putAll(original)
        unspent = initialUnspent
    }
}
