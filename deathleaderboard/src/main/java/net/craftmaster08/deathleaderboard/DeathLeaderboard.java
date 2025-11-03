package net.craftmaster08.deathleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DeathLeaderboard.MODID)
public class DeathLeaderboard {
    public static final String MODID = "deathleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(DeathLeaderboard.class);
    private static MinecraftServer server;
    private boolean commandsRegistered = false;

    public DeathLeaderboard() {
        if (!isStatsCorePresent()) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling DeathLeaderboard.");
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
        LOGGER.info("Initialized DeathLeaderboard mod");
    }

    private boolean isStatsCorePresent() {
        boolean present = net.minecraftforge.fml.ModList.get().isLoaded(StatsCore.MODID);
        LOGGER.info("StatsCore present: {}", present);
        return present;
    }

    private void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        LOGGER.info("DeathLeaderboard server set");
/*
        if (!commandsRegistered && areDependenciesReady()) {
            registerCommands(event.getServer().getCommands().getDispatcher());
            commandsRegistered = true;
            LOGGER.info("Registered /deaths command during server starting");
        } else if (!areDependenciesReady()) {
            LOGGER.warn("Cannot register /deaths command: Dependencies not fully initialized (ConfigManager: {}, DailyDeathTracker: {})",
                    StatsCore.getConfigManager() != null ? "present" : "null",
                    StatsCore.getDailyStatsTracker() != null ? "present" : "null");
        } else {
            LOGGER.info("Skipping /deaths command registration; already registered");
        }*/

        if (areDependenciesReady()) {
            registerCommands(event.getServer().getCommands().getDispatcher());
            LOGGER.info("Registered /deaths command during server starting");
        } else {
            LOGGER.warn("Cannot register /deaths command: Dependencies not fully initialized (ConfigManager: {}, DailyDeathTracker: {})",
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
        DeathRunCommand.register(dispatcher);
    }

    public static MinecraftServer getServer() {
        if (server == null) {
            LOGGER.warn("Attempted to access server before initialization");
        }
        return server;
    }
}