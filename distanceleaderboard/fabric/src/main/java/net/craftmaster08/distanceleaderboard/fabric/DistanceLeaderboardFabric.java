package net.craftmaster08.distanceleaderboard.fabric;

import net.craftmaster08.distanceleaderboard.DistanceLeaderboard;
import net.fabricmc.api.ModInitializer;

public class DistanceLeaderboardFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DistanceLeaderboard.init();
    }
}
