package net.craftmaster08.deathleaderboard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeathRunCommand {
    private static final Logger LOGGER = LogManager.getLogger(DeathRunCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("deaths")
                .requires(source -> source.hasPermission(0))
                .executes(context -> new LeaderboardExecutor(context.getSource()).execute());

        try {
            dispatcher.register(command);
            LOGGER.info("Successfully registered /deaths command");
        } catch (Exception e) {
            LOGGER.error("Failed to register /deaths command", e);
        }
    }
}
