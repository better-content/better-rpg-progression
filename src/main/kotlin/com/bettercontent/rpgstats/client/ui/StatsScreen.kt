package com.bettercontent.rpgstats.client.ui

import com.bettercontent.rpgstats.client.cache.ClientCache
import com.bettercontent.rpgstats.client.cache.ClientCurveDef
import com.bettercontent.rpgstats.client.cache.ClientEffectDef
import com.bettercontent.rpgstats.client.cache.ClientStatDef
import com.bettercontent.rpgstats.common.config.json.CurveDef
import com.bettercontent.rpgstats.common.curve.Curves
import com.bettercontent.rpgstats.common.network.Network
import com.bettercontent.rpgstats.common.network.packets.C2SApplyStats
import com.bettercontent.rpgstats.common.salience.AspectIdentity
import com.bettercontent.rpgstats.common.sound.ModSounds
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import kotlin.math.abs

internal data class StatsScreenLayout(
    val panelX: Int,
    val panelY: Int,
    val panelWidth: Int,
    val panelHeight: Int,
    val contentX: Int,
    val contentWidth: Int,
    val summaryTop: Int,
    val attributeHeaderTop: Int,
    val attributeTop: Int,
    val attributeTileWidth: Int,
    val attributeTileHeight: Int,
    val propertyHeaderTop: Int,
    val propertyTop: Int,
    val footerTop: Int,
    val propertyViewportHeight: Int
) {
    fun attributeX(index: Int): Int = contentX + (index % 2) * (attributeTileWidth + StatsScreenLayoutPolicy.TILE_GAP)
    fun attributeY(index: Int): Int = attributeTop + (index / 2) * attributeTileHeight
}

internal object StatsScreenLayoutPolicy {
    const val OUTER_MARGIN = 8
    const val PANEL_MAX_WIDTH = 600
    const val PANEL_MAX_HEIGHT = 336
    const val PANEL_PADDING = 8
    const val TILE_GAP = 6
    const val SUMMARY_TOP = 17
    const val SUMMARY_HEIGHT = 16
    const val ATTRIBUTE_HEADER_TOP = 35
    const val HEADER_HEIGHT = 10
    const val ATTRIBUTE_TOP = 47
    const val ATTRIBUTE_TILE_HEIGHT = 28
    const val BADGE_SIZE = 18
    const val BADGE_X = 5
    const val BADGE_Y = 2
    const val BADGE_TEXT_GAP = 2
    const val CONTROL_TOP = 2
    const val CONTROL_SIZE = 14
    const val DETAIL_TOP = 16
    const val SECTION_GAP = 2
    const val FOOTER_HEIGHT = 25

    fun calculate(screenWidth: Int, screenHeight: Int, attributeCount: Int): StatsScreenLayout {
        val panelWidth = minOf(PANEL_MAX_WIDTH, screenWidth - OUTER_MARGIN * 2).coerceAtLeast(300)
        val panelHeight = minOf(PANEL_MAX_HEIGHT, screenHeight - OUTER_MARGIN * 2).coerceAtLeast(224)
        val panelX = (screenWidth - panelWidth) / 2
        val panelY = (screenHeight - panelHeight) / 2
        val contentX = panelX + PANEL_PADDING
        val contentWidth = panelWidth - PANEL_PADDING * 2
        val tileWidth = (contentWidth - TILE_GAP) / 2
        val attributeRows = (attributeCount + 1) / 2
        val propertyHeaderTop = panelY + ATTRIBUTE_TOP + attributeRows * ATTRIBUTE_TILE_HEIGHT + SECTION_GAP
        val propertyTop = propertyHeaderTop + HEADER_HEIGHT + SECTION_GAP
        val footerTop = panelY + panelHeight - FOOTER_HEIGHT
        return StatsScreenLayout(
            panelX, panelY, panelWidth, panelHeight, contentX, contentWidth,
            panelY + SUMMARY_TOP, panelY + ATTRIBUTE_HEADER_TOP, panelY + ATTRIBUTE_TOP,
            tileWidth, ATTRIBUTE_TILE_HEIGHT, propertyHeaderTop, propertyTop, footerTop,
            (footerTop - propertyTop - SECTION_GAP).coerceAtLeast(0)
        )
    }
}

