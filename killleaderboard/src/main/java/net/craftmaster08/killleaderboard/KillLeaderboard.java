package net.craftmaster08.killleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(KillLeaderboard.MODID)
public class KillLeaderboard {
    public static final String MODID = "killleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(KillLeaderboard.class);
    private static MinecraftServer server;

    public KillLeaderboard() {
        if (!isStatsCorePresent()) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling KillLeaderboard.");
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
        LOGGER.info("Initialized KillLeaderboard mod");
    }

    private boolean isStatsCorePresent() {
        boolean present = net.minecraftforge.fml.ModList.get().isLoaded(StatsCore.MODID);
        LOGGER.info("StatsCore present: {}", present);
        return present;
    }

    private void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        LOGGER.info("KillLeaderboard server set");

        if (areDependenciesReady()) {
            registerCommands(event.getServer().getCommands().getDispatcher());
            LOGGER.info("Registered /kills command during server starting");
        } else {
            LOGGER.warn("Cannot register /kills command: Dependencies not fully initialized (ConfigManager: {}, DailyKillTracker: {})",
                    StatsCore.getConfigManager() != null ? "present" : "null",
                    StatsCore.getDailyStatsTracker() != null ? "present" : "null");
        }
    }

    private boolean areDependenciesReady() {
        return StatsCore.getConfigManager() != null &&
                StatsCore.getDailyStatsTracker() != null &&
                server != null;
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        KillRunCommand.register(dispatcher);
    }

    public static MinecraftServer getServer() {
        if (server == null) {
            LOGGER.warn("Attempted to access server before initialization");
        }
        return server;
    }
}