package com.bettercontent.betterrpgprogression.common.item

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import java.util.UUID

class StillBeatingHeartItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: net.minecraft.world.entity.player.Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (level.isClientSide) return InteractionResultHolder.success(stack)
        val serverPlayer = player as? ServerPlayer ?: return InteractionResultHolder.pass(stack)
        val fragments = runCatching { HeartFragmentData.fragmentsForLevelBig(StillBeatingHeartData.getLevel(stack)) }.getOrNull()
            ?: return InteractionResultHolder.fail(stack)
        if (fragments <= java.math.BigInteger.ZERO) return InteractionResultHolder.pass(stack)

        // The pending row and the consumed legacy item share playerdata, so an inventory-full
        // conversion waits safely for ordinary fragment delivery rather than dropping an entity.
        val entitlement = HeartFragmentEntitlements.Entitlement(UUID.randomUUID(), fragments)
        if (!HeartFragmentEntitlements.enqueue(serverPlayer.persistentData, entitlement)) return InteractionResultHolder.fail(stack)
        if (!serverPlayer.abilities.instabuild) stack.shrink(1)
        return InteractionResultHolder.consume(stack)
    }

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        if (StillBeatingHeartData.getData(stack) == null) return

        val level = StillBeatingHeartData.getLevel(stack)
        tooltip += Component.translatable("item.better_rpg_progression.still_beating_heart.tooltip.level", level)
        val fragments = runCatching { HeartFragmentData.fragmentsForLevelBig(level) }.getOrNull()
        tooltip += if (fragments == null) {
            Component.translatable("item.better_rpg_progression.still_beating_heart.tooltip.convert_unresolved")
        } else {
            Component.translatable("item.better_rpg_progression.still_beating_heart.tooltip.convert", fragments)
        }
    }

}
