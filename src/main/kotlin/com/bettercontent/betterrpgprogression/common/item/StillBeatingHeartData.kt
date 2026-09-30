package com.bettercontent.betterrpgprogression.common.item

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object StillBeatingHeartData {
    const val DATA_TAG: String = "StillBeatingHeartData"
    private const val CAPTURED_DEATH_LEVEL_TAG: String = "better_rpg_progression_captured_death_level"
    private const val SCHEMA_VERSION: Int = 3
    private const val BASE_LP_PER_TICK: Int = 5
    private const val LEVELS_PER_DOUBLING: Double = 10.0
    private const val MAX_LP_PER_TICK: Int = 4096

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun create(player: ServerPlayer, source: DamageSource): ItemStack {
        return createForLevel(player.experienceLevel, ModItems.STILL_BEATING_HEART.get())
    }

    @JvmStatic
    fun createForLevel(level: Int, item: Item): ItemStack {
        if (level <= 0) return ItemStack.EMPTY
        val stack = ItemStack(item)
        val root = CompoundTag()

        root.putInt("schema_version", SCHEMA_VERSION)
        root.putInt("level", level.coerceAtLeast(0))

        stack.orCreateTag.put(DATA_TAG, root)
        return stack
    }

    @JvmStatic
    fun captureDeathLevel(persistentData: CompoundTag, level: Int) {
        persistentData.putInt(CAPTURED_DEATH_LEVEL_TAG, level.coerceAtLeast(0))
    }

    @JvmStatic
    fun consumeCapturedDeathLevel(persistentData: CompoundTag): Int {
        val level = persistentData.getInt(CAPTURED_DEATH_LEVEL_TAG).coerceAtLeast(0)
        persistentData.remove(CAPTURED_DEATH_LEVEL_TAG)
        return level
    }

    @JvmStatic
    fun getData(stack: ItemStack): CompoundTag? {
        val tag = stack.tag ?: return null
        if (!tag.contains(DATA_TAG, Tag.TAG_COMPOUND.toInt())) return null
        return tag.getCompound(DATA_TAG)
    }

    @JvmStatic
    fun getLevel(stack: ItemStack): Int {
        val data = getData(stack) ?: return 0
        return data.getInt("level")
    }

    @JvmStatic
    fun isValid(stack: ItemStack): Boolean = getLevel(stack) > 0

    @JvmStatic
    fun lpPerTick(level: Int): Int {
        if (level <= 0) return 0
        val scaled = BASE_LP_PER_TICK * 2.0.pow(max(0, level) / LEVELS_PER_DOUBLING)
        return min(MAX_LP_PER_TICK, max(1, scaled.toInt()))
    }

    @JvmStatic
    fun lpPerTick(stack: ItemStack): Int = lpPerTick(getLevel(stack))
}
