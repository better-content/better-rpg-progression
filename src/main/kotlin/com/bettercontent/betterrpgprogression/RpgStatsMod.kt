package com.bettercontent.betterrpgprogression

import com.bettercontent.betterrpgprogression.common.item.ModItems
import com.bettercontent.betterrpgprogression.common.block.ModBlocks
import com.bettercontent.betterrpgprogression.common.block.entity.ModBlockEntities
import com.bettercontent.betterrpgprogression.common.config.HeartFragmentConfig
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig
import com.bettercontent.betterrpgprogression.common.attribute.ModAttributes
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.sound.ModSounds
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

@Mod(RpgStatsMod.MODID)
object RpgStatsMod {
    const val MODID: String = "better_rpg_progression"

    init {
        val modBus = KotlinModLoadingContext.get().getKEventBus()
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, HeartFragmentConfig.SPEC)
        ModAttributes.register(modBus)
        ModItems.register(modBus)
        ModBlocks.register(modBus)
        ModBlockEntities.register(modBus)
        ModSounds.register(modBus)
        Network.init()
        // Everything else is registered via @EventBusSubscriber objects.
    }
}
