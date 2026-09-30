package com.bettercontent.betterrpgprogression.common.item

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModItems {
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, RpgStatsMod.MODID)

    val STILL_BEATING_HEART: RegistryObject<Item> = ITEMS.register("still_beating_heart") {
        StillBeatingHeartItem(
            Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.UNCOMMON)
        )
    }

    /** Ordinary, stackable final-death entitlement. */
    val HEART_FRAGMENT: RegistryObject<Item> = ITEMS.register("heart_fragment") {
        Item(Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON))
    }

    fun register(bus: IEventBus) {
        ITEMS.register(bus)
    }
}
