package com.roomruster;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.roomruster.core.RotationEngine;
import com.roomruster.core.WeekSchedule;
import com.roomruster.notify.DiscordNotifier;

public class App {
    // WEEK 1 BEGINS ON THIS MONDAY — HARD-CODED FOREVER
    private static final LocalDate WEEK_1_START = LocalDate.of(2025, 12, 1); // Monday
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        RotationEngine engine = new RotationEngine();

        switch (args[0]) {
            case "--print" -> {
                int n = args.length > 1 ? Integer.parseInt(args[1]) : 10;
                printWeeks(engine, n);
            }
            case "--send" -> sendThisWeek(engine);
            case "--daemon" -> {
                DayOfWeek day = getDayOfWeekFromArgs(args, DayOfWeek.SUNDAY);
                LocalTime time = getTimeFromArgs(args, LocalTime.of(18, 0));
                runDaemon(engine, day, time);
            }
            case "--daemon-sim" -> {
                int interval = getIntervalSecondsFromArgs(args, 10);
                runDaemonSim(engine, interval);
            }
            default -> printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("""
                Room-Ruster CLI
                Usage:
                  --print [N]          → print next N weeks (default 10)
                  --send               → send the upcoming week's roster to Discord
                  --daemon             → run weekly at Sunday 18:00 (customizable)
                  --daemon-sim [sec]   → simulate posting every N seconds
                """);
    }

    // ===================================================================
    // CORRECT WEEK INDEX CALCULATION — THIS IS THE FIX
    // ===================================================================
    private static int calculateCurrentWeekIndex() {
        LocalDate today = LocalDate.now(); // uses system default timezone
        LocalDate upcomingMonday = getUpcomingMonday(today);

        long weeksSinceWeek1 = ChronoUnit.WEEKS.between(WEEK_1_START, upcomingMonday);
        int weekIndex = (int) weeksSinceWeek1 + 1;

        System.out.printf("[DEBUG] Today: %s | Upcoming Monday: %s | Week Index: %d%n",
                today, upcomingMonday, weekIndex);

        return weekIndex;
    }

    // Always returns the NEXT Monday (even if today is Monday → next week)
    private static LocalDate getUpcomingMonday(LocalDate today) {
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        // If today is Monday, next(DayOfWeek.MONDAY) = next week → perfect
        // If today is Sunday, next Monday = tomorrow → perfect
        return nextMonday;
    }

    // ===================================================================
    // SEND CURRENT WEEK
    // ===================================================================
    private static void sendThisWeek(RotationEngine engine) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        if (webhook == null || webhook.isBlank()) {
            System.err.println("DISCORD_WEBHOOK_URL not set!");
            System.exit(1);
        }

        int weekIndex = calculateCurrentWeekIndex();
        LocalDate weekStart = WEEK_1_START.plusWeeks(weekIndex - 1);

        sendSchedule(engine, webhook, weekIndex, weekStart);
    }

    // ===================================================================
    // DAEMON MODE
    // ===================================================================
    private static void runDaemon(RotationEngine engine, DayOfWeek day, LocalTime time) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        if (webhook == null || webhook.isBlank()) {
            System.err.println("DISCORD_WEBHOOK_URL not set!");
            System.exit(1);
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        long initialDelay = computeInitialDelay(day, time);
        long period = 7 * 24 * 60 * 60; // 7 days in seconds

        System.out.printf("Daemon started. First run in %d seconds, then every week on %s at %s%n",
                initialDelay, day, time);

        executor.scheduleAtFixedRate(() -> {
            try {
                int weekIndex = calculateCurrentWeekIndex();
                LocalDate weekStart = WEEK_1_START.plusWeeks(weekIndex - 1);
                sendSchedule(engine, webhook, weekIndex, weekStart);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, initialDelay, period, TimeUnit.SECONDS);
    }

    private static long computeInitialDelay(DayOfWeek day, LocalTime time) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime next = now.with(TemporalAdjusters.nextOrSame(day))
                .withHour(time.getHour())
                .withMinute(time.getMinute())
                .withSecond(0)
                .withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusWeeks(1);
        }
        return Duration.between(now, next).getSeconds();
    }

    // ===================================================================
    // SIMULATION MODE
    // ===================================================================
    private static void runDaemonSim(RotationEngine engine, int intervalSeconds) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        final int[] week = { calculateCurrentWeekIndex() };

        exec.scheduleAtFixedRate(() -> {
            try {
                int weekIndex = week[0]++;
                LocalDate weekStart = WEEK_1_START.plusWeeks(weekIndex - 1);
                String message = formatMessage(weekIndex, weekStart, engine.getWeekSchedule(weekIndex));
                System.out.println("\n=== SIM WEEK " + weekIndex + " ===\n" + message);
                if (webhook != null && !webhook.isBlank()) {
                    new DiscordNotifier(webhook).send(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    // ===================================================================
    // PRINT WEEKS
    // ===================================================================
    private static void printWeeks(RotationEngine engine, int count) {
        for (int i = 0; i < count; i++) {
            int weekIndex = i + calculateCurrentWeekIndex();
            LocalDate weekStart = WEEK_1_START.plusWeeks(weekIndex - 1);
            WeekSchedule sc = engine.getWeekSchedule(weekIndex);
            System.out.println(formatMessage(weekIndex, weekStart, sc));
            System.out.println("─".repeat(50));
        }
    }

    // ===================================================================
    // SHARED: SEND & FORMAT
    // ===================================================================
    private static void sendSchedule(RotationEngine engine, String webhook, int weekIndex, LocalDate weekStart) throws Exception {
        WeekSchedule sc = engine.getWeekSchedule(weekIndex);
        String message = formatMessage(weekIndex, weekStart, sc);
        new DiscordNotifier(webhook).send(message);
        System.out.println("Successfully sent Week " + weekIndex + " (" + weekStart + ")");
    }

    private static String formatMessage(int weekIndex, LocalDate weekStart, WeekSchedule sc) {
        return "@everyone\n\n" +
               "**Room-Ruster — Week " + weekIndex + " (starting " + weekStart.format(DATE_FMT) + ")**\n" +
               "Dish Washing (2): " + String.join(", ", sc.dishWashers()) + "\n" +
               "Room Care (1): " + sc.roomCare() + "\n" +
               "Shoe Washing (3): " + String.join(", ", sc.shoeWashers()) + "\n" +
               "Free (" + sc.freePeople().size() + "): " + String.join(", ", sc.freePeople());
    }

    // ===================================================================
    // ARGUMENT HELPERS (unchanged)
    // ===================================================================
    private static DayOfWeek getDayOfWeekFromArgs(String[] args, DayOfWeek def) {
        for (int i = 1; i < args.length - 1; i++) {
            if ("--day".equals(args[i])) return DayOfWeek.valueOf(args[i + 1].toUpperCase());
        }
        return def;
    }

    private static LocalTime getTimeFromArgs(String[] args, LocalTime def) {
        for (int i = 1; i < args.length - 1; i++) {
            if ("--time".equals(args[i])) return LocalTime.parse(args[i + 1]);
        }
        return def;
    }

    private static int getIntervalSecondsFromArgs(String[] args, int def) {
        for (int i = 1; i < args.length - 1; i++) {
            if ("--interval-seconds".equals(args[i])) return Integer.parseInt(args[i+1]);
        }
        return def;
    }
}