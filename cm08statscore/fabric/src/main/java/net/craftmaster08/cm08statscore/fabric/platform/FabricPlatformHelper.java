package net.craftmaster08.cm08statscore.fabric.platform;

import net.craftmaster08.cm08statscore.platform.PlatformHelper;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.function.Consumer;

public class FabricPlatformHelper implements PlatformHelper {
    private static final ResourceLocation LATE_PHASE = ResourceLocation.fromNamespaceAndPath("cm08statscore", "late");
    private static boolean latePhaseRegistered = false;

    private static synchronized void ensureLatePhase() {
        if (!latePhaseRegistered) {
            ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);
            latePhaseRegistered = true;
        }
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STARTING.register(callback::accept);
    }

    @Override
    public void onServerStartingLate(Consumer<MinecraftServer> callback) {
        ensureLatePhase();
        ServerLifecycleEvents.SERVER_STARTING.register(LATE_PHASE, callback::accept);
    }

    @Override
    public void onEndServerTick(Consumer<MinecraftServer> callback) {
        ServerTickEvents.END_SERVER_TICK.register(callback::accept);
    }
}
