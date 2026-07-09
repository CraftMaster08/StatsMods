package net.craftmaster08.playtimeleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.platform.Services;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class PlaytimeLeaderboard {
    public static final String MODID = "playtimeleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(PlaytimeLeaderboard.class);

    private static DailyStatsTracker dailyTracker;

    private PlaytimeLeaderboard() {}

    /** Called once by each loader's mod entrypoint. */
    public static void init() {
        if (!Services.PLATFORM.isModLoaded(StatsCore.MODID)) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling PlaytimeLeaderboard.");
            return;
        }
        // Registered "late" so cm08statscore's own server-starting listener (which loads the config)
        // has already run by the time this fires.
        Services.PLATFORM.onServerStartingLate(PlaytimeLeaderboard::onServerStarting);
    }

    private static void onServerStarting(MinecraftServer server) {
        ConfigManager cfg = StatsCore.getConfigManager();
        if (cfg == null) {
            LOGGER.error("StatsCore configuration not initialized; PlaytimeLeaderboard disabled");
            return;
        }

        var playTimeStat = Stats.CUSTOM.get(Stats.PLAY_TIME);

        dailyTracker = new DailyStatsTracker(
                List.of(playTimeStat),
                "daily_playtimes",
                "last_known_playtimes",
                72000.0
        );

        dailyTracker.setDailyResetTime(cfg.dailyResetTime);
        StatsCore.registerDailyTracker(dailyTracker);

        PlaytimeRunCommand.register(server.getCommands().getDispatcher());
        LOGGER.info("PlaytimeLeaderboard initialized");
    }

    public static DailyStatsTracker getDailyTracker() {
        return dailyTracker;
    }
}
