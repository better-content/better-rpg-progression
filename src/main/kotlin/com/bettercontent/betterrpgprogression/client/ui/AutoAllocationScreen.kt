package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.network.packets.C2SAutoAllocationPlan
import com.bettercontent.betterrpgprogression.common.points.AutoAllocationPlan
import com.bettercontent.gameplaynotices.BetterUiTheme
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

/** One planned attribute for each earned Minecraft experience level. */
class AutoAllocationScreen(private val returnTo: Screen) : Screen(Component.translatable("screen.better_rpg_progression.auto_plan")) {
    private val plan = ClientCache.stats.autoAllocationPlan.toMutableList()
    private var enabled = ClientCache.stats.autoAllocationEnabled
    private var centeredLevel = (ClientCache.stats.lifePeak + 1).coerceIn(1, AutoAllocationPlan.MAX_ENTRIES)
    private var dragX: Double? = null
    private var dragRemainder = 0.0
    private val pageWidth get() = minOf(600, width - 16)
    private val pageHeight get() = minOf(330, height - 16)
    private val left get() = (width - pageWidth) / 2
    private val top get() = (height - pageHeight) / 2
    private val cardWidth = 48

    override fun init() {
        super.init()
        val x = left + 12
        val y = top + 27
        addRenderableWidget(Button.builder(enabledLabel()) { button ->
            enabled = !enabled
            button.message = enabledLabel()
        }.pos(x, y).size(95, 19).build())
        addRenderableWidget(Button.builder(Component.translatable("screen.better_rpg_progression.clear_plan")) {
            plan.clear()
        }.pos(x + 101, y).size(80, 19).build())
        addRenderableWidget(Button.builder(Component.translatable("screen.better_rpg_progression.done")) {
            trimTail()
            Network.sendToServer(C2SAutoAllocationPlan(enabled, plan.toList()))
            minecraft?.setScreen(returnTo)
        }.pos(left + pageWidth - 92, y).size(80, 19).build())
        val definitions = ClientCache.defs
        val buttonWidth = minOf(230, pageWidth - 40)
        val xButton = width / 2 - buttonWidth / 2
        definitions.forEachIndexed { index, def ->
            val label = Component.translatable(def.nameKey)
            addRenderableWidget(Button.builder(label) {
                val slot = centeredLevel - 1
                while (plan.size <= slot) plan.add("")
                plan[slot] = def.id
            }.pos(xButton, top + 61 + index * 23).size(buttonWidth, 20).build())
        }
    }

    private fun trimTail() {
        while (plan.lastOrNull().isNullOrEmpty() && plan.isNotEmpty()) plan.removeAt(plan.lastIndex)
    }

    private fun enabledLabel() = Component.translatable(if (enabled) "screen.better_rpg_progression.auto_enabled" else "screen.better_rpg_progression.auto_paused")

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val x = left; val y = top
        val border = BetterUiTheme.color(0xFFAA8E62.toInt(), 0xFF987750.toInt())
        val paper = BetterUiTheme.color(0xFFECDFBD.toInt(), 0xFF182B26.toInt())
        val ink = BetterUiTheme.color(0xFF30483E.toInt(), 0xFFF4E6C7.toInt())
        val muted = BetterUiTheme.color(0xFF776C55.toInt(), 0xFFC4B99E.toInt())
        graphics.fill(x, y, x + pageWidth, y + pageHeight, border)
        graphics.fill(x + 2, y + 2, x + pageWidth - 2, y + pageHeight - 2, paper)
        centered(graphics, "RECORD / LEVEL PLAN", width / 2, y + 8, ink)
        centered(graphics, "Level $centeredLevel: choose an attribute", width / 2, y + 48, ink)
        val timelineY = y + pageHeight - 63
        graphics.fill(x + 10, timelineY - 5, x + pageWidth - 10, timelineY - 4, border)
        val mid = width / 2
        val visible = (pageWidth - 24) / cardWidth
        val half = visible / 2
        for (offset in -half..half) {
            val level = centeredLevel + offset
            if (level !in 1..AutoAllocationPlan.MAX_ENTRIES) continue
            val cardX = mid + offset * cardWidth - (cardWidth - 3) / 2
            if (cardX < x + 10 || cardX + cardWidth - 3 > x + pageWidth - 10) continue
            val assigned = plan.getOrNull(level - 1).orEmpty()
            graphics.fill(cardX, timelineY, cardX + cardWidth - 3, timelineY + 42,
                if (level == centeredLevel) BetterUiTheme.color(0xFFBCCDAF.toInt(), 0xFF547B61.toInt())
                else BetterUiTheme.color(0xFFD7C8A7.toInt(), 0xFF2D4237.toInt()))
            centered(graphics, "Lv $level", cardX + 22, timelineY + 4, ink)
            val name = ClientCache.defs.firstOrNull { it.id == assigned }?.let { Component.translatable(it.nameKey).string }
            centered(graphics, font.plainSubstrByWidth(name ?: "—", 41), cardX + 22, timelineY + 22, ink)
        }
        centered(graphics, "DRAG OR SCROLL LEVELS", width / 2, y + pageHeight - 17, muted)
        super.render(graphics, mouseX, mouseY, partialTick)
        children().filterIsInstance<Button>().filter { it.visible }.forEach { button ->
            val bx = button.x; val by = button.y; val bw = button.width; val bh = button.height
            val hover = mouseX in bx until (bx + bw) && mouseY in by until (by + bh)
            graphics.fill(bx, by, bx + bw, by + bh, if (hover) 0xFF59755C.toInt() else 0xFF405D49.toInt())
            graphics.fill(bx, by, bx + bw, by + 2, border)
            centered(graphics, button.message.string, bx + bw / 2, by + (bh - font.lineHeight) / 2, 0xFFF9EFD7.toInt())
        }
        val attributeButtons = children().filterIsInstance<Button>().drop(3)
        ClientCache.defs.zip(attributeButtons).forEach { (def, button) ->
            val label = Component.translatable(def.nameKey).string
            val code = label.filter { it.isLetterOrDigit() }.take(3).uppercase()
            graphics.drawString(font, code, button.x + 7, button.y + 6, def.color or 0xFF000000.toInt(), false)
        }
    }

    private fun centered(graphics: GuiGraphics, label: String, x: Int, y: Int, color: Int) {
        graphics.drawString(font, label, x - font.width(label) / 2, y, color, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseY in (top + pageHeight - 63).toDouble()..(top + pageHeight - 20).toDouble()) {
            dragX = mouseX
            dragRemainder = 0.0
            val offset = ((mouseX - width / 2) / cardWidth).roundToInt()
            centeredLevel = (centeredLevel + offset).coerceIn(1, AutoAllocationPlan.MAX_ENTRIES)
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        val oldX = this.dragX ?: return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        dragRemainder += oldX - mouseX
        val steps = (dragRemainder / cardWidth).toInt()
        if (steps != 0) {
            centeredLevel = (centeredLevel + steps).coerceIn(1, AutoAllocationPlan.MAX_ENTRIES)
            dragRemainder -= steps * cardWidth
        }
        this.dragX = mouseX
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        dragX = null
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollDelta: Double): Boolean {
        centeredLevel = (centeredLevel - scrollDelta.toInt()).coerceIn(1, AutoAllocationPlan.MAX_ENTRIES)
        return true
    }

    override fun onClose() { minecraft?.setScreen(returnTo) }
}
