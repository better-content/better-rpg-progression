package com.bettercontent.betterrpgprogression.common.salience

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraft.resources.ResourceLocation

enum class AspectIdentity(val index: Int, val statId: String, val glyph: String, val title: String, val color: Int) {
    // Keep the already-authored badge/motif atlas slots; only these five are Life choices.
    POWER(0, "power", "✦", "Power", 0xFF4055),
    MOTION(3, "motion", "➜", "Motion", 0xE0B01F),
    ENDURANCE(4, "endurance", "∞", "Endurance", 0x52606A),
    RENEWAL(6, "renewal", "✚", "Renewal", 0x6CCAF0),
    CONTROL(7, "control", "⊕", "Control", 0x8E5BB7);

    val label: String get() = "$glyph $title"
    val badge: String get() = (0xE100 + index).toChar().toString()

    companion object {
        val BADGES = ResourceLocation(RpgStatsMod.MODID, "textures/gui/stat_badges.png")
        val FONT = ResourceLocation(RpgStatsMod.MODID, "stats")
        const val BADGE_ATLAS_COLUMNS = 8 // existing 144px texture, not entries.size
        fun fromStatId(id: String): AspectIdentity? = entries.firstOrNull {
            id == "${RpgStatsMod.MODID}:${it.statId}"
        }
    }
}