internal data class StatsPropertyTotals(val committed: Double, val draft: Double)

internal object StatsPropertyVisibility {
    const val EPSILON = 1e-9

    fun isVisible(totals: StatsPropertyTotals): Boolean =
        abs(totals.committed) > EPSILON || abs(totals.draft) > EPSILON

    fun hasPendingChange(totals: StatsPropertyTotals): Boolean =
        abs(totals.draft - totals.committed) > EPSILON

    fun direction(totals: StatsPropertyTotals): Int = when {
        totals.draft - totals.committed > EPSILON -> 1
        totals.committed - totals.draft > EPSILON -> -1
        else -> 0
    }
}

class StatsScreen : Screen(Component.translatable("screen.rpg_stats.title")) {
    private companion object {
        const val PROPERTY_ROW_HEIGHT = 22
        const val CONTROL_WIDTH = 62
        val DEFAULT_FONT = ResourceLocation("minecraft", "default")

        const val PANEL_BORDER = 0xFFAA8E62.toInt()
        const val PANEL_BACKGROUND = 0xFFECDFBD.toInt()
        const val SUMMARY_BACKGROUND = 0xFFE4D5AF.toInt()
        const val HEADER_BACKGROUND = 0xFFDDCCA4.toInt()
        const val ROW_BACKGROUND = 0xFFF5EBD3.toInt()
        const val ROW_ALTERNATE = 0xFFEDE0C2.toInt()
        const val ROW_HOVER = 0xFFD9CEAE.toInt()
        const val RULE_COLOR = 0xFFBDA479.toInt()
        const val PRIMARY_TEXT = 0xFF30483E.toInt()
        const val SECONDARY_TEXT = 0xFF776C55.toInt()
        const val PENDING_UP = 0xFF386C47.toInt()
        const val PENDING_DOWN = 0xFF994A3B.toInt()
    }

    private data class Row(val def: ClientStatDef, var plus: Button? = null, var minus: Button? = null)

    private data class PropertyProvider(
        val attributeId: String,
        val operation: Int,
        val friendlyName: String,
        val sourceName: String,
        val color: Int,
        val perPoint: Double,
        val primary: Boolean,
        val displayAsPercent: Boolean
    )

    private data class PropertyRow(
        val friendlyName: String,
        val sourceName: String,
        val color: Int,
        val operation: Int,
        val displayAsPercent: Boolean,
        val originalTotal: Double,
        val workingTotal: Double
    )

    private var rows: List<Row> = emptyList()
    private var originalAlloc: Map<String, Int> = emptyMap()
    private var workingAlloc: MutableMap<String, Int> = mutableMapOf()
    private var workingUnspent = 0
    private var lifePeak = 0
    private var applyButton: Button? = null
    private var rightScrollOffset = 0.0
    private var rightContentHeight = 0
    private val previewPulses = mutableMapOf<String, Int>()

