package net.craftmaster08.playtimeleaderboard.forge;

import net.craftmaster08.playtimeleaderboard.PlaytimeLeaderboard;
import net.minecraftforge.fml.common.Mod;

@Mod(PlaytimeLeaderboard.MODID)
public class PlaytimeLeaderboardForge {
    public PlaytimeLeaderboardForge() {
        PlaytimeLeaderboard.init();
    }
}
