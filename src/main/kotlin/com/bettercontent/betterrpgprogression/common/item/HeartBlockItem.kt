package com.bettercontent.betterrpgprogression.common.item

import com.bettercontent.betterrpgprogression.common.block.entity.HeartBlockEntity
import com.bettercontent.betterrpgprogression.common.block.ModBlocks
import com.bettercontent.betterrpgprogression.common.block.entity.ModBlockEntities
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

/** Block item that carries the installed-fragment investment across item boundaries. */
class HeartBlockItem(block: Block, properties: Properties) : BlockItem(block, properties) {
    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        super.appendHoverText(stack, level, tooltip, flag)
        tooltip += Component.translatable("item.better_rpg_progression.heart_block.fragments", fragments(stack))
            .withStyle(ChatFormatting.GRAY)
    }

    companion object {
        fun withFragments(fragments: Long): ItemStack {
            val stack = ItemStack(ModBlocks.HEART_BLOCK_ITEM.get())
            val data = CompoundTag()
            data.putLong(HeartBlockEntity.FRAGMENTS_TAG, fragments.coerceAtLeast(0))
            BlockItem.setBlockEntityData(stack, ModBlockEntities.HEART_BLOCK.get(), data)
            return stack
        }

        fun fragments(stack: ItemStack): Long = BlockItem.getBlockEntityData(stack)
            ?.getLong(HeartBlockEntity.FRAGMENTS_TAG)
            ?.coerceAtLeast(0)
            ?: 0
    }
}
