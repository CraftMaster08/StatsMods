package net.craftmaster08.playtimeleaderboard;

import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.craftmaster08.cm08statscore.statstracker.DailyStatsTracker;
import net.minecraft.stats.Stats;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod(PlaytimeLeaderboard.MODID)
public class PlaytimeLeaderboard {
    public static final String MODID = "playtimeleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(PlaytimeLeaderboard.class);

    private static DailyStatsTracker dailyTracker;

    public PlaytimeLeaderboard() {
        if (!ModList.get().isLoaded(StatsCore.MODID)) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling PlaytimeLeaderboard.");
            return;
        }
        // LOWEST priority guarantees StatsCore's normal-priority ServerStartingEvent
        // listener (which loads the config) has already run by the time this fires.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
    }

    public void onServerStarting(ServerStartingEvent event) {
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

        PlaytimeRunCommand.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("PlaytimeLeaderboard initialized");
    }

    public static DailyStatsTracker getDailyTracker() {
        return dailyTracker;
    }
}
