package net.craftmaster08.deathleaderboard.forge;

import net.craftmaster08.deathleaderboard.DeathLeaderboard;
import net.minecraftforge.fml.common.Mod;

@Mod(DeathLeaderboard.MODID)
public class DeathLeaderboardForge {
    public DeathLeaderboardForge() {
        DeathLeaderboard.init();
    }
}
