package com.bettercontent.betterrpgprogression.gametest;

import com.bettercontent.betterrpgprogression.RpgStatsMod;
import com.bettercontent.betterrpgprogression.common.data.StatsCap;
import com.bettercontent.betterrpgprogression.common.event.CommonForgeEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Covers the clone handler with real capability invalidation, not just an in-place reset. */
@GameTestHolder(RpgStatsMod.MODID)
@PrefixGameTestTemplate(false)
public final class StatsCloneGameTests {
    private StatsCloneGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void deathCloneKeepsAutoPlan(GameTestHelper helper) {
        checkClone(helper, true);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void nonDeathCloneKeepsLife(GameTestHelper helper) {
        checkClone(helper, false);
    }

    private static void checkClone(GameTestHelper helper, boolean wasDeath) {
        var original = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "oldStats"));
        var replacement = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "newStats"));
        var oldStats = StatsCap.INSTANCE.get(original);
        helper.assertTrue(oldStats != null, "Original stats capability missing");
        var plan = List.of("better_rpg_progression:arms", "", "better_rpg_progression:hands");
        oldStats.setAutoAllocationEnabled(true);
        oldStats.setAutoAllocationCursor(1);
        oldStats.getAutoAllocationPlan().addAll(plan);
        oldStats.setLifePeakLevel(42);
        oldStats.setUnspentPoints(3);
        oldStats.getAllocations().put("better_rpg_progression:arms", 8);
        original.invalidateCaps();
        helper.assertTrue(StatsCap.INSTANCE.get(original) == null, "Original capability was not invalidated");
        replacement.experienceLevel = 7;

        CommonForgeEvents.INSTANCE.onClone(new PlayerEvent.Clone(replacement, original, wasDeath));

        var copied = StatsCap.INSTANCE.get(replacement);
        helper.assertTrue(copied != null && copied.getAutoAllocationEnabled(), "Clone lost enabled auto plan");
        helper.assertTrue(plan.equals(copied.getAutoAllocationPlan()), "Clone lost or changed ordered plan");
        helper.assertTrue(copied.getAutoAllocationCursor() == 1, "Clone lost cursor");
        helper.assertTrue(copied.getLifePeakLevel() == (wasDeath ? 7 : 42), "Incorrect Life peak");
        helper.assertTrue(copied.getUnspentPoints() == (wasDeath ? 0 : 3), "Incorrect unspent Life points");
        helper.assertTrue(wasDeath ? copied.getAllocations().isEmpty() : copied.getAllocations().equals(oldStats.getAllocations()),
            "Incorrect Life allocations after clone");
        helper.assertTrue(StatsCap.INSTANCE.get(original) == null, "Clone left original capabilities revived");
        copied.getAutoAllocationPlan().clear();
        helper.assertTrue(plan.equals(oldStats.getAutoAllocationPlan()), "Clone shared mutable plan with original");
        helper.succeed();
    }
}
