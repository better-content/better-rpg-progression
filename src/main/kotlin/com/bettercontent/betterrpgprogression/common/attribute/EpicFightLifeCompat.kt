package com.bettercontent.betterrpgprogression.common.attribute

import com.bettercontent.betterrpgprogression.RpgStatsMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import yesman.epicfight.api.animation.types.AirSlashAnimation
import yesman.epicfight.gameasset.EpicFightSkills
import yesman.epicfight.skill.Skill
import yesman.epicfight.world.capabilities.EpicFightCapabilities
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener
import java.util.Collections
import java.util.UUID
import java.util.WeakHashMap

/** Optional Epic Fight per-player hooks. Never mutate its shared animation or other players' skills. */
object EpicFightLifeCompat {
    private val staminaId = UUID.nameUUIDFromBytes(ResourceLocation(RpgStatsMod.MODID, "air_attack_cost").toString().toByteArray())
    private val damageId = UUID.nameUUIDFromBytes(ResourceLocation(RpgStatsMod.MODID, "air_slash_damage").toString().toByteArray())
    private val registered = Collections.newSetFromMap(WeakHashMap<PlayerPatch<*>, Boolean>())

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val player = event.player as? ServerPlayer ?: return
        val patch = EpicFightCapabilities.getPlayerPatch(player) ?: return
        if (!registered.add(patch)) return
        val listener = patch.eventListener
        listener.removeListener(PlayerEventListener.EventType.SKILL_CONSUME_EVENT, staminaId)
        listener.addEventListener(PlayerEventListener.EventType.SKILL_CONSUME_EVENT, staminaId, { consume ->
            val attacker = consume.playerPatch.original as? ServerPlayer ?: return@addEventListener
            if (consume.skill !== EpicFightSkills.BASIC_ATTACK ||
                consume.resourceType != Skill.Resource.STAMINA || consume.amount <= 0f) return@addEventListener
            // In pinned Epic Fight, only the air branch of BasicAttack has a positive cost;
            // the dash and normal branches consume zero. The weight-adjusted cost arrives here.
            val reduction = attacker.getAttributeValue(ModAttributes.AIR_ATTACK_STAMINA_REDUCTION.get())
            if (reduction > 0.0) consume.amount = LifeEffectsMath.airAttackCost(consume.amount, reduction)
        })
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_HURT, damageId)
        listener.addEventListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_HURT, damageId, { hit ->
            val attacker = hit.playerPatch.original as? ServerPlayer ?: return@addEventListener
            if (hit.forgeEvent.isCanceled || hit.damageSource.animation?.get() !is AirSlashAnimation) return@addEventListener
            val bonus = attacker.getAttributeValue(ModAttributes.AIR_SLASH_DAMAGE_BONUS.get())
            if (bonus > 0.0) hit.forgeEvent.amount = LifeEffectsMath.airSlashDamage(hit.forgeEvent.amount, bonus)
        })
    }
}
