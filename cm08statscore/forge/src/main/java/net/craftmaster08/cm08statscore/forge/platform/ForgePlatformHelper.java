package net.craftmaster08.cm08statscore.forge.platform;

import net.craftmaster08.cm08statscore.platform.PlatformHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

public class ForgePlatformHelper implements PlatformHelper {
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
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void onServerStartingLate(Consumer<MinecraftServer> callback) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerStartingEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void onEndServerTick(Consumer<MinecraftServer> callback) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) -> callback.accept(event.getServer()));
    }
}
