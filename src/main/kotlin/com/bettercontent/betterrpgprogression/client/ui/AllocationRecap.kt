package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.common.salience.AspectIdentity
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderGuiOverlayEvent
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object AllocationRecap {
    private var ticks = 0
    private var deltas: Map<String, Int> = emptyMap()

    fun accept(accepted: Map<String, Int>) {
        deltas = accepted.filterValues { it > 0 }
        ticks = if (deltas.isEmpty()) 0 else 80
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END && ticks > 0) ticks--
    }

    @SubscribeEvent
    fun render(event: RenderGuiOverlayEvent.Post) {
        if (ticks <= 0 || !event.overlay.id().equals(VanillaGuiOverlay.HOTBAR.id())) return
        val minecraft = Minecraft.getInstance()
        if (minecraft.player == null || minecraft.options.hideGui) return
        val line = component()
        val fade = if (ticks < 20) ticks / 20f else 1f
        val width = minecraft.font.width(line) + 12
        val x = (event.window.guiScaledWidth - width) / 2
        val y = event.window.guiScaledHeight - 96 - ((1f - fade) * 8).toInt()
        event.guiGraphics.fill(x, y - 10, x + width, y + 18, ((170 * fade).toInt() shl 24) or 0x101318)
        event.guiGraphics.drawString(minecraft.font, line, x + 6, y + 5,
            ((255 * fade).toInt() shl 24) or 0xFFFFFF, false)
    }

    private fun component(): Component {
        val result: MutableComponent = Component.literal("Stats increased: ").withStyle { it.withColor(0xB8BEC7) }
        val defs = ClientCache.defs.associateBy { it.id }
        deltas.entries.sortedBy { AspectIdentity.fromStatId(it.key)?.index ?: 99 }.forEachIndexed { index, (id, amount) ->
            if (index > 0) result.append(Component.literal(" · ").withStyle { it.withColor(0x777777) })
            val aspect = AspectIdentity.fromStatId(id)
            val natural = defs[id]?.let { Component.translatable(it.nameKey).string } ?: id.substringAfter(':')
            if (aspect != null) {
                result.append(Component.literal(aspect.badge).withStyle { it.withFont(AspectIdentity.FONT) })
                result.append(Component.literal(" "))
            }
            val label = if (aspect == null) "$natural +$amount" else "$natural — ${aspect.title} +$amount"
            result.append(Component.literal(label).withStyle { it.withColor(aspect?.color ?: 0xFFFFFF) })
        }
        return result
    }
}
