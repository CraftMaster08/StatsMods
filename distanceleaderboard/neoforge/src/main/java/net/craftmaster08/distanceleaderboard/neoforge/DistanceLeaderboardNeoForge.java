package net.craftmaster08.distanceleaderboard.neoforge;

import net.craftmaster08.distanceleaderboard.DistanceLeaderboard;
import net.neoforged.fml.common.Mod;

@Mod(DistanceLeaderboard.MODID)
public class DistanceLeaderboardNeoForge {
    public DistanceLeaderboardNeoForge() {
        DistanceLeaderboard.init();
    }
}
