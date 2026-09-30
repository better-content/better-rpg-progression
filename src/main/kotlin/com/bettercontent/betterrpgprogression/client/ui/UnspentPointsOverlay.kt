package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.client.cache.ClientCache
import com.bettercontent.betterrpgprogression.client.input.KeybindRegister
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderGuiOverlayEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object UnspentPointsOverlay {

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGuiOverlayEvent.Post) {
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.isPaused) return

        val unspent = ClientCache.stats.unspent
        if (unspent <= 0) return

        val guiGraphics = event.guiGraphics
        val font = mc.font

        val text1 = Component.translatable("overlay.better_rpg_progression.unspent_points", unspent.toString())
        val text2 = Component.translatable("overlay.better_rpg_progression.press_key", KeybindRegister.mapping().translatedKeyMessage)
        val boxWidth = maxOf(font.width(text1), font.width(text2))

        val padding = 10
        val x = event.window.guiScaledWidth - boxWidth - padding
        val y = padding

        val bgColor = 0x80000000.toInt()
        val textColor = 0xFFFF55

        guiGraphics.fill(x - 3, y - 3, x + boxWidth + 3, y + font.lineHeight * 2 + 3, bgColor)

        guiGraphics.drawString(font, text1, x, y, textColor)
        guiGraphics.drawString(font, text2, x, y + font.lineHeight, textColor)
    }
}
