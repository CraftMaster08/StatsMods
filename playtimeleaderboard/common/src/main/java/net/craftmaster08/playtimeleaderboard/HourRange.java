package net.craftmaster08.playtimeleaderboard;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public enum HourRange {
    UNDER_100(0, 100, new ChatFormatting[]{ChatFormatting.GRAY}, ChatFormatting.GRAY, null, null),
    H_100_199(100, 200, new ChatFormatting[]{ChatFormatting.WHITE}, ChatFormatting.WHITE, null, null),
    H_200_299(200, 300, new ChatFormatting[]{ChatFormatting.GOLD}, ChatFormatting.GOLD, null, null),
    H_300_399(300, 400, new ChatFormatting[]{ChatFormatting.AQUA}, ChatFormatting.AQUA, null, null),
    H_400_499(400, 500, new ChatFormatting[]{ChatFormatting.DARK_GREEN}, ChatFormatting.DARK_GREEN, null, null),
    H_500_599(500, 600, new ChatFormatting[]{ChatFormatting.DARK_AQUA}, ChatFormatting.DARK_AQUA, null, null),
    H_600_699(600, 700, new ChatFormatting[]{ChatFormatting.DARK_RED}, ChatFormatting.DARK_RED, null, null),
    H_700_799(700, 800, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, ChatFormatting.LIGHT_PURPLE, null, null),
    H_800_899(800, 900, new ChatFormatting[]{ChatFormatting.BLUE}, ChatFormatting.BLUE, null, null),
    H_900_999(900, 1000, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, ChatFormatting.DARK_PURPLE, null, null),

    H_1000_1099(1000, 1100, new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.AQUA}, ChatFormatting.LIGHT_PURPLE, "✫", ChatFormatting.RED),
    H_1100_1199(1100, 1200, new ChatFormatting[]{ChatFormatting.WHITE}, ChatFormatting.GRAY, "✪", ChatFormatting.GRAY),
    H_1200_1299(1200, 1300, new ChatFormatting[]{ChatFormatting.YELLOW}, ChatFormatting.GRAY, "✪", ChatFormatting.GOLD),
    H_1300_1399(1300, 1400, new ChatFormatting[]{ChatFormatting.AQUA}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_AQUA),
    H_1400_1499(1400, 1500, new ChatFormatting[]{ChatFormatting.GREEN}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_GREEN),
    H_1500_1599(1500, 1600, new ChatFormatting[]{ChatFormatting.DARK_AQUA}, ChatFormatting.GRAY, "✪", ChatFormatting.BLUE),
    H_1600_1699(1600, 1700, new ChatFormatting[]{ChatFormatting.RED}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_RED),
    H_1700_1799(1700, 1800, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_PURPLE),
    H_1800_1899(1800, 1900, new ChatFormatting[]{ChatFormatting.BLUE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_BLUE),
    H_1900_1999(1900, 2000, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, ChatFormatting.GRAY, "✪", ChatFormatting.DARK_GRAY),

    H_2000_2099(2000, 2100, new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.GRAY}, ChatFormatting.DARK_GRAY, "⚝", ChatFormatting.GRAY),
    H_2100_2199(2100, 2200, new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.YELLOW, ChatFormatting.YELLOW, ChatFormatting.GOLD}, ChatFormatting.GOLD, "⚝", ChatFormatting.GRAY),
    H_2200_2299(2200, 2300, new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.DARK_AQUA}, ChatFormatting.DARK_AQUA, "⚝", ChatFormatting.GOLD),
    H_2300_2399(2300, 2400, new ChatFormatting[]{ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD}, ChatFormatting.YELLOW, "⚝", ChatFormatting.DARK_PURPLE),
    H_2400_2499(2400, 2500, new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY}, ChatFormatting.DARK_GRAY, "⚝", ChatFormatting.AQUA),
    H_2500_2599(2500, 2600, new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.GREEN, ChatFormatting.GREEN, ChatFormatting.DARK_GREEN}, ChatFormatting.DARK_GREEN, "⚝", ChatFormatting.GRAY),
    H_2600_2699(2600, 2700, new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.RED, ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE}, ChatFormatting.DARK_PURPLE, "⚝", ChatFormatting.DARK_RED),
    H_2700_2799(2700, 2800, new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY}, ChatFormatting.DARK_GRAY, "⚝", ChatFormatting.GOLD),
    H_2800_2899(2800, 2900, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_GREEN, ChatFormatting.YELLOW}, ChatFormatting.GOLD, "⚝", ChatFormatting.GREEN),
    H_2900_2999(2900, 3000, new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.DARK_AQUA, ChatFormatting.DARK_AQUA, ChatFormatting.DARK_BLUE}, ChatFormatting.DARK_BLUE, "⚝", ChatFormatting.AQUA),

    H_3000_3099(3000, 3100, new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.GOLD, ChatFormatting.GOLD, ChatFormatting.RED}, ChatFormatting.DARK_RED, "✥", ChatFormatting.YELLOW),
    H_3100_3199(3100, 3200, new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.DARK_AQUA, ChatFormatting.DARK_AQUA, ChatFormatting.GOLD}, ChatFormatting.YELLOW, "✥", ChatFormatting.BLUE),
    H_3200_3299(3200, 3300, new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.DARK_RED}, ChatFormatting.RED, "✥", ChatFormatting.RED),
    H_3300_3399(3300, 3400, new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.RED}, ChatFormatting.DARK_RED, "✥", ChatFormatting.BLUE),
    H_3400_3499(3400, 3500, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.LIGHT_PURPLE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE}, ChatFormatting.DARK_GREEN, "✥", ChatFormatting.DARK_GREEN),
    H_3500_3599(3500, 3600, new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.DARK_RED, ChatFormatting.DARK_RED, ChatFormatting.DARK_GREEN}, ChatFormatting.GREEN, "✥", ChatFormatting.RED),
    H_3600_3699(3600, 3700, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.BLUE}, ChatFormatting.DARK_BLUE, "✥", ChatFormatting.GREEN),
    H_3700_3799(3700, 3800, new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.RED, ChatFormatting.RED, ChatFormatting.AQUA}, ChatFormatting.DARK_AQUA, "✥", ChatFormatting.DARK_RED),
    H_3800_3899(3800, 3900, new ChatFormatting[]{ChatFormatting.DARK_BLUE, ChatFormatting.BLUE, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_PURPLE}, ChatFormatting.LIGHT_PURPLE, "✥", ChatFormatting.DARK_BLUE),
    H_3900_3999(3900, 4000, new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.GREEN, ChatFormatting.GREEN, ChatFormatting.DARK_AQUA}, ChatFormatting.BLUE, "✥", ChatFormatting.RED),

    H_4000_4099(4000, 4100, new ChatFormatting[]{ChatFormatting.DARK_PURPLE, ChatFormatting.RED, ChatFormatting.RED, ChatFormatting.GOLD}, ChatFormatting.YELLOW, "✮", ChatFormatting.DARK_PURPLE),
    H_4100_4199(4100, 4200, new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.GOLD, ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE}, ChatFormatting.DARK_PURPLE, "✮", ChatFormatting.YELLOW),
    H_4200_4299(4200, 4300, new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.DARK_AQUA, ChatFormatting.AQUA, ChatFormatting.WHITE}, ChatFormatting.GRAY, "✮", ChatFormatting.BLUE),
    H_4300_4399(4300, 4400, new ChatFormatting[]{ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_GRAY, ChatFormatting.DARK_GRAY, ChatFormatting.DARK_PURPLE}, ChatFormatting.BLACK, "✮", ChatFormatting.BLACK),
    H_4400_4499(4400, 4500, new ChatFormatting[]{ChatFormatting.DARK_GREEN, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.GOLD}, ChatFormatting.DARK_PURPLE, "✮", ChatFormatting.DARK_GREEN),
    H_4500_4599(4500, 4600, new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.AQUA, ChatFormatting.AQUA, ChatFormatting.DARK_AQUA}, ChatFormatting.DARK_AQUA, "✮", ChatFormatting.WHITE),
    H_4600_4699(4600, 4700, new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.YELLOW, ChatFormatting.GOLD}, ChatFormatting.BLACK, "✮", ChatFormatting.DARK_AQUA),
    H_4700_4799(4700, 4800, new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.RED, ChatFormatting.RED, ChatFormatting.BLUE}, ChatFormatting.DARK_BLUE, "✮", ChatFormatting.WHITE),
    H_4800_4899(4800, 4900, new ChatFormatting[]{ChatFormatting.DARK_PURPLE, ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW}, ChatFormatting.DARK_AQUA, "✮", ChatFormatting.DARK_PURPLE),
    H_4900_4999(4900, 5000, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.WHITE, ChatFormatting.WHITE, ChatFormatting.GREEN}, ChatFormatting.GREEN, "✮", ChatFormatting.DARK_GREEN),

    H_5000_PLUS(5000, Double.MAX_VALUE, new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE, ChatFormatting.BLUE, ChatFormatting.BLUE}, ChatFormatting.DARK_BLUE, "✦", ChatFormatting.DARK_RED);

    private final double minHours;
    private final double maxHours;
    private final ChatFormatting[] hoursColors;
    private final ChatFormatting hColor;
    private final String starSymbol;
    private final ChatFormatting starColor;

    HourRange(double minHours, double maxHours, ChatFormatting[] hoursColors, ChatFormatting hColor, String starSymbol, ChatFormatting starColor) {
        this.minHours = minHours;
        this.maxHours = maxHours;
        this.hoursColors = hoursColors;
        this.hColor = hColor;
        this.starSymbol = starSymbol;
        this.starColor = starColor;
    }

    public static HourRange findRange(double playtime) {
        for (HourRange range : values()) {
            if (playtime >= range.minHours && playtime < range.maxHours) {
                return range;
            }
        }
        return H_5000_PLUS;
    }

    public MutableComponent formatHours(double playtime) {
        String hoursText = playtime >= 1000.0 ? String.format("%d", (int) playtime) : String.format("%.2f", playtime);
        MutableComponent component = Component.literal("");

        if (starSymbol != null) {
            component.append(Component.literal(starSymbol + " ")
                    .withStyle(Style.EMPTY.withColor(starColor).withBold(false)));
        }

        if (hoursColors.length > 1) {
            String[] chars = hoursText.split("");
            for (int i = 0; i < chars.length; i++) {
                ChatFormatting color = i < hoursColors.length ? hoursColors[i] : ChatFormatting.WHITE;
                component.append(Component.literal(chars[i])
                        .withStyle(Style.EMPTY.withColor(color).withBold(false)));
            }
        } else {
            component.append(Component.literal(hoursText)
                    .withStyle(Style.EMPTY.withColor(hoursColors[0]).withBold(false)));
        }

        component.append(Component.literal("h")
                .withStyle(Style.EMPTY.withColor(hColor).withBold(false)));

        return component;
    }
}
