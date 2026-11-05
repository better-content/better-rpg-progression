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
    fun `small nonzero next point changes remain visible`() {
        assertEquals("+0.000025%", StatsEffectPresentation.format(.00000025, true))
        assertEquals("+0.00000025", StatsEffectPresentation.format(.00000025, false))
        assertEquals("—", StatsEffectPresentation.format(0.0, true))
    }

    @Test
    fun `Goety spell range reflects integer cast distance and preserves raw progress`() {
        val range = ClientEffectDef("goety:spell_range", 0,
            ClientCurveDef("linear", 0.0, 0.0, .05, 0.0, 1.0), displayAsPercent = false)
        val early = StatsEffectPresentation.effectiveSpellRange(range, 1, 0, 0.0)
        assertEquals(0.0, early.bonus)
        assertEquals(0.0, early.next)
        val threshold = StatsEffectPresentation.effectiveSpellRange(range, 19, 0, 0.0)
        assertEquals(1.0, threshold.next)
        val committed = StatsEffectPresentation.effectiveSpellRange(range, 20, 20, 1.0)
        assertEquals(1.0, committed.bonus)
        assertEquals(0.0, committed.next)
        // A fractional equipment bonus can make a raw +.05 point cross the integer boundary earlier.
        assertEquals(1.0, StatsEffectPresentation.effectiveSpellRange(range, 1, 0, .95).bonus)
        assertEquals(20, StatsEffectPresentation.nextSpellRangePoint(range, 0, 0, 0.0))
        assertEquals(1, StatsEffectPresentation.nextSpellRangePoint(range, 0, 0, .95))
        assertNull(StatsEffectPresentation.nextSpellRangePoint(range, 20, 20, 1.0))
        assertEquals("+1", StatsEffectPresentation.format(
            StatsEffectPresentation.effectiveSpellRange(range, 20, 0, 0.0).bonus, false))
    }

    @Test
    fun `equipped totals never imply a draft projection or invent an absent attribute`() {
        assertEquals("17.375", StatsEffectPresentation.formatEquipped(17.375))
        assertEquals("0", StatsEffectPresentation.formatEquipped(-0.0))
        assertNull(StatsEffectPresentation.formatEquipped(null))
        assertNull(StatsEffectPresentation.formatEquipped(Double.NaN))
    }

    @Test
    fun `paired optional integrations have stable presentation groups`() {
        assertEquals("goety", StatsEffectPresentation.group("goety:spell_range"))
        assertEquals("goety", StatsEffectPresentation.group("goety:casting_speed"))
        assertEquals("parcool", StatsEffectPresentation.group("parcool:stamina_recovery"))
        assertEquals("cold_sweat", StatsEffectPresentation.group("better_rpg_progression:heat_drift_reduction"))
        assertEquals("epicfight", StatsEffectPresentation.group("better_rpg_progression:air_slash_damage_bonus"))
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
