package com.roomruster;

import com.roomruster.core.RotationEngine;
import com.roomruster.core.WeekSchedule;
import com.roomruster.notify.DiscordNotifier;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        // Defaults
        LocalDate start = getStartDateFromArgsOrEnv(args);
        RotationEngine engine = new RotationEngine();

        switch (args[0]) {
            case "--print" -> {
                if (args.length < 2) {
                    System.err.println("Missing N for --print N");
                    System.exit(1);
                }
                int n = Integer.parseInt(args[1]);
                printWeeks(engine, start, n);
            }
            case "--send" -> {
                sendThisWeek(engine, start);
            }
            case "--daemon" -> {
                LocalTime at = getTimeFromArgs(args, LocalTime.of(8, 0));
                runDaemon(engine, start, at);
            }
            case "--daemon-sim" -> {
                int interval = getIntervalSecondsFromArgs(args, 5);
                int startWeek = getStartWeekFromArgs(args, computeCurrentWeekIndex(start));
                runDaemonSim(engine, start, startWeek, interval);
            }
            default -> printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Room-Ruster CLI\n" +
                "Usage:\n" +
                "  java -jar room-ruster.jar --print N [--start YYYY-MM-DD]\n" +
                "  java -jar room-ruster.jar --send [--start YYYY-MM-DD]\n" +
                "  java -jar room-ruster.jar --daemon [--start YYYY-MM-DD] [--time HH:mm]\n" +
                "  java -jar room-ruster.jar --daemon-sim [--start YYYY-MM-DD] [--start-week N] [--interval-seconds N]\n" +
                "Notes:\n" +
                "  --start sets the anchor Monday for week 1. Default from env START_DATE or next upcoming Monday.\n" +
                "  --send posts the current week's schedule to the Discord webhook in DISCORD_WEBHOOK_URL.\n" +
                "  --daemon runs a weekly poster every Monday at --time (default 08:00) to DISCORD_WEBHOOK_URL.\n" +
                "  --daemon-sim posts on a fixed interval (default 5s) and advances week index each tick (demo mode).\n" +
                "     Use --interval-seconds to control pace and --start-week to set initial week index.");
    }

    private static LocalDate getStartDateFromArgsOrEnv(String[] args) {
        LocalDate start = null;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--start".equals(args[i])) {
                start = LocalDate.parse(args[i+1], DATE_FMT);
                break;
            }
        }
        if (start == null) {
            String env = System.getenv("START_DATE");
            if (env != null && !env.isBlank()) {
                start = LocalDate.parse(env.trim(), DATE_FMT);
            }
        }
        if (start == null) {
            // Default: next Monday from today (or today if Monday)
            LocalDate today = LocalDate.now();
            DayOfWeek dow = today.getDayOfWeek();
            int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - dow.getValue() + 7) % 7;
            start = today.plusDays(daysUntilMonday);
        }
        // Ensure start is a Monday for clarity
        if (start.getDayOfWeek() != DayOfWeek.MONDAY) {
            start = start.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        return start;
    }

    private static void printWeeks(RotationEngine engine, LocalDate start, int n) {
        for (int i = 0; i < n; i++) {
            int weekIndex = i + 1;
            LocalDate weekStart = start.plusWeeks(i);
            WeekSchedule schedule = engine.getWeekSchedule(weekIndex);
            System.out.println(formatWeeklySchedule(weekIndex, weekStart, schedule));
            System.out.println();
        }
    }

    private static void sendThisWeek(RotationEngine engine, LocalDate start) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        if (webhook == null || webhook.isBlank()) {
            System.err.println("DISCORD_WEBHOOK_URL env var not set. Create a Discord channel webhook and set the URL.");
            System.exit(2);
        }
        LocalDate today = LocalDate.now();
        int weekIndex = computeCurrentWeekIndex(start);
        LocalDate weekStart = start.plusWeeks(weekIndex - 1);
        sendFor(engine, webhook, weekIndex, weekStart);
    }

    private static String formatWeeklySchedule(int weekIndex, LocalDate weekStart, WeekSchedule sc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Room-Ruster — Week ").append(weekIndex)
          .append(" (starting ").append(weekStart.format(DATE_FMT)).append(")\n");
        sb.append("Dish Washing (2): ").append(String.join(", ", sc.dishWashers())).append("\n");
        sb.append("Room Care (1): ").append(sc.roomCare()).append("\n");
        sb.append("Shoe Washing (3): ").append(String.join(", ", sc.shoeWashers())).append("\n");
        sb.append("Free ("+sc.freePeople().size()+"): ").append(String.join(", ", sc.freePeople())).append("\n");
        return sb.toString();
    }

    private static void runDaemon(RotationEngine engine, LocalDate start, LocalTime time) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        if (webhook == null || webhook.isBlank()) {
            System.err.println("DISCORD_WEBHOOK_URL env var not set. Create a Discord channel webhook and set the URL.");
            System.exit(2);
        }
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        long initialDelay = computeInitialDelayToNextMonday(time);
        long period = TimeUnit.DAYS.toSeconds(7);
        System.out.println("Daemon started. First post in " + initialDelay + " seconds; then every 7 days at " + time + ".");
        ses.scheduleAtFixedRate(() -> {
            try {
                LocalDate monday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                int weekIndex = (int) (ChronoUnit.WEEKS.between(start, monday) + 1);
                LocalDate weekStart = start.plusWeeks(weekIndex - 1);
                sendFor(engine, webhook, weekIndex, weekStart);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, initialDelay, period, TimeUnit.SECONDS);
    }

    private static long computeInitialDelayToNextMonday(LocalTime at) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                .withHour(at.getHour()).withMinute(at.getMinute()).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusWeeks(1);
        }
        return Duration.between(now, next).getSeconds();
    }

    private static void sendFor(RotationEngine engine, String webhook, int weekIndex, LocalDate weekStart) throws Exception {
        WeekSchedule schedule = engine.getWeekSchedule(weekIndex);
        String content = formatWeeklySchedule(weekIndex, weekStart, schedule);
        DiscordNotifier notifier = new DiscordNotifier(webhook);
        notifier.send(content);
        System.out.println("Sent schedule for week " + weekIndex + " (starting " + weekStart + ") to Discord.");
    }

    private static LocalTime getTimeFromArgs(String[] args, LocalTime def) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--time".equals(args[i])) {
                return LocalTime.parse(args[i+1]);
            }
        }
        String env = System.getenv("POST_TIME");
        if (env != null && !env.isBlank()) {
            return LocalTime.parse(env.trim());
        }
        return def;
    }

    private static int getIntervalSecondsFromArgs(String[] args, int def) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--interval-seconds".equals(args[i])) {
                return Integer.parseInt(args[i+1]);
            }
        }
        String env = System.getenv("INTERVAL_SECONDS");
        if (env != null && !env.isBlank()) {
            return Integer.parseInt(env.trim());
        }
        return def;
    }

    private static int getStartWeekFromArgs(String[] args, int def) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--start-week".equals(args[i])) {
                return Integer.parseInt(args[i+1]);
            }
        }
        String env = System.getenv("START_WEEK");
        if (env != null && !env.isBlank()) {
            return Integer.parseInt(env.trim());
        }
        return def;
    }

    private static int computeCurrentWeekIndex(LocalDate start) {
        LocalDate today = LocalDate.now();
        return (int) (ChronoUnit.WEEKS.between(start, today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) + 1);
    }

    private static void runDaemonSim(RotationEngine engine, LocalDate start, int startWeekIndex, int intervalSeconds) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        if (webhook == null || webhook.isBlank()) {
            System.out.println("[SIM] DISCORD_WEBHOOK_URL not set; will print schedules to stdout only.");
        }
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        System.out.println("Simulation daemon started. Interval: " + intervalSeconds + "s. Starting at week " + startWeekIndex + ".");
        final int[] counter = { startWeekIndex };
        ses.scheduleAtFixedRate(() -> {
            try {
                int weekIndex = counter[0]++;
                LocalDate weekStart = start.plusWeeks(weekIndex - 1);
                WeekSchedule sc = engine.getWeekSchedule(weekIndex);
                String content = formatWeeklySchedule(weekIndex, weekStart, sc);
                System.out.println(content);
                if (webhook != null && !webhook.isBlank()) {
                    new DiscordNotifier(webhook).send(content);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }
}
