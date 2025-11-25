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

    // WEEK 1 STARTS ON MONDAY 1 DECEMBER 2025 — HARD-CODED FOREVER
    private static final LocalDate WEEK_1_START = LocalDate.of(2025, 12, 1);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // UNIQUE MARKER — IF YOU SEE THIS IN DISCORD, IT'S THE NEW CODE
    private static final String VERSION = "FINAL_FIX_2025_NOV_25";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        RotationEngine engine = new RotationEngine();

        switch (args[0]) {
            case "--print" -> printWeeks(engine, args.length > 1 ? Integer.parseInt(args[1]) : 10);
            case "--send" -> sendThisWeek(engine);
            case "--daemon" -> runDaemon(engine);
            case "--daemon-sim" -> runDaemonSim(engine, 10);
            default -> printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Room-Ruster — FINAL VERSION (Nov 25 2025)");
        System.out.println("Usage: --print [N] | --send | --daemon | --daemon-sim");
    }

    // CORRECT WEEK CALCULATION — BULLETPROOF
    private static int calculateCurrentWeekIndex() {
        LocalDate today = LocalDate.now();
        LocalDate upcomingMonday = getUpcomingMonday(today);

        long weeksSinceStart = ChronoUnit.WEEKS.between(WEEK_1_START, upcomingMonday);
        int weekIndex = (int) weeksSinceStart + 1;

        System.out.printf("[DEBUG] Today: %s | Upcoming Monday: %s | Week Index: %d%n",
                today, upcomingMonday, weekIndex);

        return weekIndex;
    }

    // Always returns the NEXT Monday (even if today is Monday → next week)
    private static LocalDate getUpcomingMonday(LocalDate today) {
        DayOfWeek dow = today.getDayOfWeek();
        int daysToAdd = (8 - dow.getValue()) % 7; // Monday = 1
        if (daysToAdd == 0) daysToAdd = 7;       // today is Monday → go to next Monday
        return today.plusDays(daysToAdd);
    }

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

    private static void runDaemon(RotationEngine engine) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        if (webhook == null || webhook.isBlank()) {
            System.err.println("DISCORD_WEBHOOK_URL not set!");
            System.exit(1);
        }

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        long delay = computeInitialDelay(DayOfWeek.SUNDAY, LocalTime.of(18, 0));

        System.out.println("Daemon started — posting every Sunday 18:00");

        exec.scheduleAtFixedRate(() -> {
            try {
                int weekIndex = calculateCurrentWeekIndex();
                LocalDate weekStart = WEEK_1_START.plusWeeks(weekIndex - 1);
                sendSchedule(engine, webhook, weekIndex, weekStart);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, delay, 7 * 24 * 60 * 60, TimeUnit.SECONDS);
    }

    private static long computeInitialDelay(DayOfWeek day, LocalTime time) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime next = now.with(TemporalAdjusters.nextOrSame(day))
                .withHour(time.getHour()).withMinute(time.getMinute()).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusWeeks(1);
        return Duration.between(now, next).getSeconds();
    }

    private static void runDaemonSim(RotationEngine engine, int intervalSec) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        final int[] week = { calculateCurrentWeekIndex() };

        exec.scheduleAtFixedRate(() -> {
            try {
                int wi = week[0]++;
                LocalDate ws = WEEK_1_START.plusWeeks(wi - 1);
                String msg = formatMessage(wi, ws, engine.getWeekSchedule(wi));
                System.out.println(msg);
                if (webhook != null && !webhook.isBlank()) new DiscordNotifier(webhook).send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, intervalSec, TimeUnit.SECONDS);
    }

    private static void printWeeks(RotationEngine engine, int count) {
        int current = calculateCurrentWeekIndex();
        for (int i = 0; i < count; i++) {
            int wi = current + i;
            LocalDate ws = WEEK_1_START.plusWeeks(wi - 1);
            System.out.println(formatMessage(wi, ws, engine.getWeekSchedule(wi)));
            System.out.println("─".repeat(60));
        }
    }

    private static void sendSchedule(RotationEngine engine, String webhook, int weekIndex, LocalDate weekStart) throws Exception {
        WeekSchedule sc = engine.getWeekSchedule(weekIndex);
        String message = formatMessage(weekIndex, weekStart, sc);
        new DiscordNotifier(webhook).send(message);
        System.out.println("Sent Week " + weekIndex + " to Discord");
    }

    private static String formatMessage(int weekIndex, LocalDate weekStart, WeekSchedule sc) {
        return "@everyone\n\n" +
               "**Room-Ruster — Week " + weekIndex + " (starting " + weekStart.format(DATE_FMT) + ")**\n" +
               "Dish Washing (2): " + String.join(", ", sc.dishWashers()) + "\n" +
               "Room Care (1): " + sc.roomCare() + "\n" +
               "Shoe Washing (3): " + String.join(", ", sc.shoeWashers()) + "\n" +
               "Free (" + sc.freePeople().size() + "): " + String.join(", ", sc.freePeople()) + "\n\n" +
               "Sent by Room-Ruster " + VERSION;
    }
}