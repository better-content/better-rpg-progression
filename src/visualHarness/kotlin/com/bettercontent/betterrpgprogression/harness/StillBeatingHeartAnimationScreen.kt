package com.bettercontent.betterrpgprogression.harness

import com.bettercontent.betterrpgprogression.common.item.ModItems
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/** Development-only fixture that renders the real atlas-backed item sprite at two GUI scales. */
class StillBeatingHeartAnimationScreen : Screen(Component.literal("Still-Beating Heart Animation")) {
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics)
        guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 82, 0xFFFFFF)
        guiGraphics.drawCenteredString(
            font,
            Component.literal("The enlarged and inventory-scale hearts must complete two visible pulses."),
            width / 2,
            height / 2 + 62,
            0xB0B0B0
        )

        val heart = ItemStack(ModItems.STILL_BEATING_HEART.get())
        guiGraphics.pose().pushPose()
        guiGraphics.pose().translate((width / 2 - 56).toDouble(), (height / 2 - 48).toDouble(), 0.0)
        guiGraphics.pose().scale(6.0f, 6.0f, 1.0f)
        guiGraphics.renderItem(heart, 0, 0)
        guiGraphics.pose().popPose()

        guiGraphics.renderItem(heart, width / 2 + 48, height / 2 - 8)
        guiGraphics.renderItemDecorations(font, heart, width / 2 + 48, height / 2 - 8)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun isPauseScreen(): Boolean = false
}
