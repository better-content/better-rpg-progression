package com.bettercontent.betterrpgprogression.common.attribute

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.RangedAttribute
import net.minecraftforge.event.entity.EntityAttributeModificationEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModAttributes {
    private val ATTRIBUTES: DeferredRegister<Attribute> =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, RpgStatsMod.MODID)

    val HUNGER_EFFICIENCY: RegistryObject<Attribute> = ATTRIBUTES.register("hunger_efficiency") {
        RangedAttribute("attribute.name.better_rpg_progression.hunger_efficiency", 1.0, 1.0, 1024.0).setSyncable(true)
    }

    val THIRST_EFFICIENCY: RegistryObject<Attribute> = ATTRIBUTES.register("thirst_efficiency") {
        RangedAttribute("attribute.name.better_rpg_progression.thirst_efficiency", 1.0, 1.0, 1024.0).setSyncable(true)
    }

    val MINING_SPEED: RegistryObject<Attribute> = ATTRIBUTES.register("mining_speed") {
        RangedAttribute("attribute.name.better_rpg_progression.mining_speed", 1.0, 1.0, 1024.0).setSyncable(true)
    }

    val AIR_CAPACITY: RegistryObject<Attribute> = ATTRIBUTES.register("air_capacity") {
        RangedAttribute("attribute.name.better_rpg_progression.air_capacity", 1.0, 1.0, 2.0).setSyncable(true)
    }

    val RECOIL_REDUCTION: RegistryObject<Attribute> = ATTRIBUTES.register("recoil_reduction") {
        RangedAttribute("attribute.name.better_rpg_progression.recoil_reduction", 0.0, -1.0, 1.0).setSyncable(true)
    }

    val DISPERSION_REDUCTION: RegistryObject<Attribute> = ATTRIBUTES.register("dispersion_reduction") {
        RangedAttribute("attribute.name.better_rpg_progression.dispersion_reduction", 0.0, -1.0, 1.0).setSyncable(true)
    }

    val ARROW_SPREAD_REDUCTION: RegistryObject<Attribute> = ATTRIBUTES.register("arrow_spread_reduction") {
        RangedAttribute("attribute.name.better_rpg_progression.arrow_spread_reduction", 0.0, 0.0, 0.35).setSyncable(true)
    }

    val ARROW_SPEED_BONUS: RegistryObject<Attribute> = ATTRIBUTES.register("arrow_speed_bonus") {
        RangedAttribute("attribute.name.better_rpg_progression.arrow_speed_bonus", 0.0, 0.0, 0.08).setSyncable(true)
    }

    val OUTGOING_DAMAGE: RegistryObject<Attribute> = ATTRIBUTES.register("outgoing_damage") {
        RangedAttribute("attribute.name.better_rpg_progression.outgoing_damage", 0.0, 0.0, 2.0).setSyncable(true)
    }

    val HARMFUL_EFFECT_DURATION_REDUCTION: RegistryObject<Attribute> = ATTRIBUTES.register("harmful_effect_duration_reduction") {
        RangedAttribute("attribute.name.better_rpg_progression.harmful_effect_duration_reduction", 0.0, 0.0, 0.25).setSyncable(true)
    }

    val BENEFICIAL_EFFECT_DURATION: RegistryObject<Attribute> = ATTRIBUTES.register("beneficial_effect_duration") {
        RangedAttribute("attribute.name.better_rpg_progression.beneficial_effect_duration", 0.0, 0.0, 0.25).setSyncable(true)
    }

    fun register(bus: IEventBus) {
        ATTRIBUTES.register(bus)
    }
}

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object PlayerAttributeRegistration {
    @SubscribeEvent
    fun onEntityAttributeModification(event: EntityAttributeModificationEvent) {
        event.add(EntityType.PLAYER, ModAttributes.HUNGER_EFFICIENCY.get())
        event.add(EntityType.PLAYER, ModAttributes.THIRST_EFFICIENCY.get())
        event.add(EntityType.PLAYER, ModAttributes.MINING_SPEED.get())
        event.add(EntityType.PLAYER, ModAttributes.AIR_CAPACITY.get())
        event.add(EntityType.PLAYER, ModAttributes.RECOIL_REDUCTION.get())
        event.add(EntityType.PLAYER, ModAttributes.DISPERSION_REDUCTION.get())
        event.add(EntityType.PLAYER, ModAttributes.ARROW_SPREAD_REDUCTION.get())
        event.add(EntityType.PLAYER, ModAttributes.ARROW_SPEED_BONUS.get())
        event.add(EntityType.PLAYER, ModAttributes.OUTGOING_DAMAGE.get())
        event.add(EntityType.PLAYER, ModAttributes.HARMFUL_EFFECT_DURATION_REDUCTION.get())
        event.add(EntityType.PLAYER, ModAttributes.BENEFICIAL_EFFECT_DURATION.get())
    }
}
