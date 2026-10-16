package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.client.cache.ClientStatDef
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

/** An overview for learning and comparing all attributes, not a tooltip catalogue. */
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

    private data class Row(val def: ClientStatDef, val minus: Button, val plus: Button)
    private val snapshot = ClientCache.stats
    private val draft = StatsAllocationDraft(snapshot)
    private var rows: List<Row> = emptyList()
    private var commit: Button? = null
    private var reset: Button? = null
    private val pulses = mutableMapOf<String, Int>()

    override fun init() {
        super.init()
        val layout = layout()
        rows = ClientCache.defs.mapIndexed { index, def ->
            val x = layout.cardX(index)
            val y = layout.cardY(index) + layout.cardHeight - 26
            val minus = button(Component.translatable("screen.better_rpg_progression.decrease", Component.translatable(def.nameKey)),
                x + 10, y, 18, 18, Component.literal("−")) {
                if (draft.remove(def.id)) refreshButtons()
            }
            val plus = button(Component.translatable("screen.better_rpg_progression.increase", Component.translatable(def.nameKey)),
                x + layout.cardWidth - 28, y, 18, 18, Component.literal("+")) {
                if (draft.add(def.id, def.maxPoints)) {
                    AspectIdentity.fromStatId(def.id)?.let { aspect ->
                        pulses[def.id] = 8
                        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(ModSounds.forAspect(aspect), 1f, .25f))
                    }
                    refreshButtons()
                }
            }
            // The narrative and numbers are always visible; narration explains the symbol controls.
            addRenderableWidget(minus)
            addRenderableWidget(plus)
            Row(def, minus, plus)
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

    private fun refreshButtons() {
        rows.forEach {
            it.plus.active = draft.canAdd(it.def.id, it.def.maxPoints)
            it.minus.active = draft.pending(it.def.id) > 0
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
        val layout = layout()
        val mx = layout.logical(mouseX.toDouble()).toInt()
        val my = layout.logical(mouseY.toDouble()).toInt()
        graphics.fill(0, 0, width, height, BACKGROUND)
        graphics.pose().pushPose()
        graphics.pose().scale(layout.scale, layout.scale, 1f)
        drawHeader(graphics, layout)
        rows.forEachIndexed { index, row -> drawCard(graphics, layout, index, row, mx, my) }
        graphics.fill(16, layout.footerTop, layout.width - 16, layout.footerTop + 1, BORDER)
        graphics.drawString(font, text("legend"), 16, layout.footerTop + 7, MUTED, false)
        val permanence = text("life_rule").string
        graphics.drawString(font, permanence, layout.width - 16 - font.width(permanence), layout.footerTop + 20, MUTED, false)
        // Native widgets retain focus, keyboard activation and descriptive narration.
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
        val purposeKey = "${def.nameKey}.purpose"
        val purpose = if (I18n.exists(purposeKey)) Component.translatable(purposeKey) else text("generic_purpose")
        font.split(purpose, w - 20).forEachIndexed { line, formatted ->
            graphics.drawString(font, formatted, x + 10, y + 34 + line * 10, INK, false)
        }
        val valueX = x + w - 49
        val bonusX = valueX - 42
        graphics.drawString(font, text("effect_column"), x + 10, y + 62, MUTED, false)
        graphics.drawString(font, text("bonus_column"), bonusX, y + 62, MUTED, false)
        graphics.drawString(font, text("next_column"), valueX + 2, y + 62, GAIN, false)
        val effectsTop = y + 77
        val effectHeight = minOf(18, (h - 110) / maxOf(5, def.effects.size)).coerceAtLeast(10)
        val movementBase = minecraft?.player?.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue ?: 0.1
        if (def.effects.isEmpty()) graphics.drawString(font, text("no_active_effects"), x + 10, effectsTop, MUTED, false)
        def.effects.forEachIndexed { effectIndex, effect ->
            val effectY = effectsTop + effectIndex * effectHeight
            val reading = StatsEffectPresentation.read(effect, draft.points(def.id), movementBase)
            val bonus = StatsEffectPresentation.format(reading.bonus, reading.percent)
            val next = if (def.maxPoints >= 0 && draft.points(def.id) >= def.maxPoints) "—"
                else StatsEffectPresentation.format(reading.next, reading.percent)
            val label = effectName(effect.attributeId)
            graphics.drawString(font, label, x + 10, effectY, MUTED, false)
            graphics.drawString(font, bonus, valueX - 6 - font.width(bonus), effectY,
                if (draft.pending(def.id) > 0) INK else MUTED, false)
            graphics.drawString(font, next, x + w - 10 - font.width(next), effectY, GAIN, false)
        }
        val controlsY = y + h - 26
        graphics.fill(x + 10, controlsY - 6, x + w - 10, controlsY - 5, BORDER)
        val pending = draft.pending(def.id)
        val points = draft.points(def.id)
        val counter = if (def.maxPoints >= 0) "$points/${def.maxPoints}" else points.toString()
        val count = if (pending > 0) text(if (points == 1) "point_draft" else "points_draft", counter, pending)
            else text(if (points == 1) "point_count" else "points_count", counter)
        graphics.drawString(font, count, x + (w - font.width(count)) / 2, controlsY + 5,
            if (pending > 0) GAIN else MUTED, false)
    }

    private fun button(message: Component, x: Int, y: Int, width: Int, height: Int,
                       symbol: Component? = null, primary: Boolean = false, action: (Button) -> Unit): Button =
        AllocationButton(x, y, width, height, message, Button.OnPress { action(it) }, symbol, primary)

    /** Draw once, rather than painting over native scrolling text at a different depth. */
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

    /** Hue stays recognizable, but even the dark Endurance pigment has readable text contrast. */
    private fun readableAccent(color: Int): Int {
        fun channel(shift: Int) = ((((color shr shift) and 255) * .55 + 255 * .45).toInt() shl shift)
        return 0xFF000000.toInt() or channel(16) or channel(8) or channel(0)
    }

    private fun text(key: String, vararg args: Any) = Component.translatable("screen.better_rpg_progression.$key", *args)
    private fun layout() = StatsScreenLayoutPolicy.calculate(width, height, ClientCache.defs.size)
    override fun onClose() { minecraft?.setScreen(returnTo) }
    override fun isPauseScreen() = false

    // Widget coordinates live in the same readable canvas as rendering, at every global GUI scale.
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val l = layout()
        return super.mouseClicked(l.logical(mouseX), l.logical(mouseY), button)
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