    override fun init() {
        super.init()
        val snapshot = ClientCache.stats
        originalAlloc = snapshot.allocations
        workingAlloc = snapshot.allocations.toMutableMap()
        workingUnspent = snapshot.unspent
        lifePeak = snapshot.lifePeak
        rows = ClientCache.defs.map(::Row)

        val layout = layout()
        rows.forEachIndexed { index, row ->
            val tileX = layout.attributeX(index)
            val buttonY = layout.attributeY(index) + StatsScreenLayoutPolicy.CONTROL_TOP
            row.minus = Button.builder(Component.literal("−")) { decrement(row) }
                .pos(tileX + layout.attributeTileWidth - CONTROL_WIDTH, buttonY)
                .size(StatsScreenLayoutPolicy.CONTROL_SIZE, StatsScreenLayoutPolicy.CONTROL_SIZE).build().also {
                    it.tooltip = Tooltip.create(Component.translatable("screen.rpg_stats.decrease", Component.translatable(row.def.nameKey)))
                    addRenderableWidget(it)
                }
            row.plus = Button.builder(Component.literal("+")) { increment(row) }
                .pos(tileX + layout.attributeTileWidth - StatsScreenLayoutPolicy.CONTROL_SIZE - 2, buttonY)
                .size(StatsScreenLayoutPolicy.CONTROL_SIZE, StatsScreenLayoutPolicy.CONTROL_SIZE).build().also {
                    it.tooltip = Tooltip.create(Component.translatable("screen.rpg_stats.increase", Component.translatable(row.def.nameKey)))
                    addRenderableWidget(it)
                }
        }

        val actionWidth = 80
        val actionY = layout.footerTop + 4
        addRenderableWidget(Button.builder(Component.translatable("screen.rpg_stats.auto_plan")) {
            minecraft?.setScreen(AutoAllocationScreen(this))
        }.pos(layout.contentX, actionY).size(92, 18).build())
        applyButton = Button.builder(Component.translatable("screen.rpg_stats.apply")) {
            Network.sendToServer(C2SApplyStats(workingAlloc.toMap()))
            onClose()
        }.pos(width / 2 - actionWidth / 2, actionY).size(actionWidth, 18).build().also(::addRenderableWidget)

        refreshButtons()
        syncRowButtons(layout)
    }

    private fun increment(row: Row) {
        val current = workingAlloc[row.def.id] ?: 0
        val cap = if (row.def.maxPoints >= 0) row.def.maxPoints else Int.MAX_VALUE
        if (workingUnspent > 0 && current < cap) {
            workingAlloc[row.def.id] = current + 1
            workingUnspent--
            preview(row.def)
            refreshButtons()
        }
    }

    override fun tick() {
        super.tick()
        previewPulses.replaceAll { _, ticks -> ticks - 1 }
        previewPulses.entries.removeIf { it.value <= 0 }
    }

