package net.craftmaster08.deathleaderboard.neoforge;

import net.craftmaster08.deathleaderboard.DeathLeaderboard;
import net.neoforged.fml.common.Mod;

@Mod(DeathLeaderboard.MODID)
public class DeathLeaderboardNeoForge {
    public DeathLeaderboardNeoForge() {
        DeathLeaderboard.init();
    }
}
