package net.craftmaster08.playtimeleaderboard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlaytimeRunCommand {
    private static final Logger LOGGER = LogManager.getLogger(PlaytimeRunCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("playtime")
                .requires(source -> source.hasPermission(0))
                .executes(context -> new LeaderboardExecutor(context.getSource(), LOGGER).execute());

        try {
            dispatcher.register(command);
            LOGGER.info("Successfully registered /playtime command");
        } catch (Exception e) {
            LOGGER.error("Failed to register /playtime command", e);
        }
    }
}