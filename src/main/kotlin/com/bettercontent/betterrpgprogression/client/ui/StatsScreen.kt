package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.client.cache.ClientEffectDef
import com.bettercontent.betterrpgprogression.client.cache.ClientStatDef
import com.bettercontent.betterrpgprogression.client.ui.StatsScreenLayoutPolicy.CARD_PADDING
import com.bettercontent.betterrpgprogression.client.ui.StatsScreenLayoutPolicy.EFFECT_GAP
import com.bettercontent.betterrpgprogression.client.ui.StatsScreenLayoutPolicy.LINE_HEIGHT
import com.bettercontent.betterrpgprogression.client.ui.StatsScreenLayoutPolicy.PURPOSE_TOP
import com.bettercontent.betterrpgprogression.client.ui.StatsScreenLayoutPolicy.VALUE_WIDTH
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.network.packets.C2SApplyStats
import com.bettercontent.betterrpgprogression.common.salience.AspectIdentity
import com.bettercontent.betterrpgprogression.common.sound.ModSounds
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.language.I18n
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraftforge.registries.ForgeRegistries

/** The first screen shows the complete effect breakdown for every Life choice. */
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

    private data class Card(val def: ClientStatDef, val purpose: List<FormattedCharSequence>, val effects: List<EffectRow>) {
        val requiredHeight get() = StatsScreenLayoutPolicy.requiredCardHeight(purpose.size,
            effects.map { it.lineCount }.ifEmpty { listOf(1) })
    }
    private data class EffectRow(val effect: ClientEffectDef, val label: List<FormattedCharSequence>, val group: Component?,
                                 val range: Boolean, val equipped: Boolean) {
        val lineCount get() = label.size + (if (group != null) 1 else 0) + 1 +
            (if (range) 2 else if (equipped) 1 else 0)
    }
    private data class Controls(val def: ClientStatDef, val minus: Button, val plus: Button)
    private val snapshot = ClientCache.stats
    private val draft = StatsAllocationDraft(snapshot, ClientCache.defs.mapTo(mutableSetOf()) { it.id })
    private lateinit var layout: StatsScreenLayout
    private var cards: List<Card> = emptyList()
    private var controls: List<Controls> = emptyList()
    private var commit: Button? = null
    private var reset: Button? = null
    private val pulses = mutableMapOf<String, Int>()

    override fun init() {
        super.init()
        val defs = ClientCache.defs
        val minCardWidth = StatsScreenLayoutPolicy.cardWidth(StatsScreenLayoutPolicy.MIN_WIDTH,
            StatsScreenLayoutPolicy.columns(defs.size))
        // Measure on the narrowest canvas so the footer and controls never cover any effect rows.
        val measured = defs.map { makeCard(it, minCardWidth) }
        layout = StatsScreenLayoutPolicy.calculate(width, height, measured.map { it.requiredHeight })
        cards = if (layout.cardWidth == minCardWidth) measured else defs.map { makeCard(it, layout.cardWidth) }
        controls = cards.mapIndexed { index, card ->
            val def = card.def
            val x = layout.cardX(index)
            val y = layout.controlsY(index)
            val name = Component.translatable(def.nameKey)
            val narrate = { cardNarration(card) }
            val minus = addRenderableWidget(button(text("decrease", name), x + CARD_PADDING, y, 18, 18,
                Component.literal("−"), narration = narrate) {
                if (draft.remove(def.id)) refreshButtons()
            })
            val plus = addRenderableWidget(button(text("increase", name), x + layout.cardWidth - CARD_PADDING - 18, y,
                18, 18, Component.literal("+"), narration = narrate) { addPoint(def) })
            Controls(def, minus, plus)
        }
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

    private fun refreshButtons() {
        controls.forEach { row ->
            row.plus.active = draft.canAdd(row.def.id, row.def.maxPoints)
            row.minus.active = draft.pending(row.def.id) > 0
        }
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
        val mx = layout.logical(mouseX.toDouble()).toInt()
        val my = layout.logical(mouseY.toDouble()).toInt()
        graphics.fill(0, 0, width, height, BACKGROUND)
        graphics.pose().pushPose()
        graphics.pose().scale(layout.scale, layout.scale, 1f)
        drawHeader(graphics, layout)
        val movementBase = movementBase()
        cards.forEachIndexed { index, card -> drawCard(graphics, index, card, mx, my, movementBase) }
        graphics.fill(16, layout.footerTop, layout.width - 16, layout.footerTop + 1, BORDER)
        graphics.drawString(font, text("effects_hint"), 16, layout.footerTop + 7, MUTED, false)
        graphics.drawString(font, text("legend"), 16, layout.footerTop + 20, MUTED, false)
        val permanence = text("life_rule")
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
        val available = text("budget", draft.unspent, draft.spent).string
        graphics.drawString(font, available, layout.width - 16 - font.width(available), 20, GAIN, false)
        val peak = text("life_peak", snapshot.lifePeak).string
        graphics.drawString(font, peak, layout.width - 16 - font.width(peak), 36, MUTED, false)
    }

    private fun makeCard(def: ClientStatDef, cardWidth: Int): Card {
        val labelWidth = StatsScreenLayoutPolicy.effectLabelWidth(cardWidth)
        var previousGroup: String? = null
        val effects = def.effects.map { effect ->
            val group = StatsEffectPresentation.group(effect.attributeId)
            val heading = if (group != previousGroup) text("group_$group") else null
            previousGroup = group
            EffectRow(effect, font.split(effectName(effect.attributeId), labelWidth), heading,
                range = effect.available && effect.attributeId == "goety:spell_range",
                equipped = effect.available && def.id.substringAfter(':') in setOf("endurance", "renewal"))
        }
        return Card(def, font.split(purpose(def), cardWidth - 2 * CARD_PADDING), effects)
    }

    private fun drawCard(graphics: GuiGraphics, index: Int, card: Card, mouseX: Int, mouseY: Int, movementBase: Double) {
        val def = card.def
        val x = layout.cardX(index)
        val y = layout.cardY(index)
        val w = layout.cardWidth
        val bottom = y + layout.cardHeight(index)
        val left = x + CARD_PADDING
        val right = x + w - CARD_PADDING
        val hovered = mouseX in x until (x + w) && mouseY in y until bottom
        graphics.fill(x, y, x + w, bottom, BORDER)
        graphics.fill(x + 1, y + 1, x + w - 1, bottom - 1, if (hovered) CARD_HOVER else CARD)
        graphics.fill(x + 1, y + 1, x + w - 1, y + 3, def.color or 0xFF000000.toInt())
        val accent = readableAccent(def.color)
        if ((pulses[def.id] ?: 0) > 0) graphics.fill(x + 1, y + 3, x + 3, bottom - 1, accent)
        AspectIdentity.fromStatId(def.id)?.let { aspect ->
            graphics.blit(AspectIdentity.BADGES, x + 9, y + 9, (aspect.index * 18).toFloat(), 0f,
                18, 18, 18 * AspectIdentity.BADGE_ATLAS_COLUMNS, 18)
        }
        drawFitted(graphics, Component.translatable(def.nameKey), x + 33, y + 13, w - 43, accent)
        card.purpose.forEachIndexed { line, formatted ->
            graphics.drawString(font, formatted, left, y + PURPOSE_TOP + line * LINE_HEIGHT, INK, false)
        }

        val points = draft.points(def.id)
        val committed = committedPoints(def)
        val pending = draft.pending(def.id)
        var rowY = y + StatsScreenLayoutPolicy.effectsTop(card.purpose.size)
        if (card.effects.isEmpty()) {
            graphics.drawString(font, text("no_active_effects"), left, rowY, MUTED, false)
        } else {
            graphics.drawString(font, text("effect_column"), left, rowY - LINE_HEIGHT, MUTED, false)
            listOf("current_column", "draft_column", "next_column").forEachIndexed { column, key ->
                drawFitted(graphics, text(key), right - (2 - column) * VALUE_WIDTH, rowY - LINE_HEIGHT,
                    VALUE_WIDTH - 5, MUTED, rightAligned = true)
            }
        }
        card.effects.forEachIndexed { effectIndex, row ->
            row.group?.let {
                drawFitted(graphics, it, left, rowY, right - left, GOLD)
                rowY += LINE_HEIGHT
            }
            val effect = row.effect
            val labelColor = if (effect.available) INK else DISABLED
            if (effect.isPrimary) graphics.fill(left - 4, rowY, left - 2, rowY + font.lineHeight, accent)
            row.label.forEachIndexed { line, label ->
                graphics.drawString(font, label, left, rowY + line * LINE_HEIGHT, labelColor, false)
            }
            if (effect.available) {
                effectValues(def, effect, committed, points, movementBase).forEachIndexed { column, value ->
                    val color = when (column) { 0 -> MUTED; 1 -> if (pending > 0) GAIN else INK; else -> GAIN }
                    drawFitted(graphics, Component.literal(value), right - (2 - column) * VALUE_WIDTH, rowY,
                        VALUE_WIDTH - 5, color, rightAligned = true)
                }
            } else {
                drawFitted(graphics, text("unavailable_short"), right, rowY, 3 * VALUE_WIDTH - 5, DISABLED, rightAligned = true)
            }
            rowY += row.label.size * LINE_HEIGHT
            effectNotes(def, row, committed, points, movementBase).forEachIndexed { line, note ->
                val color = if (!effect.available) DISABLED else if (line == 0) GOLD else MUTED
                drawFitted(graphics, note, left, rowY, right - left, color)
                rowY += LINE_HEIGHT
            }
            if (effectIndex < card.effects.lastIndex) graphics.fill(left, rowY + 1, right, rowY + 2, BORDER)
            rowY += EFFECT_GAP
        }
        val controlsY = layout.controlsY(index)
        graphics.fill(left, controlsY - 6, right, controlsY - 5, BORDER)
        val count = text("card_points", committed, points, pending)
        graphics.drawString(font, count, x + (w - font.width(count)) / 2, controlsY + 5,
            if (pending > 0) GAIN else MUTED, false)
    }

    // Keep variable-width bonus numbers and localized notes intact instead of cutting them off.
    private fun drawFitted(graphics: GuiGraphics, label: Component, anchorX: Int, y: Int, maxWidth: Int,
                           color: Int, rightAligned: Boolean = false) {
        val width = font.width(label)
        val left = anchorX - if (rightAligned) minOf(width, maxWidth) else 0
        if (width <= maxWidth) {
            graphics.drawString(font, label, left, y, color, false)
        } else {
            graphics.pose().pushPose()
            graphics.pose().translate(left.toDouble(), y.toDouble(), 0.0)
            graphics.pose().scale(maxWidth.toFloat() / width, 1f, 1f)
            graphics.drawString(font, label, 0, 0, color, false)
            graphics.pose().popPose()
        }
    }

    private fun effectValues(def: ClientStatDef, effect: ClientEffectDef, committed: Int, points: Int,
                             movementBase: Double): List<String> {
        val current = effectReading(def, effect, committed, movementBase)
        val proposed = effectReading(def, effect, points, movementBase)
        return listOf(StatsEffectPresentation.format(current.bonus, current.percent),
            StatsEffectPresentation.format(proposed.bonus, proposed.percent),
            if (def.maxPoints >= 0 && points >= def.maxPoints) "—"
            else StatsEffectPresentation.format(proposed.next, proposed.percent))
    }

    private fun effectNotes(def: ClientStatDef, row: EffectRow, committed: Int, points: Int,
                            movementBase: Double): List<Component> = buildList {
        val effect = row.effect
        val milestone = if (row.range) spellRangeAt(def, effect, 20)
            else StatsEffectPresentation.read(effect, 20, movementBase)
        add(text(if (effect.available) "landmark_20" else "landmark_20_unavailable",
            StatsEffectPresentation.format(milestone.bonus, milestone.percent)))
        if (row.range) {
            val raw = StatsEffectPresentation.read(effect, points)
            add(text("range_progress", StatsEffectPresentation.format(raw.bonus, false),
                StatsEffectPresentation.format(raw.next, false)))
            val threshold = StatsEffectPresentation.nextSpellRangePoint(effect, points, committed,
                liveAttribute(effect.attributeId) ?: 0.0, if (def.maxPoints >= 0) def.maxPoints else 20)
            add(threshold?.let { text("range_threshold", it) } ?: text("range_no_threshold"))
        } else if (row.equipped) {
            val equipped = StatsEffectPresentation.formatEquipped(liveAttribute(effect.attributeId))
            add(if (equipped == null) text("equipped_unknown") else text("equipped_total", equipped))
        }
    }

    /** The canvas is not a widget; read the same breakdown when focusing its allocation controls. */
    private fun cardNarration(card: Card): Component {
        val def = card.def
        val committed = committedPoints(def)
        val points = draft.points(def.id)
        val movementBase = movementBase()
        val summary = Component.translatable(def.nameKey).append(". ").append(purpose(def)).append(". ")
            .append(text("detail_points", committed, points, draft.pending(def.id)))
        if (card.effects.isEmpty()) summary.append(". ").append(text("no_active_effects"))
        card.effects.forEach { row ->
            val effect = row.effect
            summary.append(". ").append(effectName(effect.attributeId)).append(": ")
            val values = if (effect.available) text("narrated_effect", *effectValues(def, effect, committed, points, movementBase).toTypedArray())
                else text("unavailable_short")
            summary.append(values)
            effectNotes(def, row, committed, points, movementBase).forEach { summary.append(". ").append(it) }
        }
        return summary
    }

    override fun getNarrationMessage(): Component {
        val message = super.getNarrationMessage().copy().append(". ").append(text("effects_hint"))
            .append(". ").append(text("life_rule"))
        // When neither control on a card can receive focus, retain its breakdown in screen narration.
        cards.zip(controls).forEach { (card, buttons) ->
            if (!buttons.minus.active && !buttons.plus.active) message.append(". ").append(cardNarration(card))
        }
        return message
    }

    private fun committedPoints(def: ClientStatDef) = snapshot.allocations[def.id] ?: 0

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
                       symbol: Component? = null, primary: Boolean = false, narration: (() -> Component)? = null,
                       action: (Button) -> Unit): Button =
        AllocationButton(x, y, width, height, message, Button.OnPress { action(it) }, symbol, primary, narration)

    private class AllocationButton(x: Int, y: Int, width: Int, height: Int, message: Component,
                                   onPress: OnPress, private val symbol: Component?, private val primary: Boolean,
                                   private val narration: (() -> Component)?) :
        Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {
        override fun updateWidgetNarration(output: NarrationElementOutput) {
            super.updateWidgetNarration(output)
            narration?.let { output.nest().add(NarratedElementType.HINT, it()) }
        }

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
    override fun onClose() { minecraft?.setScreen(returnTo) }
    override fun isPauseScreen() = false

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) =
        super.mouseClicked(layout.logical(mouseX), layout.logical(mouseY), button)

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) =
        super.mouseReleased(layout.logical(mouseX), layout.logical(mouseY), button)

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double) =
        super.mouseDragged(layout.logical(mouseX), layout.logical(mouseY), button, layout.logical(dragX), layout.logical(dragY))

    override fun mouseMoved(mouseX: Double, mouseY: Double) =
        super.mouseMoved(layout.logical(mouseX), layout.logical(mouseY))

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollDelta: Double) =
        super.mouseScrolled(layout.logical(mouseX), layout.logical(mouseY), scrollDelta)
}
