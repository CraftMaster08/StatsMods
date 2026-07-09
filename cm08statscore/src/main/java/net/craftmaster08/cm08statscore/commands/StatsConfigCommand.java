package net.craftmaster08.cm08statscore.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.craftmaster08.cm08statscore.StatsCore;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StatsConfigCommand {
    private static final Logger LOGGER = LogManager.getLogger(StatsConfigCommand.class);

    private static final List<String> MINECRAFT_COLORS = Arrays.stream(ChatFormatting.values())
            .filter(ChatFormatting::isColor)
            .map(color -> color.getName().toUpperCase())
            .collect(Collectors.toList());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("statsconfig")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(StatsConfigCommand::reloadConfig))
                .then(Commands.literal("blacklist")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(onlinePlayers())
                                        .executes(c -> blacklistAdd(c, StringArgumentType.getString(c, "player")))))
                        .then(Commands.literal("list")
                                .executes(StatsConfigCommand::blacklistList))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(blacklistedPlayers())
                                        .executes(c -> blacklistRemove(c, StringArgumentType.getString(c, "player"))))))
                .then(Commands.literal("color")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(onlinePlayers())
                                .executes(c -> colorShow(c, StringArgumentType.getString(c, "player")))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("color", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MINECRAFT_COLORS, builder))
                                                .executes(c -> colorSet(c, StringArgumentType.getString(c, "player"), StringArgumentType.getString(c, "color")))))
                                .then(Commands.literal("reset")
                                        .executes(c -> colorReset(c, StringArgumentType.getString(c, "player"))))))
                .then(Commands.literal("daily_reset_time")
                        .executes(StatsConfigCommand::dailyResetTimeShow)
                        .then(Commands.literal("set")
                                .then(Commands.argument("time", StringArgumentType.greedyString())
                                        .executes(c -> dailyResetTimeSet(c, StringArgumentType.getString(c, "time"))))))
                .then(Commands.literal("intlimit")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(onlinePlayers())
                                .executes(c -> displayIntLimitAmount(c, StringArgumentType.getString(c, "player")))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(c -> setIntLimitAmount(c, StringArgumentType.getString(c, "player"), IntegerArgumentType.getInteger(c, "amount")))))))
                .then(Commands.literal("cooldown")
                        .executes(StatsConfigCommand::showCooldown)
                        .then(Commands.literal("set")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                        .executes(c -> setCooldown(c, IntegerArgumentType.getInteger(c, "seconds"))))));

        dispatcher.register(command);
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) throw new IllegalStateException("ConfigManager not initialized");
            config.loadConfig();
            StatsCore.updateAllDailyResetTimes();
            source.sendSystemMessage(Component.literal("Reloaded statscore_config.json").withStyle(ChatFormatting.GREEN));
            LOGGER.info("Reloaded statscore_config.json");
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to reload: " + e.getMessage()).withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to reload config", e);
            return 0;
        }
    }

    private static int blacklistAdd(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            config.blacklistedPlayers.add(player);
            config.saveConfig();
            source.sendSystemMessage(Component.literal("Added " + player + " to blacklist").withStyle(ChatFormatting.GREEN));
            LOGGER.info("Added {} to blacklist", player);
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to add to blacklist: " + e.getMessage()).withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to add {} to blacklist", player, e);
            return 0;
        }
    }

    private static int blacklistList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config.blacklistedPlayers.isEmpty()) {
                source.sendSystemMessage(Component.literal("No blacklisted players").withStyle(ChatFormatting.YELLOW));
            } else {
                source.sendSystemMessage(Component.literal("Blacklisted players: " + String.join(", ", config.blacklistedPlayers)).withStyle(ChatFormatting.WHITE));
            }
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int blacklistRemove(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config.blacklistedPlayers.remove(player)) {
                config.saveConfig();
                source.sendSystemMessage(Component.literal("Removed " + player + " from blacklist").withStyle(ChatFormatting.GREEN));
                LOGGER.info("Removed {} from blacklist", player);
            } else {
                source.sendSystemMessage(Component.literal(player + " not in blacklist").withStyle(ChatFormatting.YELLOW));
            }
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to remove from blacklist: " + e.getMessage()).withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to remove {} from blacklist", player, e);
            return 0;
        }
    }

    private static int colorShow(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            ChatFormatting color = config.usernameColors.getOrDefault(player, ChatFormatting.WHITE);
            source.sendSystemMessage(Component.literal(player + "'s color: " + color.getName()).withStyle(color));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int colorSet(CommandContext<CommandSourceStack> context, String player, String colorName) {
        CommandSourceStack source = context.getSource();
        try {
            ChatFormatting color = ChatFormatting.getByName(colorName.toUpperCase());
            if (color == null || !color.isColor()) {
                source.sendSystemMessage(Component.literal("Invalid color: " + colorName).withStyle(ChatFormatting.RED));
                return 0;
            }
            ConfigManager config = StatsCore.getConfigManager();
            config.usernameColors.put(player, color);
            config.saveConfig();
            source.sendSystemMessage(Component.literal("Set " + player + "'s color to " + colorName).withStyle(color));
            LOGGER.info("Set {}'s color to {}", player, colorName);
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int colorReset(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config.usernameColors.remove(player) != null) {
                config.saveConfig();
                source.sendSystemMessage(Component.literal("Reset " + player + "'s color to default").withStyle(ChatFormatting.GREEN));
                LOGGER.info("Reset {}'s color", player);
            } else {
                source.sendSystemMessage(Component.literal(player + " has no custom color").withStyle(ChatFormatting.YELLOW));
            }
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int dailyResetTimeShow(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            source.sendSystemMessage(Component.literal("Current daily reset time: " + config.dailyResetTime)
                    .withStyle(ChatFormatting.WHITE));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int dailyResetTimeSet(CommandContext<CommandSourceStack> context, String timeStr) {
        CommandSourceStack source = context.getSource();
        try {
            if (!timeStr.matches("^\\d{2}:\\d{2}:\\d{2} UTC$")) {
                source.sendSystemMessage(Component.literal("Invalid format. Use 'HH:mm:ss UTC'").withStyle(ChatFormatting.RED));
                return 0;
            }
            ConfigManager config = StatsCore.getConfigManager();
            config.dailyResetTime = timeStr;
            config.saveConfig();
            StatsCore.updateAllDailyResetTimes();
            source.sendSystemMessage(Component.literal("Set daily reset time to " + timeStr).withStyle(ChatFormatting.GREEN));
            LOGGER.info("Set daily reset time to {}", timeStr);
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int displayIntLimitAmount(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            int amount = config.intLimits.getOrDefault(player, 0);
            source.sendSystemMessage(Component.literal(player + "'s intlimit count: " + amount).withStyle(ChatFormatting.WHITE));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int setIntLimitAmount(CommandContext<CommandSourceStack> context, String player, int amount) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            config.intLimits.put(player, amount);
            config.saveConfig();
            source.sendSystemMessage(Component.literal("Set " + player + "'s intlimit count to " + amount).withStyle(ChatFormatting.GREEN));
            LOGGER.info("Set {}'s intlimit count to {}", player, amount);
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to set intlimit count for " + player + ": " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to set intlimit count for {} to {}", player, amount, e);
            return 0;
        }
    }

    private static int setCooldown(CommandContext<CommandSourceStack> context, int seconds) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            config.cooldownSeconds = seconds;
            config.saveConfig();
            source.sendSystemMessage(Component.literal("Leaderboard cooldown set to " + seconds + " seconds")
                    .withStyle(ChatFormatting.GREEN));
            LOGGER.info("Set leaderboard cooldown to {} seconds", seconds);
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int showCooldown(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            int cd = config.cooldownSeconds;
            source.sendSystemMessage(Component.literal("Current leaderboard cooldown: " + cd + " seconds")
                    .withStyle(ChatFormatting.WHITE));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static SuggestionProvider<CommandSourceStack> onlinePlayers() {
        return (ctx, builder) -> SharedSuggestionProvider.suggest(
                ctx.getSource().getOnlinePlayerNames(), builder);
    }

    private static SuggestionProvider<CommandSourceStack> blacklistedPlayers() {
        return (ctx, builder) -> SharedSuggestionProvider.suggest(
                StatsCore.getConfigManager().getBlacklistedPlayers(), builder);
    }
}