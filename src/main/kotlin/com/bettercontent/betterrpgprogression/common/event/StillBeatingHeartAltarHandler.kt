package com.bettercontent.betterrpgprogression.common.event

import com.bettercontent.betterrpgprogression.common.item.ModItems
import com.bettercontent.betterrpgprogression.common.item.StillBeatingHeartData
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fml.ModList
import wayoftime.bloodmagic.altar.IBloodAltar

object StillBeatingHeartAltarHandler {
    private const val BLOODMAGIC_MODID = "bloodmagic"
    private const val DISCOVERY_RADIUS = 8
    private const val DISCOVERY_INTERVAL_TICKS = 20

    private val trackedAltars = mutableMapOf<String, MutableSet<Long>>()

    fun isBloodMagicLoaded(): Boolean = ModList.get().isLoaded(BLOODMAGIC_MODID)

    fun discoverNear(player: net.minecraft.server.level.ServerPlayer) {
        if (!isBloodMagicLoaded()) return
        if (player.tickCount % DISCOVERY_INTERVAL_TICKS != 0) return

        val level = player.serverLevel()
        val origin = player.blockPosition()
        val dimensionKey = level.dimension().location().toString()
        val positions = trackedAltars.getOrPut(dimensionKey) { mutableSetOf() }

        BlockPos.betweenClosed(
            origin.offset(-DISCOVERY_RADIUS, -DISCOVERY_RADIUS, -DISCOVERY_RADIUS),
            origin.offset(DISCOVERY_RADIUS, DISCOVERY_RADIUS, DISCOVERY_RADIUS)
        ).forEach { pos ->
            val blockEntity = level.getBlockEntity(pos) ?: return@forEach
            if (isBloodAltar(blockEntity) && heartIn(blockEntity) != null) {
                positions += pos.asLong()
            }
        }
    }

    fun tickLevel(level: ServerLevel) {
        if (!isBloodMagicLoaded()) return

        val dimensionKey = level.dimension().location().toString()
        val positions = trackedAltars[dimensionKey] ?: return
        val iterator = positions.iterator()

        while (iterator.hasNext()) {
            val pos = BlockPos.of(iterator.next())
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity == null || !isBloodAltar(blockEntity)) {
                iterator.remove()
                continue
            }

            val heart = heartIn(blockEntity)
            if (heart == null) {
                iterator.remove()
                continue
            }

            val container = blockEntity as? Container
            if (container == null) {
                iterator.remove()
                continue
            }
            val inserted = fillHeartContainer(
                container,
                isHeartItem = { stack -> stack.`is`(ModItems.STILL_BEATING_HEART.get()) },
                fill = { amount -> fillAltar(blockEntity, amount) }
            )
            if (inserted > 0) {
                blockEntity.setChanged()
            }
        }
    }

    fun lpPerTick(level: Int): Int = StillBeatingHeartData.lpPerTick(level)

    internal fun fillHeartContainerForTests(container: Container, fill: (Int) -> Int): Int =
        fillHeartContainer(container, isHeartItem = { true }, fill = fill)

    private fun fillHeartContainer(container: Container, isHeartItem: (ItemStack) -> Boolean, fill: (Int) -> Int): Int {
        if (container.containerSize <= 0) return 0
        val stack = container.getItem(0)
        if (stack.isEmpty || !isHeartItem(stack) || !StillBeatingHeartData.isValid(stack)) return 0
        return fill(StillBeatingHeartData.lpPerTick(stack))
    }

    private fun heartIn(blockEntity: Any): ItemStack? {
        val container = blockEntity as? Container ?: return null
        if (container.containerSize <= 0) return null

        val stack = container.getItem(0)
        if (stack.`is`(ModItems.STILL_BEATING_HEART.get())) return stack
        return null
    }

    private fun isBloodAltar(blockEntity: Any): Boolean = blockEntity is IBloodAltar

    private fun fillAltar(blockEntity: Any, amount: Int): Int {
        return (blockEntity as IBloodAltar).fillMainTank(amount)
    }
}
