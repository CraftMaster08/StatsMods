package net.craftmaster08.playtimeleaderboard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlaytimeRunCommand {
    private static final Logger LOGGER = LogManager.getLogger(PlaytimeRunCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("playtime")
                .requires(s -> s.hasPermission(0))
                .executes(ctx -> new LeaderboardExecutor(ctx.getSource(), 1).execute())
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> new LeaderboardExecutor(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")).execute()));

        dispatcher.register(cmd);
        LOGGER.info("/playtime registered");
    }
}
