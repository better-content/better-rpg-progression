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
    public static void vitalityPreservesWholeIncomingEffect(GameTestHelper helper) {
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
            "Vitality changed flags, curatives, hidden state or factor data while scaling duration");
        helper.assertTrue(incoming.getDuration() == 80, "Vitality mutated the caller's instance");
        helper.succeed();
    }
}
