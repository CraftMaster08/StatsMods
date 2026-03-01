package net.craftmaster08.cm08statscore.statstracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public class ResetScheduler {
    private static final Logger LOGGER = LogManager.getLogger(ResetScheduler.class);
    private LocalTime resetTime;
    private Instant lastResetCheck;
    private final DailyStatsTracker tracker;

    ResetScheduler(DailyStatsTracker tracker) {
        this.tracker = tracker;
        this.lastResetCheck = Instant.now();
    }

    public void setDailyResetTime(String timeStr) {
        try {
            String[] parts = timeStr.split(" ");
            if (parts.length != 2 || !parts[1].equals("UTC")) {
                throw new DateTimeParseException("Invalid format, expected 'HH:mm:ss UTC'", timeStr, 0);
            }
            String fullTime = timeStr.trim();
            if (!fullTime.toUpperCase().endsWith(" UTC")) {
                fullTime += " UTC";
            }
            LOGGER.info("Set daily reset time to: {}", fullTime);
            this.resetTime = LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (DateTimeParseException e) {
            LOGGER.error("Invalid daily_reset_time format: {}. Defaulting to 00:00:00 UTC", timeStr, e);
            this.resetTime = LocalTime.of(0, 0, 0);
        }
    }

    public void checkReset() {
        Instant now = Instant.now();
        ZonedDateTime currentZdt = ZonedDateTime.ofInstant(now, ZoneId.of("UTC"));
        ZonedDateTime lastCheckZdt = ZonedDateTime.ofInstant(lastResetCheck, ZoneId.of("UTC"));
        LocalDate today = currentZdt.toLocalDate();
        ZonedDateTime todayReset = ZonedDateTime.of(today, resetTime, ZoneId.of("UTC"));

        if (currentZdt.isAfter(todayReset) && lastCheckZdt.isBefore(todayReset)) {
            resetStats(currentZdt);
            tracker.saveData();
        }

        if (!currentZdt.toLocalDate().equals(lastCheckZdt.toLocalDate())) {
            ZonedDateTime tomorrowReset = todayReset.plusDays(1);
            if (currentZdt.isAfter(tomorrowReset) && lastCheckZdt.isBefore(tomorrowReset)) {
                resetStats(currentZdt);
                tracker.saveData();
            }
        }
        lastResetCheck = now;
    }

    private void resetStats(ZonedDateTime currentZdt) {
        if (tracker.hasAnyNonZeroDailyStats()) {
            LOGGER.info("Resetting daily stats at {}", currentZdt);
            tracker.resetDailyStats();
        }
    }

    public Instant getLastResetCheck() {
        return lastResetCheck;
    }

    public void setLastResetCheck(Instant resetCheck) {
        lastResetCheck = resetCheck;
    }
}