package net.craftmaster08.playtimeleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.craftmaster08.cm08statscore.statstracker.StatsTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod(PlaytimeLeaderboard.MODID)
public class PlaytimeLeaderboard {
    public static final String MODID = "playtimeleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(PlaytimeLeaderboard.class);
    private static MinecraftServer server;
    private static StatsTracker statsTracker;
    private static DailyStatsTracker dailyStatsTracker;

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

        if (areDependenciesReady()) {
            registerCommands(event.getServer().getCommands().getDispatcher());
            LOGGER.info("Registered /playtime command during server startup");
        } else {
            LOGGER.warn("Cannot register /playtime command: Dependencies not fully initialized (ConfigManager: {})",
                    StatsCore.getConfigManager() != null ? "present" : "null");
        }

        statsTracker = new StatsTracker(
                server,
                "minecraft:custom",
                "play_time",
                Stats.CUSTOM.get(Stats.PLAY_TIME)
        );

        dailyStatsTracker = new DailyStatsTracker(
                Path.of("playtime_daily.json"),
                "daily_playtimes",
                statsTracker,
                Stats.CUSTOM.get(Stats.PLAY_TIME)
        );

        LOGGER.info("StatsTracker & DailyStatsTracker initialized");
    }

    private boolean areDependenciesReady() {
        return StatsCore.getConfigManager() != null &&
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

    public static StatsTracker getStatsTracker() {
        if (statsTracker == null) {
            LOGGER.warn("StatsTracker not initialized yet");
        }
        return statsTracker;
    }

    public static DailyStatsTracker getDailyStatsTracker() {
        if (dailyStatsTracker == null) {
            LOGGER.warn("DailyStatsTracker not initialized yet");
        }
        return dailyStatsTracker;
    }
}