package com.bettercontent.betterrpgprogression.common.event;

import com.bettercontent.betterrpgprogression.common.attribute.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Cancel milk's beneficial removal, never re-add/rescale an existing effect. */
public final class MilkPreservation {
    private static final Set<UUID> DRINKING = new HashSet<>();
    private MilkPreservation() {}

    @SubscribeEvent
    public static void onStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(Items.MILK_BUCKET)
                && player.getAttributeValue(ModAttributes.INSTANCE.getBENEFICIAL_EFFECT_DURATION().get()) > 0.0) {
            DRINKING.add(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRemove(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !DRINKING.contains(player.getUUID())) return;
        // The last milk stack may already be empty at this native boundary, so checking
        // getUseItem().is(MILK_BUCKET) here loses protection on the normal one-bucket path.
        var effect = event.getEffectInstance();
        if (effect != null && effect.getEffect().isBeneficial()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) DRINKING.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) DRINKING.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && (!player.isUsingItem() || !player.getUseItem().is(Items.MILK_BUCKET))) {
            DRINKING.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) DRINKING.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) DRINKING.remove(player.getUUID());
    }
}
