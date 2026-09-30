package com.bettercontent.betterrpgprogression.harness

import com.bettercontent.betterrpgprogression.common.salience.AspectIdentity
import com.bettercontent.betterrpgprogression.common.sound.ModSounds
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component

class IdentitySoundReviewScreen : Screen(Component.literal("Systemic Salience — Sound Review")) {
    override fun init() {
        val buttonWidth = 164
        val left = width / 2 - buttonWidth - 6
        val top = height / 2 - 74
        AspectIdentity.entries.forEachIndexed { index, aspect ->
            val x = left + (index % 2) * (buttonWidth + 12)
            val y = top + (index / 2) * 30
            addRenderableWidget(Button.builder(Component.literal("${aspect.label}  ▶")) { play(aspect) }
                .pos(x, y).size(buttonWidth, 20).build())
        }
    }

    fun play(aspect: AspectIdentity) {
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(ModSounds.forAspect(aspect), 1.0f, .45f))
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics)
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 106, 0xFFFFFF)
        graphics.drawCenteredString(font, "Provisional 150–300ms mono motifs · review at gameplay volume",
            width / 2, height / 2 - 91, 0xB8BEC7)
        super.render(graphics, mouseX, mouseY, partialTick)
    }
}
