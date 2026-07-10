package net.craftmaster08.cm08statscore.neoforge.platform;

import net.craftmaster08.cm08statscore.platform.PlatformHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Path;
import java.util.function.Consumer;

public class NeoForgePlatformHelper implements PlatformHelper {
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void onServerStartingLate(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerStartingEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void onEndServerTick(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> callback.accept(event.getServer()));
    }
}
