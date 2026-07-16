package net.craftmaster08.killleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.platform.Services;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class KillLeaderboard {
    public static final String MODID = "killleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(KillLeaderboard.class);

    private static DailyStatsTracker dailyTracker;

    private KillLeaderboard() {}

    /** Called once by each loader's mod entrypoint. */
    public static void init() {
        if (!Services.PLATFORM.isModLoaded(StatsCore.MODID)) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling KillLeaderboard.");
            return;
        }
        Services.PLATFORM.onServerStartingLate(KillLeaderboard::onServerStarting);
    }

    private static void onServerStarting(MinecraftServer server) {
        ConfigManager cfg = StatsCore.getConfigManager();
        if (cfg == null) {
            LOGGER.error("StatsCore configuration not initialized; KillLeaderboard disabled");
            return;
        }

        var killsStat = Stats.CUSTOM.get(Stats.PLAYER_KILLS);

        dailyTracker = new DailyStatsTracker(
                List.of(killsStat),
                "daily_kills",
                "last_known_kills",
                1.0,
                "kills_daily.json"
        );

        dailyTracker.setDailyResetTime(cfg.dailyResetTime);
        StatsCore.registerDailyTracker(dailyTracker);

        KillRunCommand.register(server.getCommands().getDispatcher());
        StatsCore.registerLeaderboardCommand("/playerkills", "Shows the PVP kills leaderboard");
        LOGGER.info("KillLeaderboard initialized");
    }

    public static DailyStatsTracker getDailyTracker() {
        return dailyTracker;
    }
}
