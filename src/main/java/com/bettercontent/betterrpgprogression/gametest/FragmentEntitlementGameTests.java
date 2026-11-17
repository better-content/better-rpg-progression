package com.bettercontent.betterrpgprogression.gametest;

import com.bettercontent.betterrpgprogression.RpgStatsMod;
import com.bettercontent.betterrpgprogression.common.config.HeartFragmentConfig;
import com.bettercontent.betterrpgprogression.common.event.StillBeatingHeartAltarHandler;
import com.bettercontent.betterrpgprogression.common.item.HeartFragmentEntitlements;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.NetworkHooks;

/** Real server death/PlayerList clone/respawn; synthetic transport is NOT a client/network test. */
@GameTestHolder(RpgStatsMod.MODID)
@PrefixGameTestTemplate(false)
public final class FragmentEntitlementGameTests {
    private FragmentEntitlementGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void serverDeathCloneRetainsFullInventoryEntitlement(GameTestHelper helper) {
        helper.assertTrue(StillBeatingHeartAltarHandler.INSTANCE.isBloodMagicLoaded(),
                "Blood Magic provider absent: a gated-off death is not entitlement coverage");
        // Independent expected value: for level 4, ceil(4 * (2^(4/4) - 1)) = 4.
        // Refuse another server config rather than reuse the production calculation as an oracle.
        helper.assertTrue(HeartFragmentConfig.INSTANCE.entitlementScale() == 4,
                "Fixture requires the reviewed default heart entitlement scale 4");
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "HeartDeathTest");
        Path data = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id + ".dat");
        Path old = data.resolveSibling(data.getFileName() + "_old");
        Path stats = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id + ".json");
        Path advances = server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(id + ".json");
        for (Path path : List.of(data, old, stats, advances)) {
            helper.assertTrue(!Files.exists(path, LinkOption.NOFOLLOW_LINKS),
                    "Refusing a synthetic actor UUID with pre-existing saved state: " + path);
        }
        var fragment = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(
                RpgStatsMod.MODID, "heart_fragment"));
        helper.assertTrue(fragment != null, "Installed heart fragment registry item missing");
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInitializer<Channel>() {
            @Override protected void initChannel(Channel ch) {
                ch.pipeline().addLast("death_test_sink", new ChannelOutboundHandlerAdapter() {
                    @Override public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) {
                        ReferenceCountUtil.release(packet);
                        promise.setSuccess();
                    }
                });
                ch.pipeline().addLast("packet_handler", (ChannelHandler) connection);
            }
        });
        ServerPlayer original = new ServerPlayer(server, helper.getLevel(), profile);
        FullInventoryOnClone fill = new FullInventoryOnClone(id, original, fragment);
        try {
            helper.assertTrue(connection.channel() == channel && channel.isActive(),
                    "Synthetic transport not attached to the normal ServerPlayer");
            NetworkHooks.registerServerLoginChannel(connection,
                    new ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN));
            helper.assertTrue(NetworkHooks.getConnectionType(() -> connection) == ConnectionType.MODDED,
                    "Forge modded connection metadata missing");
            connection.setProtocol(ConnectionProtocol.PLAY);
            channel.runPendingTasks();
            channel.checkException();
            server.getPlayerList().placeNewPlayer(connection, original);
            server.getConnection().getConnections().add(connection);
            original.setGameMode(GameType.SURVIVAL);
            BlockPos site = helper.absolutePos(new BlockPos(2, 2, 2));
            original.teleportTo(helper.getLevel(), site.getX() + .5, site.getY(), site.getZ() + .5, 0, 0);
            helper.assertTrue(server.getPlayerList().getPlayer(id) == original,
                    "PlayerList did not register the ordinary server player");
            original.experienceLevel = 4;
            original.setHealth(1.0F);
            helper.assertTrue(HeartFragmentEntitlements.INSTANCE.nextPending(original.getPersistentData()) == null,
                    "New actor already carried a heart entitlement");
            // New ServerPlayer has 60 ticks of spawn protection. Void damage is a genuine
            // native BYPASSES_INVULNERABILITY death mechanism, NOT ordinary combat coverage.
            var fatal = helper.getLevel().damageSources().fellOutOfWorld();
            helper.assertTrue(fatal.is(DamageTypeTags.BYPASSES_INVULNERABILITY),
                    "Fixture cannot bypass native spawn protection without mutating its timer");
            MinecraftForge.EVENT_BUS.register(fill);
            helper.assertTrue(original.hurt(fatal, 2.0F) && original.isDeadOrDying(),
                    "Actual bypass-invulnerability fatal server damage did not reach final death");
            var before = HeartFragmentEntitlements.INSTANCE.pending(original.getPersistentData());
            helper.assertTrue(before.size() == 1 && BigInteger.valueOf(4).equals(before.get(0).getFragments()),
                    "Real uncanceled death must enqueue exactly four fragments once");
            UUID entitlement = before.get(0).getId();
            // This observer fills the NEW actor during the actual Forge Clone event. It never
            // posts a synthetic Clone/Respawn/Death event or calls the production handler.
            ServerPlayer replacement;
            try {
                replacement = server.getPlayerList().respawn(original, false);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(fill);
            }
            helper.assertTrue(fill.observed && replacement != original
                            && server.getPlayerList().getPlayer(id) == replacement,
                    "PlayerList did not run the native death clone and respawn lifecycle");
            helper.assertTrue(replacement.getInventory().items.stream().allMatch(stack ->
                            stack.is(Items.COBBLESTONE) && stack.getCount() == 64),
                    "Clone observer did not establish a full main inventory before respawn delivery");
            helper.assertTrue(replacement.getInventory().countItem(fragment) == 0 && fill.fragmentDrops == 0,
                    "Full inventory must neither insert nor spawn a fragment payout");
            var pending = HeartFragmentEntitlements.INSTANCE.pending(replacement.getPersistentData());
            helper.assertTrue(pending.size() == 1 && pending.get(0).getId().equals(entitlement)
                            && BigInteger.valueOf(4).equals(pending.get(0).getFragments()),
                    "Clone and full-inventory respawn must retain the one exact pending entitlement");
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(fill);
            try {
                // Only entities observed inside this actor's isolated native death/respawn
                // window and fixture radius are owned by this test; leave other entities alone.
                fill.discardOwnedSpawns();
                ServerPlayer current = server.getPlayerList().getPlayer(id);
                if (current != null) server.getPlayerList().remove(current);
            } finally {
                server.getConnection().getConnections().remove(connection);
                channel.finishAndReleaseAll();
                for (Path path : List.of(data, old, stats, advances)) {
                    try {
                        if (Files.isSymbolicLink(path)) throw new IOException("Synthetic state path replaced by symlink: " + path);
                        Files.deleteIfExists(path);
                    } catch (IOException cleanup) {
                        throw new IllegalStateException("Could not remove synthetic playerdata: " + path, cleanup);
                    }
                }
            }
        }
    }

    private static final class FullInventoryOnClone {
        private final UUID originalId;
        private final ServerPlayer actor;
        private final Item fragment;
        private final List<Entity> ownedSpawns = new ArrayList<>();
        private int fragmentDrops;
        private boolean observed;
        private FullInventoryOnClone(UUID originalId, ServerPlayer actor, Item fragment) {
            this.originalId = originalId; this.actor = actor; this.fragment = fragment;
        }

        @SubscribeEvent
        public void observeNativeSpawn(EntityJoinLevelEvent event) {
            Entity entity = event.getEntity();
            if (event.getLevel() != actor.level() || entity.distanceToSqr(actor) > 9.0D) return;
            if (entity instanceof ItemEntity item && item.getItem().is(fragment)) {
                fragmentDrops += item.getItem().getCount();
                ownedSpawns.add(item);
            } else if (entity instanceof ExperienceOrb) {
                ownedSpawns.add(entity); // Only the isolated actor's scoped death-XP fixture.
            }
        }
        void discardOwnedSpawns() { ownedSpawns.forEach(Entity::discard); }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void fillDuringActualClone(PlayerEvent.Clone event) {
            if (!event.isWasDeath() || !event.getOriginal().getUUID().equals(originalId)) return;
            for (int slot = 0; slot < event.getEntity().getInventory().items.size(); slot++) {
                event.getEntity().getInventory().items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            observed = true;
        }
    }
}
