package com.bettercontent.betterrpgprogression.common.block.entity

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.common.block.ModBlocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModBlockEntities {
    val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RpgStatsMod.MODID)
    val HEART_BLOCK: RegistryObject<BlockEntityType<HeartBlockEntity>> = BLOCK_ENTITIES.register("heart_block") {
        BlockEntityType.Builder.of(::HeartBlockEntity, ModBlocks.HEART_BLOCK.get()).build(null)
    }

    fun register(bus: IEventBus) {
        BLOCK_ENTITIES.register(bus)
    }
}
