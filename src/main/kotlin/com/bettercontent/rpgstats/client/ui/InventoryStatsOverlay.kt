package com.bettercontent.rpgstats.client.ui

import com.bettercontent.rpgstats.RpgStatsMod
import com.bettercontent.rpgstats.client.cache.ClientCache
import com.bettercontent.rpgstats.client.cache.ClientStatDef
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ContainerScreenEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object InventoryStatsOverlay {
    private const val INVENTORY_IMAGE_WIDTH = 176
    private const val EFFECT_PANEL_WIDTH = 120
    private const val COMPACT_EFFECT_PANEL_WIDTH = 32

    @SubscribeEvent
    fun onInventoryForeground(event: ContainerScreenEvent.Render.Foreground) {
        val screen = event.containerScreen
        if (screen !is InventoryScreen) return
        if (screen.javaClass.name == "com.bettercontent.journalui.JournalInventoryScreen") return

        val defs = ClientCache.defs
        if (defs.isEmpty()) return

        val stats = ClientCache.stats

        val mc = Minecraft.getInstance()
        val font = mc.font
        val rows = defs.map { def -> StatRow(def, stats.allocations[def.id] ?: 0) }
        val rowHeight = 10
        val paddingX = 4
        val paddingY = 3
        val plusText = "+"
        val plusSpace = if (stats.unspent > 0) font.width(plusText) + 4 else 0
        val contentWidth = rows.maxOfOrNull { font.width(it.label) } ?: 0
        val boxWidth = contentWidth + paddingX * 2 + plusSpace
        val boxHeight = rows.size * rowHeight + paddingY * 2

        val x = getOverlayX(screen, mc, boxWidth)
        val y = 6

        val guiGraphics = event.guiGraphics
        guiGraphics.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, 0x70000000)
        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, 0x90202020.toInt())

        rows.forEachIndexed { index, row ->
            val rowY = y + paddingY + index * rowHeight
            guiGraphics.drawString(font, row.abbreviation, x + paddingX, rowY, row.color, false)
            guiGraphics.drawString(font, row.pointsText, x + boxWidth - paddingX - plusSpace - font.width(row.pointsText), rowY, 0xE0E0E0, false)
        }

        if (stats.unspent > 0) {
            guiGraphics.drawString(font, plusText, x + boxWidth - paddingX - font.width(plusText), y + paddingY, 0xFFFF55, false)
        }
    }

    private fun getOverlayX(screen: InventoryScreen, mc: Minecraft, boxWidth: Int): Int {
        val estimatedLeftPos = (mc.window.guiScaledWidth - INVENTORY_IMAGE_WIDTH) / 2
        val rightSpace = mc.window.guiScaledWidth - (estimatedLeftPos + INVENTORY_IMAGE_WIDTH)
        val hasEffects = mc.player?.activeEffects?.isNotEmpty() == true && screen.canSeeEffects()
        val effectOffset = if (hasEffects) {
            if (rightSpace >= INVENTORY_IMAGE_WIDTH + EFFECT_PANEL_WIDTH) EFFECT_PANEL_WIDTH + 6 else COMPACT_EFFECT_PANEL_WIDTH + 6
        } else {
            0
        }
        val desiredX = INVENTORY_IMAGE_WIDTH + 6 + effectOffset
        val maxX = mc.window.guiScaledWidth - estimatedLeftPos - boxWidth - 4
        return minOf(desiredX, maxX).coerceAtLeast(4)
    }

    private data class StatRow(
        val abbreviation: String,
        val points: Int,
        val color: Int
    ) {
        val pointsText: String = points.toString()
        val label: String = "$abbreviation $pointsText"

        constructor(def: ClientStatDef, points: Int) : this(
            abbreviation = abbreviate(Component.translatable(def.nameKey).string),
            points = points,
            color = def.color or 0xFF000000.toInt()
        )
    }

    private fun abbreviate(name: String): String {
        val words = name.split(Regex("\\s+")).filter { it.isNotBlank() }
        val initials = words.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        return if (initials.length >= 2) {
            initials.take(3)
        } else {
            name.filter { it.isLetterOrDigit() }.take(3).uppercase()
        }.ifEmpty { "STAT" }
    }
}