    private fun preview(def: ClientStatDef) {
        val aspect = AspectIdentity.fromStatId(def.id) ?: return
        previewPulses[def.id] = 8
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(ModSounds.forAspect(aspect), 1.0f, .32f))
    }

    private fun decrement(row: Row) {
        val current = workingAlloc[row.def.id] ?: 0
        val committed = originalAlloc[row.def.id] ?: 0
        if (current > committed) {
            val next = current - 1
            if (next == 0) workingAlloc.remove(row.def.id) else workingAlloc[row.def.id] = next
            workingUnspent++
            refreshButtons()
        }
    }

    private fun refreshButtons() {
        var hasDiff = false
        rows.forEach { row ->
            val current = workingAlloc[row.def.id] ?: 0
            val original = originalAlloc[row.def.id] ?: 0
            val cap = if (row.def.maxPoints >= 0) row.def.maxPoints else Int.MAX_VALUE
            row.plus?.active = workingUnspent > 0 && current < cap
            row.minus?.active = current > original
            if (current != original) hasDiff = true
        }
        applyButton?.active = hasDiff
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val layout = layout()
        val properties = buildPropertyRows()
        rightContentHeight = properties.size * PROPERTY_ROW_HEIGHT
        clampScrollOffsets(layout)
        syncRowButtons(layout)

        drawPanel(guiGraphics, layout)
        drawSummary(guiGraphics, layout)
        drawSectionHeaders(guiGraphics, layout)
        drawStatRows(guiGraphics, layout, mouseX, mouseY)
        drawPropertyRows(guiGraphics, layout, properties, mouseX, mouseY)
        drawScrollbar(guiGraphics, layout.contentX + layout.contentWidth - 2, layout.propertyTop,
            layout.propertyViewportHeight, rightContentHeight, rightScrollOffset)
        guiGraphics.fill(layout.panelX + 1, layout.footerTop, layout.panelX + layout.panelWidth - 1, layout.footerTop + 1, RULE_COLOR)

        super.render(guiGraphics, mouseX, mouseY, partialTick)
        children().filterIsInstance<Button>().filter { it.visible }.forEach { button ->
            val x = button.x; val y = button.y; val w = button.width; val h = button.height
            val hover = mouseX in x until (x + w) && mouseY in y until (y + h)
            guiGraphics.fill(x, y, x + w, y + h,
                if (!button.active) 0xFF849382.toInt() else if (hover) 0xFF59755C.toInt() else 0xFF405D49.toInt())
            guiGraphics.fill(x, y, x + w, y + 2, PANEL_BORDER)
            guiGraphics.drawCenteredString(font, button.message, x + w / 2, y + (h - font.lineHeight) / 2, 0xFFF9EFD7.toInt())
        }
        drawTooltip(guiGraphics, layout, mouseX, mouseY)
    }

    private fun drawPanel(guiGraphics: GuiGraphics, layout: StatsScreenLayout) {
        borderedFill(guiGraphics, layout.panelX, layout.panelY, layout.panelX + layout.panelWidth,
            layout.panelY + layout.panelHeight, PANEL_BORDER, PANEL_BACKGROUND)
        val titleText = "RECORD / ATTRIBUTES"
        guiGraphics.drawString(font, titleText, width / 2 - font.width(titleText) / 2,
            layout.panelY + 5, PRIMARY_TEXT, false)
    }

    private fun drawSummary(guiGraphics: GuiGraphics, layout: StatsScreenLayout) {
        val x = layout.contentX
        val y = layout.summaryTop
        val right = layout.contentX + layout.contentWidth
        guiGraphics.fill(x, y, right, y + StatsScreenLayoutPolicy.SUMMARY_HEIGHT, SUMMARY_BACKGROUND)
        guiGraphics.fill(width / 2, y + 3, width / 2 + 1, y + StatsScreenLayoutPolicy.SUMMARY_HEIGHT - 3, RULE_COLOR)
        val points = Component.translatable("screen.rpg_stats.unspent", workingUnspent.toString()).string
        val peak = Component.translatable("screen.rpg_stats.life_peak", lifePeak.toString()).string
        guiGraphics.drawString(font, ellipsize(points, layout.contentWidth / 2 - 8), x + 4, y + 4, PRIMARY_TEXT, false)
        guiGraphics.drawString(font, ellipsize(peak, layout.contentWidth / 2 - 8), right - 4 - font.width(ellipsize(peak, layout.contentWidth / 2 - 8)), y + 4, PRIMARY_TEXT, false)
    }

    private fun drawSectionHeaders(guiGraphics: GuiGraphics, layout: StatsScreenLayout) {
        guiGraphics.fill(layout.contentX, layout.attributeHeaderTop, layout.contentX + layout.contentWidth,
            layout.attributeHeaderTop + StatsScreenLayoutPolicy.HEADER_HEIGHT, HEADER_BACKGROUND)
        guiGraphics.fill(layout.contentX, layout.propertyHeaderTop, layout.contentX + layout.contentWidth,
            layout.propertyHeaderTop + StatsScreenLayoutPolicy.HEADER_HEIGHT, HEADER_BACKGROUND)
        guiGraphics.drawString(font, Component.translatable("screen.rpg_stats.left_header"),
            layout.contentX + 4, layout.attributeHeaderTop + 1, PRIMARY_TEXT, false)
        guiGraphics.drawString(font, Component.translatable("screen.rpg_stats.right_header"),
            layout.contentX + 4, layout.propertyHeaderTop + 1, PRIMARY_TEXT, false)
    }

    private fun drawStatRows(guiGraphics: GuiGraphics, layout: StatsScreenLayout, mouseX: Int, mouseY: Int) {
        rows.forEachIndexed { index, row ->
            val rowX = layout.attributeX(index)
            val rowY = layout.attributeY(index)
            val hovered = mouseX in rowX until (rowX + layout.attributeTileWidth) &&
                mouseY in rowY until (rowY + layout.attributeTileHeight)
            val pulse = previewPulses[row.def.id] ?: 0
            guiGraphics.fill(rowX, rowY, rowX + layout.attributeTileWidth, rowY + layout.attributeTileHeight - 1,
                if (pulse > 0) ((0x50 + pulse * 8) shl 24) or (row.def.color and 0xFFFFFF)
                else if (hovered) ROW_HOVER else if (index % 2 == 0) ROW_BACKGROUND else ROW_ALTERNATE)
            guiGraphics.fill(rowX, rowY, rowX + 3, rowY + layout.attributeTileHeight - 1, opaque(row.def.color))
            val aspect = AspectIdentity.fromStatId(row.def.id)
            if (aspect != null) {
                guiGraphics.blit(
                    AspectIdentity.BADGES,
                    rowX + StatsScreenLayoutPolicy.BADGE_X,
                    rowY + StatsScreenLayoutPolicy.BADGE_Y,
                    (aspect.index * StatsScreenLayoutPolicy.BADGE_SIZE).toFloat(),
                    0f,
                    StatsScreenLayoutPolicy.BADGE_SIZE,
                    StatsScreenLayoutPolicy.BADGE_SIZE,
                    StatsScreenLayoutPolicy.BADGE_SIZE * AspectIdentity.entries.size,
                    StatsScreenLayoutPolicy.BADGE_SIZE
                )
            }
            val textX = if (aspect == null) rowX + 6 else rowX + StatsScreenLayoutPolicy.BADGE_X +
                StatsScreenLayoutPolicy.BADGE_SIZE + StatsScreenLayoutPolicy.BADGE_TEXT_GAP
            val maxNameWidth = rowX + layout.attributeTileWidth - CONTROL_WIDTH - 4 - textX
            guiGraphics.enableScissor(textX, rowY, textX + maxNameWidth, rowY + 13)
            guiGraphics.drawString(font, Component.translatable(row.def.nameKey),
                textX, rowY + 3, PRIMARY_TEXT, false)
            guiGraphics.disableScissor()
            val points = workingAlloc[row.def.id] ?: 0
            val detailX = textX
            guiGraphics.drawString(font, ellipsize(buildRowSummary(row.def, points).string,
                rowX + layout.attributeTileWidth - detailX - 6),
                detailX, rowY + StatsScreenLayoutPolicy.DETAIL_TOP, SECONDARY_TEXT, false)
            val counter = formatPointCounter(row.def, points)
            val counterCenter = rowX + layout.attributeTileWidth - 31
            guiGraphics.drawString(font, counter, counterCenter - font.width(counter) / 2, rowY + 4, PRIMARY_TEXT, false)
        }
    }

    private fun drawPropertyRows(guiGraphics: GuiGraphics, layout: StatsScreenLayout, properties: List<PropertyRow>, mouseX: Int, mouseY: Int) {
        guiGraphics.enableScissor(layout.contentX, layout.propertyTop, layout.contentX + layout.contentWidth,
            layout.propertyTop + layout.propertyViewportHeight)
        if (properties.isEmpty()) {
            val empty = Component.translatable("screen.rpg_stats.no_properties").string
            guiGraphics.drawCenteredString(font, ellipsize(empty, layout.contentWidth - 16),
                layout.contentX + layout.contentWidth / 2, layout.propertyTop + 6, SECONDARY_TEXT)
        }
        properties.forEachIndexed { index, property ->
            val rowY = layout.propertyTop + index * PROPERTY_ROW_HEIGHT - rightScrollOffset.toInt()
            if (rowY + PROPERTY_ROW_HEIGHT < layout.propertyTop || rowY > layout.propertyTop + layout.propertyViewportHeight) return@forEachIndexed
            val hovered = mouseX in layout.contentX until (layout.contentX + layout.contentWidth) &&
                mouseY in rowY until (rowY + PROPERTY_ROW_HEIGHT) &&
                mouseY in layout.propertyTop until (layout.propertyTop + layout.propertyViewportHeight)
            guiGraphics.fill(layout.contentX, rowY, layout.contentX + layout.contentWidth, rowY + PROPERTY_ROW_HEIGHT - 1,
                if (hovered) ROW_HOVER else if (index % 2 == 0) ROW_BACKGROUND else ROW_ALTERNATE)
            guiGraphics.fill(layout.contentX, rowY, layout.contentX + 3, rowY + PROPERTY_ROW_HEIGHT - 1, opaque(property.color))
            val textX = layout.contentX + 8
            val textWidth = layout.contentWidth - 16
            val sourceSuffix = if (property.sourceName != property.friendlyName) {
                " ← ${compactSourceName(property.friendlyName, property.sourceName)}"
            } else ""
            guiGraphics.drawString(font, ellipsize(property.friendlyName + sourceSuffix, textWidth),
                textX, rowY + 2, PRIMARY_TEXT, false)
            val current = formatEffectValue(property.originalTotal, property.displayAsPercent)
            val totals = StatsPropertyTotals(property.originalTotal, property.workingTotal)
            val changed = StatsPropertyVisibility.hasPendingChange(totals)
            val values = if (changed) {
                val pending = formatEffectValue(property.workingTotal, property.displayAsPercent)
                Component.translatable("screen.rpg_stats.current_pending", current, pending).string
            } else Component.translatable("screen.rpg_stats.current", current).string
            val valueColor = when (StatsPropertyVisibility.direction(totals)) {
                1 -> PENDING_UP
                -1 -> PENDING_DOWN
                else -> SECONDARY_TEXT
            }
            guiGraphics.drawString(font, ellipsize(values, textWidth), textX, rowY + 12, valueColor, false)
        }
        guiGraphics.disableScissor()
    }

    private fun drawScrollbar(guiGraphics: GuiGraphics, x: Int, y: Int, viewportHeight: Int,
                              contentHeight: Int, offset: Double) {
        if (contentHeight <= viewportHeight) return
        guiGraphics.fill(x, y, x + 2, y + viewportHeight, 0xAA9E8C68.toInt())
        val thumbHeight = maxOf(16, viewportHeight * viewportHeight / contentHeight)
        val maxScroll = contentHeight - viewportHeight
        val thumbY = y + (offset / maxScroll * (viewportHeight - thumbHeight)).toInt()
        guiGraphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xFF496952.toInt())
    }

    private fun drawTooltip(guiGraphics: GuiGraphics, layout: StatsScreenLayout, mouseX: Int, mouseY: Int) {
        if (rows.any { row ->
                row.minus?.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) == true ||
                    row.plus?.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) == true
            }) return
        if (mouseX !in layout.contentX until (layout.contentX + layout.contentWidth) ||
            mouseY !in layout.attributeTop until layout.propertyHeaderTop) return
        val column = if (mouseX < layout.contentX + layout.attributeTileWidth + StatsScreenLayoutPolicy.TILE_GAP) 0 else 1
        val gridRow = (mouseY - layout.attributeTop) / layout.attributeTileHeight
        val index = gridRow * 2 + column
        val row = rows.getOrNull(index) ?: return
        guiGraphics.renderComponentTooltip(font, buildTooltip(row.def), mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollDelta: Double): Boolean {
        val layout = layout()
        if (mouseY !in layout.propertyTop.toDouble()..(layout.propertyTop + layout.propertyViewportHeight).toDouble())
            return super.mouseScrolled(mouseX, mouseY, scrollDelta)
        if (mouseX in layout.contentX.toDouble()..(layout.contentX + layout.contentWidth).toDouble()) {
            rightScrollOffset = scroll(rightScrollOffset, rightContentHeight, layout.propertyViewportHeight, scrollDelta)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta)
    }

    private fun scroll(offset: Double, contentHeight: Int, viewportHeight: Int, delta: Double): Double =
        Mth.clamp(offset - delta * 12, 0.0, maxOf(0, contentHeight - viewportHeight).toDouble())

    private fun clampScrollOffsets(layout: StatsScreenLayout) {
        rightScrollOffset = Mth.clamp(rightScrollOffset, 0.0,
            maxOf(0, rightContentHeight - layout.propertyViewportHeight).toDouble())
    }

    private fun syncRowButtons(layout: StatsScreenLayout) {
        rows.forEachIndexed { index, row ->
            val x = layout.attributeX(index)
            val y = layout.attributeY(index) + StatsScreenLayoutPolicy.CONTROL_TOP
            row.minus?.y = y
            row.plus?.y = y
            row.minus?.x = x + layout.attributeTileWidth - CONTROL_WIDTH
            row.plus?.x = x + layout.attributeTileWidth - StatsScreenLayoutPolicy.CONTROL_SIZE - 2
            row.minus?.visible = true
            row.plus?.visible = true
        }
    }

    private fun buildPropertyRows(): List<PropertyRow> {
        val providers = linkedMapOf<String, PropertyProvider>()
        rows.forEach { row ->
            val sourceName = Component.translatable(row.def.nameKey).string
            row.def.effects.forEach { effect ->
                val current = providers[effect.attributeId]
                val candidate = PropertyProvider(effect.attributeId, effect.operation,
                    resolveAttributeName(effect.attributeId).string, sourceName,
                    if (effect.isPrimary) row.def.color else lightenColor(row.def.color),
                    effect.curve.perPoint, effect.isPrimary, effect.displayAsPercent)
                if (current == null || (candidate.primary && !current.primary) ||
                    (candidate.primary == current.primary && candidate.perPoint > current.perPoint)) {
                    providers[effect.attributeId] = candidate
                }
            }
        }
        return providers.values.mapNotNull { provider ->
            var originalTotal = 0.0
            var workingTotal = 0.0
            rows.forEach { row ->
                val originalPoints = originalAlloc[row.def.id] ?: 0
                val workingPoints = workingAlloc[row.def.id] ?: 0
                row.def.effects.filter { it.attributeId == provider.attributeId }.forEach { effect ->
                    originalTotal += Curves.eval(originalPoints, effect.curve.toCommon())
                    workingTotal += Curves.eval(workingPoints, effect.curve.toCommon())
                }
            }
            val totals = StatsPropertyTotals(originalTotal, workingTotal)
            if (!StatsPropertyVisibility.isVisible(totals)) null else PropertyRow(
                provider.friendlyName, provider.sourceName, provider.color,
                provider.operation, provider.displayAsPercent, originalTotal, workingTotal
            )
        }
    }

    private fun buildRowSummary(def: ClientStatDef, currentPoints: Int): Component {
        val primary = def.effects.firstOrNull { it.isPrimary } ?: def.effects.firstOrNull()
            ?: return Component.translatable("tooltip.rpg_stats.no_effects")
        val attributeName = resolveAttributeName(primary.attributeId)
        val statName = Component.translatable(def.nameKey).string
        val result = if (attributeName.string.equals(statName, ignoreCase = true)) {
            Component.translatable("screen.rpg_stats.next_point_same", formatMarginalGain(primary, currentPoints))
        } else {
            Component.translatable("screen.rpg_stats.next_point", formatMarginalGain(primary, currentPoints), attributeName)
        }
        val secondaryCount = (def.effects.size - 1).coerceAtLeast(0)
        if (secondaryCount > 0) result.append(Component.translatable("screen.rpg_stats.more_effects", secondaryCount.toString()))
        return result
    }

    private fun buildTooltip(def: ClientStatDef): List<Component> {
        if (def.effects.isEmpty()) return listOf(Component.translatable("tooltip.rpg_stats.no_effects"))
        val currentPoints = workingAlloc[def.id] ?: 0
        val aspect = AspectIdentity.fromStatId(def.id)
        val identityLine = if (aspect == null) {
            Component.translatable("tooltip.rpg_stats.aspect", Component.translatable("aspect.rpg_stats.${def.id.substringAfter(':')}"))
                .withStyle { it.withColor(def.color) }
        } else {
            Component.literal(aspect.badge).withStyle { it.withFont(AspectIdentity.FONT) }
                .append(Component.literal(" ").withStyle { it.withFont(DEFAULT_FONT) })
                .append(Component.translatable("tooltip.rpg_stats.aspect", Component.literal(aspect.label))
                    .withStyle { it.withFont(DEFAULT_FONT).withColor(def.color) })
        }
        val lines = mutableListOf(
            identityLine,
            Component.translatable("tooltip.rpg_stats.points_header", formatPointCounter(def, currentPoints)),
            Component.translatable("tooltip.rpg_stats.effects_header")
        )
        def.effects.forEach { effect ->
            val current = Curves.eval(currentPoints, effect.curve.toCommon())
            val next = Curves.eval(currentPoints + 1, effect.curve.toCommon()) - current
            val color = if (effect.isPrimary) def.color else lightenColor(def.color)
            val role = if (effect.isPrimary) "tooltip.rpg_stats.main_effect" else "tooltip.rpg_stats.bonus_effect"
            lines += Component.translatable(role).append(Component.literal(": "))
                .append(resolveAttributeName(effect.attributeId).copy().withStyle { it.withColor(color) })
                .append(Component.literal(" ")).append(Component.translatable("tooltip.rpg_stats.now_next",
                    formatEffectValue(current, effect.displayAsPercent), formatEffectValue(next, effect.displayAsPercent)))
        }
        return lines
    }

    private fun resolveAttributeName(attributeId: String): Component {
        val id = ResourceLocation.tryParse(attributeId) ?: return Component.literal(attributeId)
        val key = "attr.${id.namespace}.${id.path}"
        val translated = Component.translatable(key)
        return if (translated.string == key) Component.literal(attributeId) else translated
    }

    private fun compactSourceName(propertyName: String, sourceName: String): String {
        val propertyWords = propertyName.split(' ')
        val sourceWords = sourceName.split(' ').toMutableList()
        while (propertyWords.isNotEmpty() && sourceWords.size > 1 &&
            propertyWords.last().equals(sourceWords.last(), ignoreCase = true)) {
            sourceWords.removeLast()
        }
        return sourceWords.joinToString(" ")
    }

    private fun formatMarginalGain(effect: ClientEffectDef, currentPoints: Int): String {
        val current = Curves.eval(currentPoints, effect.curve.toCommon())
        val next = Curves.eval(currentPoints + 1, effect.curve.toCommon())
        return formatEffectValue(next - current, effect.displayAsPercent)
    }

    private fun formatPointCounter(def: ClientStatDef, points: Int): String =
        if (def.maxPoints >= 0) "$points/${def.maxPoints}" else points.toString()

    private fun formatEffectValue(value: Double, multiplier: Boolean): String =
        if (multiplier) formatSignedPercent(value) else formatSignedNumber(value)

    private fun formatSignedPercent(value: Double): String {
        val percent = value * 100.0
        return "${if (percent >= 0.0) "+" else ""}${trimNumber(percent)}%"
    }

    private fun formatSignedNumber(value: Double): String =
        "${if (value >= 0.0) "+" else ""}${trimNumber(value)}"

    private fun trimNumber(value: Double): String = when {
        abs(value) >= 100.0 -> String.format("%.0f", value)
        abs(value) >= 10.0 -> String.format("%.1f", value)
        abs(value) >= 1.0 -> String.format("%.2f", value)
        abs(value) >= 0.01 -> String.format("%.3f", value)
        else -> String.format("%.4f", value)
    }

    private fun ClientCurveDef.toCommon() = CurveDef(type, cap, k, perPoint, min, max)

    private fun lightenColor(color: Int, factor: Double = 1.35): Int {
        val red = minOf(255, (((color shr 16) and 0xFF) * factor).toInt())
        val green = minOf(255, (((color shr 8) and 0xFF) * factor).toInt())
        val blue = minOf(255, ((color and 0xFF) * factor).toInt())
        return (red shl 16) or (green shl 8) or blue
    }

    private fun opaque(color: Int): Int = color or 0xFF000000.toInt()

    private fun ellipsize(text: String, maxWidth: Int): String {
        if (font.width(text) <= maxWidth) return text
        val ellipsis = "…"
        return font.plainSubstrByWidth(text, maxOf(0, maxWidth - font.width(ellipsis))) + ellipsis
    }

    private fun borderedFill(guiGraphics: GuiGraphics, left: Int, top: Int, right: Int, bottom: Int,
                             border: Int, fill: Int) {
        guiGraphics.fill(left, top, right, bottom, border)
        guiGraphics.fill(left + 1, top + 1, right - 1, bottom - 1, fill)
    }

    private fun layout(): StatsScreenLayout = StatsScreenLayoutPolicy.calculate(width, height, rows.size)

    override fun isPauseScreen(): Boolean = false
}
