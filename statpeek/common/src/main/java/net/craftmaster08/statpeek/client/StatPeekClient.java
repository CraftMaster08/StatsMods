package net.craftmaster08.statpeek.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Shared client-side logic invoked from each loader's command/interaction wiring. */
public final class StatPeekClient {

    private StatPeekClient() {}

    public static boolean openScreenForName(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Player target = mc.level.players().stream()
                .filter(p -> p.getName().getString().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (target == null) return false;
        mc.setScreen(new StatsScreen(target));
        return true;
    }

    public static void openScreenFor(Player target) {
        Minecraft.getInstance().setScreen(new StatsScreen(target));
    }
}
