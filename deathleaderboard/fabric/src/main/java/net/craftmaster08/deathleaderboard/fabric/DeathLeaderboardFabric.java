package net.craftmaster08.deathleaderboard.fabric;

import net.craftmaster08.deathleaderboard.DeathLeaderboard;
import net.fabricmc.api.ModInitializer;

public class DeathLeaderboardFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DeathLeaderboard.init();
    }
}
