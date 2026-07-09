package net.craftmaster08.killleaderboard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KillRunCommand {
    private static final Logger LOGGER = LogManager.getLogger(KillRunCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("playerkills")
                .requires(source -> source.hasPermission(0))
                .executes(context -> new LeaderboardExecutor(context.getSource()).execute());

        try {
            dispatcher.register(command);
            LOGGER.info("Successfully registered /playerkills command");
        } catch (Exception e) {
            LOGGER.error("Failed to register /playerkills command", e);
        }
    }
}
