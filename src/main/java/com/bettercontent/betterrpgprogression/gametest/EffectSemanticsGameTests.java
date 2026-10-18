package com.bettercontent.betterrpgprogression.gametest;

import com.bettercontent.betterrpgprogression.RpgStatsMod;
import com.bettercontent.betterrpgprogression.common.attribute.ModAttributes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

/** Exercises the transformed LivingEntity.addEffect path, including Forge cure overrides. */
@GameTestHolder(RpgStatsMod.MODID)
@PrefixGameTestTemplate(false)
public final class EffectSemanticsGameTests {
    private EffectSemanticsGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void liverPreservesWholeIncomingEffect(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.getAttribute(ModAttributes.INSTANCE.getBENEFICIAL_EFFECT_DURATION().get()).setBaseValue(.25);

        var hidden = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, true, false, true);
        var incoming = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2, false, true, false, hidden, Optional.empty());
        incoming.setCurativeItems(List.of(new ItemStack(Items.HONEY_BOTTLE)));
        var expected = incoming.save(new CompoundTag());
        expected.putInt("Duration", 100);

        helper.assertTrue(player.addEffect(incoming), "Production addEffect rejected the incoming effect");
        var applied = player.getEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(applied != null && expected.equals(applied.save(new CompoundTag())),
            "Liver changed flags, curatives, hidden state or factor data while scaling duration");
        helper.assertTrue(incoming.getDuration() == 80, "Liver mutated the caller's instance");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void preservationKeepsMilkBenefitsWithoutScalingTwice(GameTestHelper helper) {
        var player = new MilkPlayer(helper.getLevel());
        player.removeAllEffects();
        var attribute = player.getAttribute(ModAttributes.INSTANCE.getBENEFICIAL_EFFECT_DURATION().get());
        attribute.setBaseValue(.125);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 800));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 800));
        var before = player.getEffect(MobEffects.MOVEMENT_SPEED).save(new CompoundTag());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "Native milk use did not start");
        var removal = new net.minecraftforge.event.entity.living.MobEffectEvent.Remove(player, MobEffects.MOVEMENT_SPEED);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(removal);
        helper.assertTrue(removal.isCanceled(), "Preservation did not guard the milk-use episode; attribute=" + attribute.getValue());
        player.finishMilk();
        var retained = player.getEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(retained != null && before.equals(retained.save(new CompoundTag())),
                "Milk must retain the exact beneficial instance, not apply duration scaling twice; before=" + before
                        + "; after=" + (retained == null ? "null" : retained.save(new CompoundTag())));
        helper.assertTrue(!player.hasEffect(MobEffects.POISON), "Milk must still remove harmful effects");
        player.stopUsingItem();
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(!player.hasEffect(MobEffects.MOVEMENT_SPEED), "Preservation leaked outside milk use");
        attribute.setBaseValue(0);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 800));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        player.finishMilk();
        helper.assertTrue(!player.hasEffect(MobEffects.MOVEMENT_SPEED), "Uninvested players must retain normal milk behaviour");
        player.stopUsingItem();
        helper.succeed();
    }

    /** Exposes the native completion boundary, including Forge's Finish event, without faking it. */
    private static final class MilkPlayer extends net.minecraftforge.common.util.FakePlayer {
        MilkPlayer(net.minecraft.server.level.ServerLevel level) {
            super(level, new com.mojang.authlib.GameProfile(
                    java.util.UUID.fromString("883c5ea5-5c5a-49e9-924c-2f3e2b1526de"), "PreservationTest"));
        }

        void finishMilk() { completeUsingItem(); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void reprojectionClearsRemovedOwnedEffectsOnly(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("f0683470-024e-46ef-9293-a5598eafbf24"), "ProjectionTest"));
        var attribute = player.getAttribute(ModAttributes.INSTANCE.getOUTGOING_DAMAGE().get());
        var owned = java.util.UUID.fromString("6b9a120f-1300-4739-9bb7-6b179236d631");
        var foreign = java.util.UUID.fromString("948ca86f-3232-49fa-a27b-26fde94defec");
        attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                owned, "better_rpg_progression:removed_definition", .3,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE));
        attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                foreign, "other_owner", .2,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE));
        com.bettercontent.betterrpgprogression.common.attribute.StatAttributeProjector.INSTANCE.reapply(player);
        helper.assertTrue(attribute.getModifier(owned) == null, "Removed RPG projection survived reprojection");
        helper.assertTrue(attribute.getModifier(foreign) != null, "Reprojection removed another owner's modifier");
        attribute.removeModifier(foreign);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void lungsIncreaseCapacityWithoutRefillingCurrentAir(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var attribute = player.getAttribute(ModAttributes.INSTANCE.getAIR_CAPACITY().get());
        attribute.setBaseValue(1.0);
        player.setAirSupply(120);
        helper.assertTrue(player.getMaxAirSupply() == 300, "Baseline air capacity changed");
        attribute.setBaseValue(1.5);
        helper.assertTrue(player.getMaxAirSupply() == 450, "Lungs failed to raise air capacity");
        helper.assertTrue(player.getAirSupply() == 120, "Increasing capacity refilled current air");
        attribute.setBaseValue(1.0);
        helper.succeed();
    }
}
