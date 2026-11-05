package com.bettercontent.betterrpgprogression.common.attribute

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier
import com.momosoftworks.coldsweat.api.util.Temperature
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.function.Function

/** Optional Cold Sweat integration, registered only when its pinned API is present. */
object ColdSweatLifeCompat {
    @SubscribeEvent
    fun registerModifiers(event: TempModifierRegisterEvent) {
        event.register(ResourceLocation(RpgStatsMod.MODID, "survivalist_rate")) { SurvivalistRateModifier() }
        event.register(ResourceLocation(RpgStatsMod.MODID, "survivalist_freezing")) { SurvivalistFreezingModifier() }
        event.register(ResourceLocation(RpgStatsMod.MODID, "survivalist_burning")) { SurvivalistBurningModifier() }
    }

    @SubscribeEvent
    fun gatherModifiers(event: GatherDefaultTempModifiersEvent) {
        if (event.entity !is ServerPlayer) return
        when (event.trait) {
            Temperature.Trait.RATE -> event.addModifier(SurvivalistRateModifier())
            Temperature.Trait.FREEZING_POINT -> event.addModifier(SurvivalistFreezingModifier())
            Temperature.Trait.BURNING_POINT -> event.addModifier(SurvivalistBurningModifier())
            else -> Unit
        }
    }
}

/** Apply to Cold Sweat's signed environment RATE, not potions, BASE equipment, or existing CORE. */
class SurvivalistRateModifier : TempModifier() {
    override fun calculate(entity: LivingEntity, trait: Temperature.Trait): Function<Double, Double> = Function { rate ->
        if (entity !is ServerPlayer || !rate.isFinite() || rate == 0.0) return@Function rate
        val body = Temperature.get(entity, Temperature.Trait.CORE) + Temperature.get(entity, Temperature.Trait.BASE)
        if (!body.isFinite() || (body > 0 && rate < 0) || (body < 0 && rate > 0)) return@Function rate
        val reduction = entity.getAttributeValue(
            if (rate > 0) ModAttributes.HEAT_DRIFT_REDUCTION.get() else ModAttributes.COLD_DRIFT_REDUCTION.get()
        ).coerceIn(0.0, 0.95)
        rate * (1.0 - reduction)
    }
}

/** Boundary modifiers compose with Cold Sweat's configured/equipment-resolved base, avoiding its NaN sentinel. */
class SurvivalistFreezingModifier : TempModifier() {
    override fun calculate(entity: LivingEntity, trait: Temperature.Trait): Function<Double, Double> = Function { boundary ->
        if (entity !is ServerPlayer || !boundary.isFinite()) boundary
        else boundary + entity.getAttributeValue(ModAttributes.FREEZING_BOUNDARY_OFFSET.get()).coerceIn(-0.5, 0.0)
    }
}

class SurvivalistBurningModifier : TempModifier() {
    override fun calculate(entity: LivingEntity, trait: Temperature.Trait): Function<Double, Double> = Function { boundary ->
        if (entity !is ServerPlayer || !boundary.isFinite()) boundary
        else boundary + entity.getAttributeValue(ModAttributes.BURNING_BOUNDARY_OFFSET.get()).coerceIn(0.0, 0.5)
    }
}
