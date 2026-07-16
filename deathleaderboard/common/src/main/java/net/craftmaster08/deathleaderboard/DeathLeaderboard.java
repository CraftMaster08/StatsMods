package net.craftmaster08.deathleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.platform.Services;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DeathLeaderboard {
    public static final String MODID = "deathleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(DeathLeaderboard.class);

    private static DailyStatsTracker dailyTracker;

    private DeathLeaderboard() {}

    /** Called once by each loader's mod entrypoint. */
    public static void init() {
        if (!Services.PLATFORM.isModLoaded(StatsCore.MODID)) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling DeathLeaderboard.");
            return;
        }
        Services.PLATFORM.onServerStartingLate(DeathLeaderboard::onServerStarting);
    }

    private static void onServerStarting(MinecraftServer server) {
        ConfigManager cfg = StatsCore.getConfigManager();
        if (cfg == null) {
            LOGGER.error("StatsCore configuration not initialized; DeathLeaderboard disabled");
            return;
        }

        var deathsStat = Stats.CUSTOM.get(Stats.DEATHS);

        dailyTracker = new DailyStatsTracker(
                List.of(deathsStat),
                "daily_deaths",
                "last_known_deaths",
                1.0,
                "deaths_daily.json"
        );

        dailyTracker.setDailyResetTime(cfg.dailyResetTime);
        StatsCore.registerDailyTracker(dailyTracker);

        DeathRunCommand.register(server.getCommands().getDispatcher());
        StatsCore.registerLeaderboardCommand("/deaths", "Shows the deaths leaderboard");
        LOGGER.info("DeathLeaderboard initialized");
    }

    public static DailyStatsTracker getDailyTracker() {
        return dailyTracker;
    }
}
