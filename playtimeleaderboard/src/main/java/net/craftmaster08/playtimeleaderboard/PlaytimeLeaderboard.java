package net.craftmaster08.playtimeleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PlaytimeLeaderboard.MODID)
public class PlaytimeLeaderboard {
    public static final String MODID = "playtimeleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(PlaytimeLeaderboard.class);
    private static MinecraftServer server;
    private boolean commandsRegistered = false;

    public PlaytimeLeaderboard() {
        if (!isStatsCorePresent()) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling PlaytimeLeaderboard.");
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
        LOGGER.info("Initialized PlaytimeLeaderboard mod");
    }

    private boolean isStatsCorePresent() {
        boolean present = net.minecraftforge.fml.ModList.get().isLoaded(StatsCore.MODID);
        LOGGER.info("StatsCore present: {}", present);
        return present;
    }

    private void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        LOGGER.info("PlaytimeLeaderboard server set");

        if (!commandsRegistered && areDependenciesReady()) {
            registerCommands(event.getServer().getCommands().getDispatcher());
            commandsRegistered = true;
            LOGGER.info("Registered /playtime command during server starting");
        } else if (!areDependenciesReady()) {
            LOGGER.warn("Cannot register /playtime command: Dependencies not fully initialized (ConfigManager: {}, DailyPlaytimeTracker: {})",
                    StatsCore.getConfigManager() != null ? "present" : "null",
                    StatsCore.getDailyStatsTracker() != null ? "present" : "null");
        } else {
            LOGGER.info("Skipping /playtime command registration; already registered");
        }
    }

    private boolean areDependenciesReady() {
        return StatsCore.getConfigManager() != null &&
                StatsCore.getDailyStatsTracker() != null &&
                server != null;
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        PlaytimeRunCommand.register(dispatcher);
    }

    public static MinecraftServer getServer() {
        if (server == null) {
            LOGGER.warn("Attempted to access server before initialization");
        }
        return server;
    }
}