package net.craftmaster08.killleaderboard.fabric;

import net.craftmaster08.killleaderboard.KillLeaderboard;
import net.fabricmc.api.ModInitializer;

public class KillLeaderboardFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KillLeaderboard.init();
    }
}
