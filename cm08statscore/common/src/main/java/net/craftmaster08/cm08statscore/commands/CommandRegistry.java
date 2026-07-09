package net.craftmaster08.cm08statscore.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandRegistry {
    private static final Logger LOGGER = LogManager.getLogger(CommandRegistry.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        StatsConfigCommand.register(dispatcher);
        LOGGER.info("Registered /statsconfig command");
    }
}
