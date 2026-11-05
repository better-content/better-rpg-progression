package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.client.cache.ClientStatDef
import com.bettercontent.betterrpgprogression.client.cache.ClientEffectDef
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.network.packets.C2SApplyStats
import com.bettercontent.betterrpgprogression.common.salience.AspectIdentity
import com.bettercontent.betterrpgprogression.common.sound.ModSounds
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.language.I18n
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraftforge.registries.ForgeRegistries

/** The eight-build overview stays visible until a card is selected for its complete effect sheet. */
class StatsScreen @JvmOverloads constructor(private val returnTo: Screen? = null) :
    Screen(Component.translatable("screen.better_rpg_progression.title")) {
    private companion object {
        const val BACKGROUND = 0xFF12221D.toInt()
        const val CARD = 0xFF1D3028.toInt()
        const val CARD_HOVER = 0xFF22382E.toInt()
        const val BORDER = 0xFF415346.toInt()
        const val INK = 0xFFF2E8D0.toInt()
        const val MUTED = 0xFFAFBDAE.toInt()
        const val GOLD = 0xFFC4AA79.toInt()
        const val GAIN = 0xFFA9D5B0.toInt()
        const val DISABLED = 0xFF718275.toInt()
    }

    private data class Row(val def: ClientStatDef, val minus: Button, val plus: Button, val details: Button)
    private val snapshot = ClientCache.stats
    private val draft = StatsAllocationDraft(snapshot)
    private var rows: List<Row> = emptyList()
    private var selected: Int? = null
    private var detailMinus: Button? = null
    private var detailPlus: Button? = null
    private var overview: Button? = null
    private var commit: Button? = null
    private var reset: Button? = null
    private val pulses = mutableMapOf<String, Int>()

    override fun init() {
        super.init()
        val layout = layout()
        rows = ClientCache.defs.mapIndexed { index, def ->
            val x = layout.cardX(index)
            val y = layout.cardY(index) + layout.cardHeight - 26
            val minus = button(text("decrease", Component.translatable(def.nameKey)), x + 10, y, 18, 18, Component.literal("−")) {
                if (draft.remove(def.id)) refreshButtons()
            }
            val plus = button(text("increase", Component.translatable(def.nameKey)), x + layout.cardWidth - 28, y, 18, 18, Component.literal("+")) {
                addPoint(def)
            }
            addRenderableWidget(minus)
            addRenderableWidget(plus)
            val details = addRenderableWidget(button(text("open_build", Component.translatable(def.nameKey)),
                x + 10, y - 19, layout.cardWidth - 20, 15) { select(index) })
            Row(def, minus, plus, details)
        }
        overview = addRenderableWidget(button(text("overview"), 16, 76, 90, 20, text("overview")) { select(null) })
        detailMinus = addRenderableWidget(button(text("decrease_selected"), layout.width - 118, layout.footerTop - 28,
            24, 20, Component.literal("−")) {
            selected?.let { index -> if (draft.remove(rows[index].def.id)) refreshButtons() }
        })
        detailPlus = addRenderableWidget(button(text("increase_selected"), layout.width - 78, layout.footerTop - 28,
            24, 20, Component.literal("+")) {
            selected?.let { index -> addPoint(rows[index].def) }
        })
        val actionY = layout.height - 32
        addRenderableWidget(button(text("auto_plan"), 16, actionY, 88, 20) {
            minecraft?.setScreen(AutoAllocationScreen(this))
        })
        reset = addRenderableWidget(button(text("reset_draft"), 112, actionY, 88, 20) {
            draft.reset()
            refreshButtons()
        })
        addRenderableWidget(button(text("back"), layout.width - 204, actionY, 64, 20) { onClose() })
        commit = addRenderableWidget(button(text("commit"), layout.width - 132, actionY, 116, 20, primary = true) {
            if (draft.spent > 0) {
                Network.sendToServer(C2SApplyStats(draft.allocations.toMap()))
                onClose()
            }
        })
        refreshButtons()
    }

    private fun addPoint(def: ClientStatDef) {
        if (draft.add(def.id, def.maxPoints)) {
            AspectIdentity.fromStatId(def.id)?.let { aspect ->
                pulses[def.id] = 8
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(ModSounds.forAspect(aspect), 1f, .25f))
            }
            refreshButtons()
        }
    }

    private fun select(index: Int?) {
        selected = index
        refreshButtons()
    }

    private fun refreshButtons() {
        rows.forEachIndexed { index, row ->
            row.plus.visible = selected == null
            row.minus.visible = selected == null
            row.details.visible = selected == null
            row.plus.active = draft.canAdd(row.def.id, row.def.maxPoints)
            row.minus.active = draft.pending(row.def.id) > 0
        }
        overview?.visible = selected != null
        detailPlus?.visible = selected != null
        detailMinus?.visible = selected != null
        val def = selected?.let { rows.getOrNull(it)?.def }
        if (def != null) overview?.message = detailNarration(def)
        detailPlus?.active = def != null && draft.canAdd(def.id, def.maxPoints)
        detailMinus?.active = def != null && draft.pending(def.id) > 0
        commit?.active = draft.spent > 0
        commit?.message = when (draft.spent) {
            0 -> text("commit")
            1 -> text("commit_one", draft.spent)
            else -> text("commit_count", draft.spent)
        }
        reset?.active = draft.spent > 0
    }

    override fun tick() {
        super.tick()
        pulses.replaceAll { _, ticks -> ticks - 1 }
        pulses.entries.removeIf { it.value <= 0 }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val layout = layout()
        val mx = layout.logical(mouseX.toDouble()).toInt()
        val my = layout.logical(mouseY.toDouble()).toInt()
        graphics.fill(0, 0, width, height, BACKGROUND)
        graphics.pose().pushPose()
        graphics.pose().scale(layout.scale, layout.scale, 1f)
        drawHeader(graphics, layout)
        val selectedDef = selected?.let { rows.getOrNull(it)?.def }
        if (selectedDef == null) rows.forEachIndexed { index, row -> drawCard(graphics, layout, index, row, mx, my) }
        else drawDetail(graphics, layout, selectedDef)
        graphics.fill(16, layout.footerTop, layout.width - 16, layout.footerTop + 1, BORDER)
        graphics.drawString(font, text(if (selectedDef == null) "overview_hint" else "detail_hint"), 16, layout.footerTop + 7, MUTED, false)
        val permanence = text("life_rule").string
        graphics.drawString(font, permanence, layout.width - 16 - font.width(permanence), layout.footerTop + 20, MUTED, false)
        super.render(graphics, mx, my, partialTick)
        graphics.pose().popPose()
    }

    private fun drawHeader(graphics: GuiGraphics, layout: StatsScreenLayout) {
        graphics.drawString(font, text("record"), 16, 12, GOLD, false)
        graphics.pose().pushPose()
        graphics.pose().translate(16.0, 28.0, 0.0)
        graphics.pose().scale(1.5f, 1.5f, 1f)
        graphics.drawString(font, text("learning_title"), 0, 0, INK, false)
        graphics.pose().popPose()
        graphics.drawString(font, text("learning_hint"), 16, 53, MUTED, false)
        val available = text(if (draft.unspent == 1) "available_one" else "available_short", draft.unspent).string
        graphics.drawString(font, available, layout.width - 16 - font.width(available), 20, GAIN, false)
        val peak = text("life_peak", snapshot.lifePeak).string
        graphics.drawString(font, peak, layout.width - 16 - font.width(peak), 36, MUTED, false)
    }

    private fun drawCard(graphics: GuiGraphics, layout: StatsScreenLayout, index: Int, row: Row, mouseX: Int, mouseY: Int) {
        val def = row.def
        val x = layout.cardX(index)
        val y = layout.cardY(index)
        val w = layout.cardWidth
        val h = layout.cardHeight
        val hovered = mouseX in x until (x + w) && mouseY in y until (y + h)
        graphics.fill(x, y, x + w, y + h, BORDER)
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, if (hovered) CARD_HOVER else CARD)
        graphics.fill(x + 1, y + 1, x + w - 1, y + 3, def.color or 0xFF000000.toInt())
        val accent = readableAccent(def.color)
        if ((pulses[def.id] ?: 0) > 0) graphics.fill(x + 1, y + 3, x + 3, y + h - 1, accent)
        AspectIdentity.fromStatId(def.id)?.let { aspect ->
            graphics.blit(AspectIdentity.BADGES, x + 9, y + 9, (aspect.index * 18).toFloat(), 0f,
                18, 18, 18 * AspectIdentity.entries.size, 18)
        }
        graphics.drawString(font, Component.translatable(def.nameKey), x + 33, y + 13, accent, false)
        val purpose = purpose(def)
        font.split(purpose, w - 20).take(2).forEachIndexed { line, formatted ->
            graphics.drawString(font, formatted, x + 10, y + 34 + line * 10, INK, false)
        }
        val points = draft.points(def.id)
        val movementBase = movementBase()
        def.effects.filter { it.isPrimary }.ifEmpty { def.effects }.take(2).forEachIndexed { effectIndex, effect ->
            val reading = effectReading(def, effect, points, movementBase)
            val name = effectName(effect.attributeId).string
            val value = if (effect.available) StatsEffectPresentation.format(reading.bonus, reading.percent)
                else text("unavailable_short").string
            val labelWidth = (w - 28 - font.width(value)).coerceAtLeast(20)
            graphics.drawString(font, font.plainSubstrByWidth(name, labelWidth), x + 10, y + 68 + effectIndex * 16, MUTED, false)
            graphics.drawString(font, value, x + w - 10 - font.width(value), y + 68 + effectIndex * 16,
                if (effect.available) GAIN else DISABLED, false)
        }
        val controlsY = y + h - 26
        graphics.fill(x + 10, controlsY - 6, x + w - 10, controlsY - 5, BORDER)
        val pending = draft.pending(def.id)
        val counter = if (def.maxPoints >= 0) "$points/${def.maxPoints}" else points.toString()
        val count = if (pending > 0) text(if (points == 1) "point_draft" else "points_draft", counter, pending)
            else text(if (points == 1) "point_count" else "points_count", counter)
        graphics.drawString(font, count, x + (w - font.width(count)) / 2, controlsY + 5,
            if (pending > 0) GAIN else MUTED, false)
    }

    private fun drawDetail(graphics: GuiGraphics, layout: StatsScreenLayout, def: ClientStatDef) {
        val x = 16
        val y = 76
        val w = layout.width - 32
        val bottom = layout.footerTop - 6
        graphics.fill(x, y, x + w, bottom, BORDER)
        graphics.fill(x + 1, y + 1, x + w - 1, bottom - 1, CARD)
        val accent = readableAccent(def.color)
        graphics.drawString(font, Component.translatable(def.nameKey), x + 108, y + 7, accent, false)
        graphics.drawString(font, purpose(def), x + 108, y + 23, INK, false)
        val points = draft.points(def.id)
        val pending = draft.pending(def.id)
        graphics.drawString(font, text("detail_points", points, pending), x + 14, y + 47, GAIN, false)
        val effects = def.effects
        if (effects.isEmpty()) graphics.drawString(font, text("no_active_effects"), x + 14, y + 75, MUTED, false)
        val groups = effects.map { StatsEffectPresentation.group(it.attributeId) }
        val groupCount = groups.distinct().size
        val rowHeight = minOf(34, (bottom - y - 125 - groupCount * 15) / effects.size.coerceAtLeast(1)).coerceAtLeast(19)
        val valueX = x + w - 117
        val nextX = x + w - 15
        graphics.drawString(font, text("effect_column"), x + 14, y + 70, MUTED, false)
        graphics.drawString(font, text("bonus_column"), valueX - 55, y + 70, MUTED, false)
        graphics.drawString(font, text("next_column"), nextX - 52, y + 70, GAIN, false)
        val movementBase = movementBase()
        var rowY = y + 85
        effects.forEachIndexed { index, effect ->
            val group = groups[index]
            if (index == 0 || group != groups[index - 1]) {
                graphics.drawString(font, text("group_$group"), x + 15, rowY, GOLD, false)
                rowY += 15
            }
            if (rowY + 18 >= bottom - 32) return@forEachIndexed
            if (effect.isPrimary) graphics.fill(x + 9, rowY - 3, x + 11, rowY + 14, accent)
            val name = effectName(effect.attributeId).string
            graphics.drawString(font, font.plainSubstrByWidth(name, (valueX - x - 85).coerceAtLeast(30)), x + 16, rowY,
                if (effect.available) INK else DISABLED, false)
            val isRange = effect.attributeId == "goety:spell_range"
            if (!effect.available) {
                graphics.drawString(font, text("unavailable"), valueX - 90, rowY, DISABLED, false)
            } else {
                val reading = effectReading(def, effect, points, movementBase)
                val bonus = StatsEffectPresentation.format(reading.bonus, reading.percent)
                val next = if (def.maxPoints >= 0 && points >= def.maxPoints) "—"
                    else StatsEffectPresentation.format(reading.next, reading.percent)
                graphics.drawString(font, bonus, valueX - font.width(bonus), rowY, INK, false)
                graphics.drawString(font, next, nextX - font.width(next), rowY, GAIN, false)
            }
            val milestone = if (isRange && effect.available) spellRangeAt(def, effect, 20)
                else StatsEffectPresentation.read(effect, 20, movementBase)
            val landmark = StatsEffectPresentation.format(milestone.bonus, milestone.percent)
            val landmarkLabel = text(if (effect.available) "landmark_20" else "landmark_20_unavailable", landmark)
            if (isRange && effect.available) {
                val raw = StatsEffectPresentation.read(effect, points)
                val progress = text("range_progress", StatsEffectPresentation.format(raw.bonus, false),
                    StatsEffectPresentation.format(raw.next, false))
                graphics.drawString(font, progress, x + 16, rowY + 11, MUTED, false)
                val live = liveAttribute(effect.attributeId) ?: 0.0
                val threshold = StatsEffectPresentation.nextSpellRangePoint(effect, points,
                    snapshot.allocations[def.id] ?: 0, live, if (def.maxPoints >= 0) def.maxPoints else 20)
                val thresholdText = threshold?.let { text("range_threshold", it) } ?: text("range_no_threshold")
                graphics.drawString(font, landmarkLabel.copy().append("  ·  ").append(thresholdText),
                    x + 16, rowY + 22, GOLD, false)
            } else {
                graphics.drawString(font, landmarkLabel, x + 16, rowY + 11,
                    if (effect.available) GOLD else DISABLED, false)
                if (def.id.substringAfter(':') == "skirmisher" && effect.available) {
                    val equipped = StatsEffectPresentation.formatEquipped(liveAttribute(effect.attributeId))
                    graphics.drawString(font, if (equipped == null) text("equipped_unknown")
                        else text("equipped_total", equipped), x + 16, rowY + 22, MUTED, false)
                }
            }
            if (index < effects.lastIndex) graphics.fill(x + 14, rowY + rowHeight - 1, x + w - 14, rowY + rowHeight, BORDER)
            rowY += rowHeight
        }
    }

    /** Screen text is drawn on a canvas; keep its numbers available to keyboard narration too. */
    private fun detailNarration(def: ClientStatDef): Component {
        val summary = text("overview").copy().append(". ").append(Component.translatable(def.nameKey))
        val points = draft.points(def.id)
        def.effects.forEach { effect ->
            summary.append(". ").append(effectName(effect.attributeId)).append(": ")
            if (!effect.available) {
                summary.append(text("unavailable"))
                summary.append(". ").append(text("landmark_20_unavailable",
                    StatsEffectPresentation.format(StatsEffectPresentation.read(effect, 20).bonus,
                        StatsEffectPresentation.read(effect, 20).percent)))
            } else {
                val reading = effectReading(def, effect, points, movementBase())
                summary.append(text("narrated_effect", StatsEffectPresentation.format(reading.bonus, reading.percent),
                    StatsEffectPresentation.format(reading.next, reading.percent)))
                val target = if (effect.attributeId == "goety:spell_range") spellRangeAt(def, effect, 20)
                    else StatsEffectPresentation.read(effect, 20, movementBase())
                summary.append(". ").append(text("landmark_20", StatsEffectPresentation.format(target.bonus, target.percent)))
                if (effect.attributeId == "goety:spell_range") {
                    val threshold = StatsEffectPresentation.nextSpellRangePoint(effect, points,
                        snapshot.allocations[def.id] ?: 0, liveAttribute(effect.attributeId) ?: 0.0,
                        if (def.maxPoints >= 0) def.maxPoints else 20)
                    summary.append(". ").append(threshold?.let { text("range_threshold", it) } ?: text("range_no_threshold"))
                }
                if (def.id.substringAfter(':') == "skirmisher") {
                    StatsEffectPresentation.formatEquipped(liveAttribute(effect.attributeId))?.let {
                        summary.append(". ").append(text("equipped_total", it))
                    }
                }
            }
        }
        return summary
    }

    private fun effectReading(def: ClientStatDef, effect: ClientEffectDef, points: Int, movementBase: Double) =
        if (effect.attributeId == "goety:spell_range") spellRangeAt(def, effect, points)
        else StatsEffectPresentation.read(effect, points, movementBase)

    private fun spellRangeAt(def: ClientStatDef, effect: ClientEffectDef, points: Int) =
        StatsEffectPresentation.effectiveSpellRange(effect, points, snapshot.allocations[def.id] ?: 0,
            liveAttribute(effect.attributeId) ?: 0.0)

    private fun liveAttribute(id: String): Double? {
        val attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(id) ?: return null) ?: return null
        return minecraft?.player?.getAttribute(attribute)?.value
    }

    private fun movementBase() = minecraft?.player?.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue ?: 0.1
    private fun purpose(def: ClientStatDef): Component {
        val key = "${def.nameKey}.purpose"
        return if (I18n.exists(key)) Component.translatable(key) else text("generic_purpose")
    }

    private fun button(message: Component, x: Int, y: Int, width: Int, height: Int,
                       symbol: Component? = null, primary: Boolean = false, action: (Button) -> Unit): Button =
        AllocationButton(x, y, width, height, message, Button.OnPress { action(it) }, symbol, primary)

    private class AllocationButton(x: Int, y: Int, width: Int, height: Int, message: Component,
                                   onPress: OnPress, private val symbol: Component?, private val primary: Boolean) :
        Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {
        override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val font = Minecraft.getInstance().font
            val highlighted = active && (isHovered || isFocused)
            graphics.fill(x, y, x + width, y + height,
                if (primary && active) 0xFF466B4D.toInt() else if (highlighted) 0xFF3A5541.toInt() else 0xFF293E31.toInt())
            graphics.fill(x, y, x + width, y + 1, if (highlighted || primary && active) GOLD else BORDER)
            val label = symbol ?: message
            graphics.drawString(font, label, x + (width - font.width(label)) / 2,
                y + (height - font.lineHeight) / 2, if (active) INK else DISABLED, false)
        }
    }

    private fun effectName(id: String): Component {
        val location = ResourceLocation.tryParse(id) ?: return Component.literal(id)
        val shortKey = "effect.better_rpg_progression.${location.namespace}.${location.path}"
        return Component.translatable(if (I18n.exists(shortKey)) shortKey else "attr.${location.namespace}.${location.path}")
    }

    private fun readableAccent(color: Int): Int {
        fun channel(shift: Int) = ((((color shr shift) and 255) * .55 + 255 * .45).toInt() shl shift)
        return 0xFF000000.toInt() or channel(16) or channel(8) or channel(0)
    }

    private fun text(key: String, vararg args: Any) = Component.translatable("screen.better_rpg_progression.$key", *args)
    private fun layout() = StatsScreenLayoutPolicy.calculate(width, height, ClientCache.defs.size)
    override fun onClose() { minecraft?.setScreen(returnTo) }
    override fun isPauseScreen() = false

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val l = layout()
        val x = l.logical(mouseX)
        val y = l.logical(mouseY)
        if (super.mouseClicked(x, y, button)) return true
        if (button == 0 && selected == null) {
            rows.indices.firstOrNull { x >= l.cardX(it) && x < l.cardX(it) + l.cardWidth &&
                y >= l.cardY(it) && y < l.cardY(it) + l.cardHeight }?.let { select(it); return true }
        }
        return false
    }
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val l = layout()
        return super.mouseReleased(l.logical(mouseX), l.logical(mouseY), button)
    }
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        val l = layout()
        return super.mouseDragged(l.logical(mouseX), l.logical(mouseY), button, l.logical(dragX), l.logical(dragY))
    }
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val l = layout()
        super.mouseMoved(l.logical(mouseX), l.logical(mouseY))
    }
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollDelta: Double): Boolean {
        val l = layout()
        return super.mouseScrolled(l.logical(mouseX), l.logical(mouseY), scrollDelta)
    }
}
