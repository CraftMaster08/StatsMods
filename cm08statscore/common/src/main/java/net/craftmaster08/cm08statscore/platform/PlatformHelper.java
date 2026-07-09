package net.craftmaster08.cm08statscore.platform;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * The small set of loader-specific hooks StatsMods needs. Implemented once per loader
 * (fabric/forge/neoforge) and loaded via {@link java.util.ServiceLoader} through {@link Services}.
 */
public interface PlatformHelper {

    boolean isModLoaded(String modId);

    Path getConfigDir();

    /**
     * Registered by cm08statscore itself during its own bootstrap. Guaranteed to run before
     * any callback registered through {@link #onServerStartingLate}.
     */
    void onServerStarting(Consumer<MinecraftServer> callback);

    /**
     * Registered by addon mods whose server-starting logic depends on cm08statscore having
     * already initialized its config/provider (see {@link #onServerStarting}).
     */
    void onServerStartingLate(Consumer<MinecraftServer> callback);

    void onEndServerTick(Consumer<MinecraftServer> callback);
}
