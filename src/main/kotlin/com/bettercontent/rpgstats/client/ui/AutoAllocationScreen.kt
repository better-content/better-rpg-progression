package com.bettercontent.rpgstats.client.ui

import com.bettercontent.rpgstats.client.cache.ClientCache
import com.bettercontent.rpgstats.common.network.Network
import com.bettercontent.rpgstats.common.network.packets.C2SAutoAllocationPlan
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Editable order and a faithful preview of the next level-earned points. */
class AutoAllocationScreen(private val returnTo: Screen) : Screen(Component.translatable("screen.rpg_stats.auto_plan")) {
    private val plan = ClientCache.stats.autoAllocationPlan.toMutableList()
    private var enabled = ClientCache.stats.autoAllocationEnabled
    private val pageWidth = 386
    private fun left() = width / 2 - pageWidth / 2
    private fun top() = height / 2 - 146

    override fun init() {
        super.init()
        val x = left() + 12
        val y = top() + 27
        val gap = 6
        val buttonWidth = (pageWidth - 24 - gap * 2) / 3
        addRenderableWidget(Button.builder(enabledLabel()) { button ->
            enabled = !enabled
            button.message = enabledLabel()
        }.pos(x, y).size(buttonWidth, 20).build())
        addRenderableWidget(Button.builder(Component.translatable("screen.rpg_stats.clear_plan")) {
            plan.clear()
            refreshEditor()
        }.pos(x + buttonWidth + gap, y).size(buttonWidth, 20).build())
        addRenderableWidget(Button.builder(Component.translatable("screen.rpg_stats.done")) {
            Network.sendToServer(C2SAutoAllocationPlan(enabled, plan.toList()))
            minecraft?.setScreen(returnTo)
        }.pos(x + (buttonWidth + gap) * 2, y).size(buttonWidth, 20).build())
        rebuildRows()
    }

    private fun refreshEditor() { clearWidgets(); init() }

    private fun rebuildRows() {
        val x = left() + 12
        val y0 = top() + 60
        ClientCache.defs.take(8).forEachIndexed { index, def ->
            val y = y0 + index * 19
            val present = def.id in plan
            addRenderableWidget(Button.builder(Component.translatable(if (present) "screen.rpg_stats.remove_plan" else "screen.rpg_stats.add_plan")) {
                if (present) plan.remove(def.id) else plan.add(def.id)
                refreshEditor()
            }.pos(x, y).size(58, 18).build())
            addRenderableWidget(Button.builder(Component.translatable(def.nameKey)) { }
                .pos(x + 64, y).size(238, 18).build().also { it.active = false })
            if (present) {
                val ordinal = plan.indexOf(def.id)
                addRenderableWidget(Button.builder(Component.literal("↑")) {
                    if (ordinal > 0) { val moved = plan.removeAt(ordinal); plan.add(ordinal - 1, moved); refreshEditor() }
                }.pos(x + 308, y).size(28, 18).build().also { it.active = ordinal > 0 })
                addRenderableWidget(Button.builder(Component.literal("↓")) {
                    if (ordinal < plan.lastIndex) { val moved = plan.removeAt(ordinal); plan.add(ordinal + 1, moved); refreshEditor() }
                }.pos(x + 342, y).size(28, 18).build().also { it.active = ordinal < plan.lastIndex })
            }
        }
    }

    private fun enabledLabel() = Component.translatable(if (enabled) "screen.rpg_stats.auto_enabled" else "screen.rpg_stats.auto_paused")

    private fun projected(): List<String?> {
        val stats = ClientCache.stats
        val counts = stats.allocations.toMutableMap()
        val limits = ClientCache.defs.associate { it.id to it.maxPoints }
        var cursor = if (plan.isEmpty()) 0 else stats.autoAllocationCursor.mod(plan.size)
        return List(8) {
            if (!enabled || plan.isEmpty()) return@List null
            var chosen: String? = null
            repeat(plan.size) {
                if (chosen == null) {
                    val id = plan[cursor]
                    cursor = (cursor + 1).mod(plan.size)
                    val cap = limits[id]
                    if (cap != null && (cap < 0 || counts.getOrDefault(id, 0) < cap)) {
                        chosen = id
                        counts[id] = counts.getOrDefault(id, 0) + 1
                    }
                }
            }
            chosen
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, 0xFF1B2822.toInt())
        val x = left(); val y = top()
        graphics.fill(x, y, x + pageWidth, y + 292, 0xFFAA8E62.toInt())
        graphics.fill(x + 2, y + 2, x + pageWidth - 2, y + 290, 0xFFECDFBD.toInt())
        graphics.drawCenteredString(font, "RECORD / AUTO ALLOCATION", width / 2, y + 9, 0xFF30483E.toInt())
        graphics.fill(x + 12, y + 53, x + pageWidth - 12, y + 54, 0xFFBDA479.toInt())
        val timelineY = y + 223
        graphics.fill(x + 12, timelineY - 4, x + pageWidth - 12, timelineY - 3, 0xFFBDA479.toInt())
        graphics.drawString(font, "NEXT EIGHT LEVEL POINTS", x + 12, timelineY, 0xFF30483E.toInt(), false)
        val steps = projected()
        val names = ClientCache.defs.associate { it.id to Component.translatable(it.nameKey).string }
        val stepWidth = 42
        val gap = 3
        steps.forEachIndexed { index, id ->
            val sx = x + 12 + index * (stepWidth + gap)
            graphics.fill(sx, timelineY + 14, sx + stepWidth, timelineY + 59, if (id == null) 0xFFD7C8A7.toInt() else 0xFFC8D2B6.toInt())
            graphics.drawCenteredString(font, "+${index + 1}", sx + stepWidth / 2, timelineY + 17, 0xFF30483E.toInt())
            val label = if (id == null) "—" else font.plainSubstrByWidth(names[id] ?: id, stepWidth - 4)
            graphics.drawCenteredString(font, label, sx + stepWidth / 2, timelineY + 34, 0xFF30483E.toInt())
        }
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun onClose() { minecraft?.setScreen(returnTo) }
}
