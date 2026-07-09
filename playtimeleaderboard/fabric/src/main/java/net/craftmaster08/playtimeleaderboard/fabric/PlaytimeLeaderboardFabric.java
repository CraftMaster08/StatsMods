package net.craftmaster08.playtimeleaderboard.fabric;

import net.craftmaster08.playtimeleaderboard.PlaytimeLeaderboard;
import net.fabricmc.api.ModInitializer;

public class PlaytimeLeaderboardFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlaytimeLeaderboard.init();
    }
}
