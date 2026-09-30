package com.bettercontent.betterrpgprogression.common.salience

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraft.resources.ResourceLocation

enum class AspectIdentity(val index: Int, val glyph: String, val title: String, val color: Int) {
    IMPACT(0, "✦", "Impact", 0xFF4055),
    TEMPO(1, "»", "Tempo", 0x00A985),
    WORK(2, "⚒", "Work", 0xF0E2C5),
    MOBILITY(3, "➜", "Mobility", 0xE0B01F),
    ENDURANCE(4, "∞", "Endurance", 0x52606A),
    ROBUSTNESS(5, "◆", "Robustness", 0xAF6A2F),
    RENEWAL(6, "✚", "Renewal", 0x6CCAF0),
    CONTROL(7, "⊕", "Control", 0x8E5BB7);

    val label: String get() = "$glyph $title"
    val badge: String get() = (0xE100 + index).toChar().toString()

    companion object {
        val BADGES = ResourceLocation(RpgStatsMod.MODID, "textures/gui/aspect_badges.png")
        val FONT = ResourceLocation(RpgStatsMod.MODID, "aspects")
        fun fromStatId(id: String): AspectIdentity? = entries.firstOrNull {
            it.name.equals(id.substringAfter(':'), ignoreCase = true)
        }
    }
}
