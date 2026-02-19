package net.craftmaster08.cm08statscore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.craftmaster08.cm08statscore.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
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
                                        .executes(context -> blacklistAdd(context, StringArgumentType.getString(context, "player")))))
                        .then(Commands.literal("list")
                                .executes(StatsConfigCommand::blacklistList))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(blacklistedPlayers())
                                        .executes(context -> blacklistRemove(context, StringArgumentType.getString(context, "player"))))))
                .then(Commands.literal("color")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(onlinePlayers())
                                .executes(context -> colorShow(context, StringArgumentType.getString(context, "player")))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("color", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(MINECRAFT_COLORS, builder))
                                                .executes(context -> colorSet(context, StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "color")))))
                                .then(Commands.literal("reset")
                                        .executes(context -> colorReset(context, StringArgumentType.getString(context, "player"))))))
                .then(Commands.literal("dailyresettime")
                        .executes(StatsConfigCommand::dailyResetTimeShow)
                        .then(Commands.argument("time", StringArgumentType.greedyString())
                                .executes(context -> dailyResetTimeSet(context, StringArgumentType.getString(context, "time")))))
                .then(Commands.literal("intlimit")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(onlinePlayers())
                                .executes(context -> displayIntLimitAmount(context, StringArgumentType.getString(context, "player")))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> setIntLimitAmount(context, StringArgumentType.getString(context, "player"), IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("cooldown")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 300))
                                .executes(context -> setCooldown(context, IntegerArgumentType.getInteger(context, "seconds"))))
                        .executes(StatsConfigCommand::showCooldown)
                );
        try {
            dispatcher.register(command);
            LOGGER.info("Successfully registered /statsconfig command");
        } catch (Exception e) {
            LOGGER.error("Failed to register /statsconfig command", e);
        }
    }

    private static SuggestionProvider<CommandSourceStack> onlinePlayers() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            return SharedSuggestionProvider.suggest(
                    server.getPlayerList().getPlayers().stream()
                            .map(player -> player.getGameProfile().getName()),
                    builder
            );
        };
    }

    private static SuggestionProvider<CommandSourceStack> blacklistedPlayers() {
        return (context, builder) -> {
            ConfigManager config = StatsCore.getConfigManager();
            if (config != null) {
                return SharedSuggestionProvider.suggest(config.getBlacklistedPlayers(), builder);
            }
            return builder.buildFuture();
        };
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager configManager = StatsCore.getConfigManager();
            if (configManager == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            configManager.loadConfig();
            source.sendSystemMessage(Component.literal("Successfully reloaded statscore_config.json")
                    .withStyle(ChatFormatting.GREEN));
            LOGGER.info("StatsCore configuration reloaded");
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to reload statscore_config.json: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to reload configuration", e);
            return 0;
        }
    }

    private static int blacklistAdd(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            Set<String> blacklistedPlayers = new HashSet<>(config.getBlacklistedPlayers());
            if (blacklistedPlayers.add(player)) {
                config.blacklistedPlayers = Set.copyOf(blacklistedPlayers);
                config.saveConfig();
                source.sendSystemMessage(Component.literal("Added " + player + " to blacklist")
                        .withStyle(ChatFormatting.GREEN));
                LOGGER.info("Added {} to blacklist", player);
                return 1;
            } else {
                source.sendSystemMessage(Component.literal(player + " is already blacklisted")
                        .withStyle(ChatFormatting.YELLOW));
                return 0;
            }
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to add " + player + " to blacklist: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to add {} to blacklist", player, e);
            return 0;
        }
    }

    private static int blacklistList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            Set<String> blacklistedPlayers = config.getBlacklistedPlayers();
            if (blacklistedPlayers.isEmpty()) {
                source.sendSystemMessage(Component.literal("Blacklist is empty")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                String players = String.join(", ", blacklistedPlayers);
                source.sendSystemMessage(Component.literal("Blacklisted players: " + players)
                        .withStyle(ChatFormatting.WHITE));
            }
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to list blacklist: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to list blacklist", e);
            return 0;
        }
    }

    private static int blacklistRemove(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            Set<String> blacklistedPlayers = new HashSet<>(config.getBlacklistedPlayers());
            if (blacklistedPlayers.remove(player)) {
                config.blacklistedPlayers = Set.copyOf(blacklistedPlayers);
                config.saveConfig();
                source.sendSystemMessage(Component.literal("Removed " + player + " from blacklist")
                        .withStyle(ChatFormatting.GREEN));
                LOGGER.info("Removed {} from blacklist", player);
                return 1;
            } else {
                source.sendSystemMessage(Component.literal(player + " is not blacklisted")
                        .withStyle(ChatFormatting.YELLOW));
                return 0;
            }
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to remove " + player + " from blacklist: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to remove {} from blacklist", player, e);
            return 0;
        }
    }

    private static int colorShow(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            ChatFormatting color = config.getUsernameColors().getOrDefault(player, ChatFormatting.WHITE);
            source.sendSystemMessage(Component.literal(player + "'s color: " + color.getName().toUpperCase())
                    .withStyle(color));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to show color for " + player + ": " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to show color for {}", player, e);
            return 0;
        }
    }

    private static int colorSet(CommandContext<CommandSourceStack> context, String player, String colorName) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            ChatFormatting color = ChatFormatting.getByName(colorName.toUpperCase());
            if (color == null || !color.isColor()) {
                source.sendSystemMessage(Component.literal("Invalid color: " + colorName)
                        .withStyle(ChatFormatting.RED));
                return 0;
            }
            Map<String, ChatFormatting> usernameColors = new HashMap<>(config.getUsernameColors());
            usernameColors.put(player, color);
            config.usernameColors = Map.copyOf(usernameColors);
            config.saveConfig();
            source.sendSystemMessage(Component.literal("Set " + player + "'s color to " + color.getName().toUpperCase())
                    .withStyle(color));
            LOGGER.info("Set {}'s color to {}", player, color.getName());
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to set color for " + player + ": " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to set color for {} to {}", player, colorName, e);
            return 0;
        }
    }

    private static int colorReset(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            Map<String, ChatFormatting> usernameColors = new HashMap<>(config.getUsernameColors());
            if (usernameColors.remove(player) != null) {
                config.usernameColors = Map.copyOf(usernameColors);
                config.saveConfig();
                source.sendSystemMessage(Component.literal("Reset " + player + "'s color to WHITE")
                        .withStyle(ChatFormatting.WHITE));
                LOGGER.info("Reset {}'s color", player);
                return 1;
            } else {
                source.sendSystemMessage(Component.literal(player + "'s color is already default")
                        .withStyle(ChatFormatting.YELLOW));
                return 0;
            }
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to reset color for " + player + ": " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to reset color for {}", player, e);
            return 0;
        }
    }

    private static int dailyResetTimeShow(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            source.sendSystemMessage(Component.literal("Daily reset time: " + config.dailyResetTime)
                    .withStyle(ChatFormatting.WHITE));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to show daily reset time: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to show daily reset time", e);
            return 0;
        }
    }

    private static int dailyResetTimeSet(CommandContext<CommandSourceStack> context, String time) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }

            // Validation
            String cleaned = time.trim().replaceAll("\\s+", " ");
            if (!cleaned.matches("^\\d{2}:\\d{2}:\\d{2}$") &&
                    !cleaned.matches("^\\d{2}:\\d{2}:\\d{2} ?UTC$")) {
                throw new IllegalArgumentException("Invalid format. Use HH:mm:ss or HH:mm:ss UTC (24-hour)");
            }

            // Normalize
            String fullTime = cleaned.endsWith("UTC") || cleaned.endsWith("utc")
                    ? cleaned.replaceAll("(?i)utc$", "UTC").trim()
                    : cleaned + " UTC";

            // range check
            String[] parts = cleaned.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int s = Integer.parseInt(parts[2]);
            if (h > 23 || m > 59 || s > 59) {
                throw new IllegalArgumentException("Time values out of range (HH 0-23, mm/ss 0-59)");
            }

            config.dailyResetTime = fullTime;
            config.saveConfig();

            source.sendSystemMessage(Component.literal("Daily reset time set to " + fullTime)
                    .withStyle(ChatFormatting.GREEN));
            LOGGER.info("Set daily reset time to {}", fullTime);
            return 1;

        } catch (IllegalArgumentException e) {
            source.sendSystemMessage(Component.literal("Error: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return 0;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to set time: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to set daily reset time", e);
            return 0;
        }
    }

    private static int displayIntLimitAmount(CommandContext<CommandSourceStack> context, String player) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            int limitCount = config.getIntLimits().getOrDefault(player, 0);
            source.sendSystemMessage(Component.literal(player + "'s intlimit count: " + limitCount)
                    .withStyle(ChatFormatting.WHITE));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to show intlimit count for " + player + ": " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to show intlimit count for {}", player, e);
            return 0;
        }
    }

    private static int setIntLimitAmount(CommandContext<CommandSourceStack> context, String player, int amount) {
        CommandSourceStack source = context.getSource();
        try {
            ConfigManager config = StatsCore.getConfigManager();
            if (config == null) {
                throw new IllegalStateException("ConfigManager not initialized");
            }
            Map<String, Integer> intLimits = new HashMap<>(config.getIntLimits());
            intLimits.put(player, amount);
            config.intLimits = Map.copyOf(intLimits);
            config.saveConfig();
            source.sendSystemMessage(Component.literal("Set " + player + "'s intlimit count to " + amount)
                    .withStyle(ChatFormatting.GREEN));
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
            if (config == null) throw new IllegalStateException("ConfigManager not initialized");

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
}