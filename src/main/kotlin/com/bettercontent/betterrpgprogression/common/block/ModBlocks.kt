package com.bettercontent.betterrpgprogression.common.block

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.common.item.HeartBlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModBlocks {
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, RpgStatsMod.MODID)

    val HEART_BLOCK: RegistryObject<HeartBlock> = BLOCKS.register("heart_block") {
        HeartBlock(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.METAL))
    }

    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, RpgStatsMod.MODID)

    val HEART_BLOCK_ITEM: RegistryObject<Item> = ITEMS.register("heart_block") {
        HeartBlockItem(HEART_BLOCK.get(), Item.Properties().stacksTo(1))
    }

    fun register(bus: IEventBus) {
        BLOCKS.register(bus)
        ITEMS.register(bus)
    }
}
