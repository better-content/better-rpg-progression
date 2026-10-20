package com.bettercontent.betterrpgprogression.diagnostics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Native lifecycle bridge only; the bounded sink retains no player, menu or world references. */
public final class PlaytestDiagnostics {
    public static final PlaytestLog LOG = new PlaytestLog("better_rpg_progression");
    private PlaytestDiagnostics() {}
    /** Kotlin-for-Forge owner registers these Java static subscribers explicitly. */
    public static void register(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.register(PlaytestDiagnostics.class);
        modBus.register(Startup.class);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> Client::register);
    }
    private static String version(String id) {
        return ModList.get().getModContainerById(id)
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse("absent");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && LOG.enabled("summary")) LOG.summary("server");
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { LOG.reset("server"); }

    private static String source() {
        var file = ModList.get().getModFileById("better_rpg_progression");
        return LOG.sourceIdentity(file == null || file.getFile() == null ? (java.nio.file.Path) null
                : file.getFile().findResource("META-INF", "better-content-source.properties"));
    }
    public static final class Startup {
        @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
            LOG.status("common", version("better_rpg_progression"), source(), () -> PlaytestLog.fields("enabled", true, "kotlinforforge_version", version("kotlinforforge"), "bloodmagic_version", version("bloodmagic")));
        }
    }
    public static final class Client {
        public static void register() { MinecraftForge.EVENT_BUS.register(Client.class); }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && LOG.enabled("summary")) LOG.summary("client");
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { LOG.reset("client"); }
    }
}
