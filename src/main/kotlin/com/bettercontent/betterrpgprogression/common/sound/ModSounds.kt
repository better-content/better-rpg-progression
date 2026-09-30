package com.bettercontent.betterrpgprogression.common.sound

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.common.salience.AspectIdentity
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModSounds {
    private val register = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RpgStatsMod.MODID)
    private val aspects: Map<String, RegistryObject<SoundEvent>> = AspectIdentity.entries.associate { aspect ->
        val path = "aspect.${aspect.name.lowercase()}"
        aspect.name.lowercase() to register.register(path) {
            SoundEvent.createVariableRangeEvent(ResourceLocation(RpgStatsMod.MODID, path))
        }
    }

    fun register(bus: IEventBus) = register.register(bus)
    fun forAspect(aspect: AspectIdentity): SoundEvent = aspects.getValue(aspect.name.lowercase()).get()
}
