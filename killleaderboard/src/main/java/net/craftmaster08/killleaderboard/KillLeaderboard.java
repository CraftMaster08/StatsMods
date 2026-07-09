package net.craftmaster08.killleaderboard;

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

@Mod(KillLeaderboard.MODID)
public class KillLeaderboard {
    public static final String MODID = "killleaderboard";
    private static final Logger LOGGER = LogManager.getLogger(KillLeaderboard.class);

    private static DailyStatsTracker dailyTracker;

    public KillLeaderboard() {
        if (!ModList.get().isLoaded(StatsCore.MODID)) {
            LOGGER.error("StatsCore mod (cm08statscore) is required but not detected. Disabling KillLeaderboard.");
            return;
        }
        // LOWEST priority guarantees StatsCore's normal-priority ServerStartingEvent
        // listener (which loads the config) has already run by the time this fires.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onServerStarting);
    }

    public void onServerStarting(ServerStartingEvent event) {
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

        KillRunCommand.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("KillLeaderboard initialized");
    }

    public static DailyStatsTracker getDailyTracker() {
        return dailyTracker;
    }
}
