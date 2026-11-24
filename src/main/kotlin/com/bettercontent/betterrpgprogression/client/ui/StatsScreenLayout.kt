package com.bettercontent.betterrpgprogression.client.ui

import kotlin.math.min

/** A readable canvas independent of the global inventory GUI scale. No global option is changed. */
internal data class StatsScreenLayout(
    val scale: Float,
    val width: Int,
    val height: Int,
    val columns: Int,
    val cardWidth: Int,
    private val rowHeights: List<Int>,
    private val rowTops: List<Int>,
    private val spanningCard: Int?,
    val footerTop: Int
) {
    fun cardX(index: Int) = StatsScreenLayoutPolicy.MARGIN + (index % columns) * (cardWidth + StatsScreenLayoutPolicy.GAP)
    fun cardY(index: Int) = rowTops[index / columns]
    fun cardHeight(index: Int) = if (index == spanningCard) rowTops[1] - rowTops[0] + rowHeights[1]
        else rowHeights[index / columns]
    fun controlsY(index: Int) = cardY(index) + cardHeight(index) - StatsScreenLayoutPolicy.CONTROL_BOTTOM_OFFSET
    fun logical(coordinate: Double) = coordinate / scale
}

internal object StatsScreenLayoutPolicy {
    const val MARGIN = 16
    const val GAP = 10
    const val MIN_WIDTH = 1040
    const val MIN_HEIGHT = 500
    const val GRID_TOP = 76
    const val FOOTER_HEIGHT = 64
    const val MIN_CARD_HEIGHT = 166
    const val CONTROL_BOTTOM_OFFSET = 26
    const val CARD_PADDING = 10
    const val PURPOSE_TOP = 34
    const val LINE_HEIGHT = 11
    const val EFFECT_GAP = 5
    const val VALUE_WIDTH = 54

    fun columns(count: Int) = minOf(3, count.coerceAtLeast(1))
    fun cardWidth(width: Int, columns: Int) = (width - MARGIN * 2 - (columns - 1) * GAP) / columns
    fun effectLabelWidth(cardWidth: Int) = cardWidth - 2 * CARD_PADDING - 3 * VALUE_WIDTH - 6
    fun effectsTop(purposeLines: Int) = PURPOSE_TOP + purposeLines * LINE_HEIGHT + 7 + LINE_HEIGHT
    fun requiredCardHeight(purposeLines: Int, effectLineCounts: List<Int>) = maxOf(MIN_CARD_HEIGHT,
        effectsTop(purposeLines) + effectLineCounts.sumOf { it * LINE_HEIGHT + EFFECT_GAP } + CONTROL_BOTTOM_OFFSET + 8)

    fun calculate(screenWidth: Int, screenHeight: Int, requiredCardHeights: List<Int>): StatsScreenLayout {
        val columns = columns(requiredCardHeights.size)
        // Five cards leave the bottom-right slot open. Let the third card (Endurance in
        // the authored roster) occupy that column, instead of making the top row as tall as it is.
        val spanningCard = if (requiredCardHeights.size == 5) 2 else null
        val rowRequirements = requiredCardHeights.mapIndexed { index, height -> if (index == spanningCard) 0 else height }
        val minimumRows = rowRequirements.chunked(columns).map { it.max() }
        val gaps = (minimumRows.size - 1).coerceAtLeast(0) * GAP
        val rowContentHeight = minimumRows.sum() + gaps
        val contentHeight = maxOf(rowContentHeight, spanningCard?.let { requiredCardHeights[it] } ?: 0)
        val minimumHeight = maxOf(MIN_HEIGHT, GRID_TOP + FOOTER_HEIGHT + MARGIN + contentHeight)
        val scale = min(1f, min(screenWidth.coerceAtLeast(1).toFloat() / MIN_WIDTH,
            screenHeight.coerceAtLeast(1).toFloat() / minimumHeight))
        val width = (screenWidth / scale).toInt().coerceAtLeast(MIN_WIDTH)
        val height = (screenHeight / scale).toInt().coerceAtLeast(minimumHeight)
        val footerTop = height - FOOTER_HEIGHT
        val spareHeight = footerTop - MARGIN - GRID_TOP - rowContentHeight
        val rowHeights = minimumRows.mapIndexed { index, rowHeight ->
            rowHeight + spareHeight / minimumRows.size + if (index < spareHeight % minimumRows.size) 1 else 0
        }
        val rowTops = rowHeights.runningFold(GRID_TOP) { top, rowHeight -> top + rowHeight + GAP }
        return StatsScreenLayout(scale, width, height, columns, cardWidth(width, columns), rowHeights, rowTops,
            spanningCard, footerTop)
    }
}

/** Draft points never refund committed allocations and never mint additional points. */
internal class StatsAllocationDraft(
    snapshot: com.bettercontent.betterrpgprogression.client.cache.ClientStatsSnapshot,
    activeIds: Set<String> = snapshot.allocations.keys
) {
    // Inert pre-consolidation investments remain committed on the server and consume no
    // new budget; never resend their stale IDs as part of a fresh five-choice request.
    private val original = snapshot.allocations.filterKeys { it in activeIds }
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
