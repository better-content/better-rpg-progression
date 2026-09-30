package com.bettercontent.betterrpgprogression.common.block

import com.bettercontent.betterrpgprogression.common.block.entity.HeartBlockEntity
import com.bettercontent.betterrpgprogression.common.block.entity.ModBlockEntities
import com.bettercontent.betterrpgprogression.common.item.HeartBlockItem
import com.bettercontent.betterrpgprogression.common.item.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.phys.BlockHitResult

class HeartBlock(properties: BlockBehaviour.Properties) : BaseEntityBlock(properties) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = HeartBlockEntity(pos, state)

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val held = player.getItemInHand(hand)
        if (!held.`is`(ModItems.HEART_FRAGMENT.get())) return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS

        val heart = level.getBlockEntity(pos) as? HeartBlockEntity ?: return InteractionResult.PASS
        if (!heart.installOneFragment()) return InteractionResult.PASS
        if (!player.abilities.instabuild) held.shrink(1)
        return InteractionResult.CONSUME
    }

    override fun getCloneItemStack(level: net.minecraft.world.level.BlockGetter, pos: BlockPos, state: BlockState): ItemStack {
        val heart = (level as? Level)?.getBlockEntity(pos) as? HeartBlockEntity
        return HeartBlockItem.withFragments(heart?.installedFragments ?: 0)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return createTickerHelper(type, ModBlockEntities.HEART_BLOCK.get(), HeartBlockEntity::tick)
    }
}
