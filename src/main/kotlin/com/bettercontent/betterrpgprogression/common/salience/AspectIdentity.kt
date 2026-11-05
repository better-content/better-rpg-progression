package com.bettercontent.betterrpgprogression.common.salience

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraft.resources.ResourceLocation

enum class AspectIdentity(val index: Int, val statId: String, val glyph: String, val title: String, val color: Int) {
    // The stable motif/sound slots are retained; identities now describe the eight Life builds.
    IMPACT(0, "vanguard", "✦", "Vanguard", 0xFF4055),
    TEMPO(1, "sharpshooter", "»", "Sharpshooter", 0x8E5BB7),
    WORK(2, "artisan", "⚒", "Artisan", 0xF0E2C5),
    MOBILITY(3, "trailblazer", "➜", "Trailblazer", 0xE0B01F),
    ENDURANCE(4, "survivalist", "∞", "Survivalist", 0xAF6A2F),
    ROBUSTNESS(5, "spellcaster", "◆", "Spellcaster", 0x6CCAF0),
    RENEWAL(6, "skirmisher", "✚", "Skirmisher", 0x52606A),
    CONTROL(7, "aerialist", "⊕", "Aerialist", 0x00A985);

    val label: String get() = "$glyph $title"
    val badge: String get() = (0xE100 + index).toChar().toString()

    companion object {
        val BADGES = ResourceLocation(RpgStatsMod.MODID, "textures/gui/stat_badges.png")
        val FONT = ResourceLocation(RpgStatsMod.MODID, "stats")
        fun fromStatId(id: String): AspectIdentity? = entries.firstOrNull {
            it.statId.equals(id.substringAfter(':'), ignoreCase = true)
        }
    }
}
