package com.bettercontent.betterrpgprogression.client.ui

import com.bettercontent.betterrpgprogression.client.cache.ClientCurveDef
import com.bettercontent.betterrpgprogression.client.cache.ClientEffectDef
import java.util.Locale
import kotlin.test.*

class StatsEffectPresentationTest {
    private fun effect(id: String, cap: Double = .25, percent: Boolean = true) =
        ClientEffectDef(id, 0, ClientCurveDef("hyperbola", cap, 20.0, 0.0, 0.0, cap), displayAsPercent = percent)

    @Test
    fun `zero invested points still have a useful next point reading`() {
        val read = StatsEffectPresentation.read(effect("better_rpg_progression:outgoing_damage", .6), 0)
        assertEquals("—", StatsEffectPresentation.format(read.bonus, read.percent))
        assertEquals("+2.86%", StatsEffectPresentation.format(read.next, read.percent))
    }

    @Test
    fun `reductions read as less spread recoil and harmful duration`() {
        for (id in listOf("arrow_spread_reduction", "recoil_reduction", "dispersion_reduction", "harmful_effect_duration_reduction")) {
            val read = StatsEffectPresentation.read(effect("better_rpg_progression:$id"), 20)
            assertEquals(-.125, read.bonus, 1e-9)
            assertTrue(read.next < 0)
            assertEquals("-12.5%", StatsEffectPresentation.format(read.bonus, read.percent))
        }
        val helpful = StatsEffectPresentation.read(effect("better_rpg_progression:beneficial_effect_duration"), 20)
        assertEquals("+12.5%", StatsEffectPresentation.format(helpful.bonus, helpful.percent))
    }

    @Test
    fun `land speed uses the player's base speed not an unexplained tiny attribute scalar`() {
        val speed = effect("minecraft:generic.movement_speed", .05, false)
        val vanilla = StatsEffectPresentation.read(speed, 20, .1)
        assertTrue(vanilla.percent)
        assertEquals("+25%", StatsEffectPresentation.format(vanilla.bonus, vanilla.percent))
        assertEquals(.125, StatsEffectPresentation.read(speed, 20, .2).bonus, 1e-9)
        val zeroBase = StatsEffectPresentation.read(speed, 20, 0.0)
        assertFalse(zeroBase.percent)
        assertTrue(zeroBase.bonus.isFinite())
    }

    @Test
    fun `knockback resistance is a fraction and reads as a percentage`() {
        val read = StatsEffectPresentation.read(effect("minecraft:generic.knockback_resistance", .15, false), 20)
        assertTrue(read.percent)
        assertEquals("+7.5%", StatsEffectPresentation.format(read.bonus, read.percent))
    }

    @Test
    fun `display is locale independent and does not hide units in a tooltip`() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("+1.25%", StatsEffectPresentation.format(.0125, true))
            assertEquals("+0.05", StatsEffectPresentation.format(.05, false))
        } finally { Locale.setDefault(old) }
    }
}
