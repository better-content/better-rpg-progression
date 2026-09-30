package com.bettercontent.betterrpgprogression.common.event

import com.bettercontent.betterrpgprogression.RpgStatsMod
import com.bettercontent.betterrpgprogression.common.attribute.MiningSpeedScaling
import com.bettercontent.betterrpgprogression.common.attribute.OutgoingDamageScaling
import com.bettercontent.betterrpgprogression.common.attribute.ModAttributes
import com.bettercontent.betterrpgprogression.common.attribute.StatAttributeProjector
import com.bettercontent.betterrpgprogression.common.data.PlayerStatsProvider
import com.bettercontent.betterrpgprogression.common.data.StatsCap
import com.bettercontent.betterrpgprogression.common.item.ModItems
import com.bettercontent.betterrpgprogression.common.item.StillBeatingHeartData
import com.bettercontent.betterrpgprogression.common.item.HeartFragmentEntitlements
import com.bettercontent.betterrpgprogression.common.network.Network
import com.bettercontent.betterrpgprogression.common.points.PointAwarder
import com.bettercontent.betterrpgprogression.common.reload.RegistryState
import com.bettercontent.betterrpgprogression.common.reload.StatReloadListener
import com.bettercontent.betterrpgprogression.common.network.packets.S2CStatDefsSync
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.living.LivingHurtEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = RpgStatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object CommonForgeEvents {
    private const val LOST_ALLOCATION_TAG = "better_rpg_progression_pending_allocation_loss"
    private const val LEGACY_PENDING_HEARTS_TAG: String = "better_rpg_progression_pending_hearts"

    @SubscribeEvent
    fun onAttachCaps(event: AttachCapabilitiesEvent<Entity>) {
        val e = event.`object`
        if (e is Player) {
            event.addCapability(StatsCap.KEY, PlayerStatsProvider())
        }
    }

    @SubscribeEvent
    fun onAddReloadListeners(event: AddReloadListenerEvent) {
        event.addListener(StatReloadListener())
    }

    @SubscribeEvent
    fun onClone(event: PlayerEvent.Clone) {
        val oldP = event.original
        val newP = event.entity

        if (event.isWasDeath) {
            newP.getCapability(StatsCap.CAP).ifPresent { newStats ->
                // Wipe, but baseline peak to current XP level to avoid instant refunds on keep-XP rules.
                newStats.resetForDeath((newP as? ServerPlayer)?.experienceLevel ?: 0)
                if (newP is ServerPlayer && oldP.persistentData.getBoolean(LOST_ALLOCATION_TAG)) {
                    oldP.persistentData.remove(LOST_ALLOCATION_TAG)
                    com.bettercontent.betterrpgprogression.common.points.LifeAllocationEvents.post(newP,
                        com.bettercontent.betterrpgprogression.api.event.LifeAllocationEvent.State.LOST_ON_DEATH,
                        com.bettercontent.betterrpgprogression.common.points.LifeAllocationEvents.newEpisode(newP))
                }
            }
        } else {
            oldP.getCapability(StatsCap.CAP).ifPresent { oldStats ->
                newP.getCapability(StatsCap.CAP).ifPresent { newStats ->
                    newStats.lifePeakLevel = oldStats.lifePeakLevel
                    newStats.unspentPoints = oldStats.unspentPoints
                    newStats.allocations.clear()
                    newStats.allocations.putAll(oldStats.allocations)
                    newStats.autoAllocationEnabled = oldStats.autoAllocationEnabled
                    newStats.autoAllocationCursor = oldStats.autoAllocationCursor
                    newStats.autoAllocationPlan.clear()
                    newStats.autoAllocationPlan.addAll(oldStats.autoAllocationPlan)
                }
            }
        }

        if (event.isWasDeath) {
            transferHeartEntitlements(oldP, newP)
        }

        if (newP is ServerPlayer) {
            StatAttributeProjector.reapply(newP)
            Network.syncTo(newP)
        }
    }

    @SubscribeEvent
    fun onRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        val player = event.entity as? ServerPlayer ?: return
        migrateLegacyPendingHearts(player)
        deliverPendingFragments(player)
    }

    @SubscribeEvent
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val p = event.entity
        if (p is ServerPlayer) {
            val stats = StatsCap.get(p)
            if (stats != null && stats.lifePeakLevel == 0 && stats.totalPointsThisLife() == 0) {
                // Baseline to current (prevents retro points for existing worlds)
                stats.lifePeakLevel = p.experienceLevel
            }

            // Send defs first, then stats snapshot
            Network.sendTo(p, S2CStatDefsSync.fromDefs(RegistryState.activeSnapshot().values.toList()))
            StatAttributeProjector.reapply(p)
            Network.syncTo(p)
            migrateLegacyPendingHearts(p)
            deliverPendingFragments(p)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBreakSpeed(event: PlayerEvent.BreakSpeed) {
        val multiplier = event.entity.getAttributeValue(ModAttributes.MINING_SPEED.get())
        event.newSpeed = MiningSpeedScaling.scale(event.newSpeed, multiplier)
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onLivingHurt(event: LivingHurtEvent) {
        if (!event.isCanceled) event.amount = OutgoingDamageScaling.scale(event.source, event.entity, event.amount)
    }

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val p = event.player
        if (p is ServerPlayer) {
            PointAwarder.tick(p)
            deliverPendingFragments(p)
        }
    }

    @SubscribeEvent
    fun onLevelTick(event: TickEvent.LevelTickEvent) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return
        val level = event.level as? net.minecraft.server.level.ServerLevel ?: return
        // Heart fragments are installed into the dedicated heart block; the retired altar-slot
        // item generator must never fill an altar from this global level tick.
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onLivingDeathCaptureLevel(event: LivingDeathEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (player.level().isClientSide || player.isSpectator) return
        if (!StillBeatingHeartAltarHandler.isBloodMagicLoaded()) return

        // Configurable Death clears XP in its normal-priority death handler. Snapshot the
        // level before that happens, but wait until LOWEST to confirm the death survived
        // any cancellation before creating the heart.
        HeartFragmentEntitlements.captureFinalDeath(player.persistentData, player.experienceLevel)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun recordAllocationLoss(event: LivingDeathEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (event.isCanceled) return
        player.persistentData.putBoolean(LOST_ALLOCATION_TAG, (StatsCap.get(player)?.totalAllocated() ?: 0) > 0)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onLivingDeath(event: LivingDeathEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (event.isCanceled) {
            HeartFragmentEntitlements.discardCapturedDeath(player.persistentData)
            return
        }
        if (player.level().isClientSide || player.isSpectator) return
        if (!StillBeatingHeartAltarHandler.isBloodMagicLoaded()) return

        HeartFragmentEntitlements.finalizeCapturedDeath(player.persistentData)?.let { entitlement ->
            HeartFragmentEntitlements.enqueue(player.persistentData, entitlement)
        }
    }

    private fun transferHeartEntitlements(from: Player, to: Player) {
        val fromData = from.persistentData
        val toData = to.persistentData
        listOf(
            "better_rpg_progression_captured_heart_entitlement",
            "better_rpg_progression_pending_heart_fragment_entitlements",
            "better_rpg_progression_next_heart_entitlement_sequence",
            "better_rpg_progression_completed_heart_entitlement_through",
            "better_rpg_progression_unresolved_heart_entitlement",
            LEGACY_PENDING_HEARTS_TAG
        ).forEach { key ->
            if (fromData.contains(key)) {
                fromData.get(key)?.let { toData.put(key, it.copy()) }
                fromData.remove(key)
            }
        }
    }

    private fun migrateLegacyPendingHearts(player: ServerPlayer) {
        val data = player.persistentData
        if (!data.contains(LEGACY_PENDING_HEARTS_TAG, Tag.TAG_LIST.toInt())) return
        val legacy = data.getList(LEGACY_PENDING_HEARTS_TAG, Tag.TAG_COMPOUND.toInt())
        val rows = (0 until legacy.size).mapNotNull { index ->
            val saved = legacy.getCompound(index)
            val stack = ItemStack.of(saved)
            if (!stack.`is`(ModItems.STILL_BEATING_HEART.get()) || !StillBeatingHeartData.isValid(stack)) return@mapNotNull null
            HeartFragmentEntitlements.LegacyHeart(
                level = StillBeatingHeartData.getLevel(stack),
                fingerprint = saved.toString()
            )
        }
        HeartFragmentEntitlements.migrateLegacyPending(data, rows)
        // A repeated migration derives the same UUIDs until this removal persists, so it cannot
        // duplicate rewards across a crash/reconnect boundary.
        data.remove(LEGACY_PENDING_HEARTS_TAG)
    }

    private fun deliverPendingFragments(player: ServerPlayer) {
        val data = player.persistentData
        val entitlement = HeartFragmentEntitlements.nextPending(data) ?: return
        // One ordinary stack per player tick bounds inventory work even for a very large exact
        // entitlement; no fragment entities are ever spawned as an overflow fallback.
        val offered = entitlement.fragments.min(java.math.BigInteger.valueOf(64)).intValueExact()
        val stack = ItemStack(ModItems.HEART_FRAGMENT.get(), offered)
        player.inventory.add(stack)
        val delivered = java.math.BigInteger.valueOf((offered - stack.count).toLong())
        if (delivered <= java.math.BigInteger.ZERO) return
        check(HeartFragmentEntitlements.recordDelivery(data, entitlement.id, delivered)) {
            "Heart entitlement disappeared during delivery: ${entitlement.id}"
        }
    }

}

/** Retained for the old personalized-heart slot policy tests; fragment delivery now uses normal merging. */
internal object PendingHeartSlotPolicy {
    fun lastEmptySlot(emptySlots: List<Boolean>): Int? = emptySlots.indices.reversed().firstOrNull { emptySlots[it] }
}
