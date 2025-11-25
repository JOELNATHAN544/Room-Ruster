package com.roomruster;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.roomruster.core.RotationEngine;
import com.roomruster.core.WeekSchedule;
import com.roomruster.notify.DiscordNotifier;

public class App {

    // THIS LINE DID NOT EXIST IN ANY OLD VERSION — IF THIS COMPILES, IT'S NEW CODE
    public static final String I_AM_THE_FINAL_VERSION_NOVEMBER_25_2025 = "YES";

    private static final LocalDate WEEK_1_START = LocalDate.of(2025, 12, 1);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
        System.out.println("Room-Ruster FINAL VERSION — NOV 25 2025 — RUNNING");

        if (args.length == 0) {
            System.out.println("Use --send or --print");
            return;
        }

        RotationEngine engine = new RotationEngine();

        if ("--send".equals(args[0])) {
            sendThisWeek(engine);
        } else if ("--print".equals(args[0])) {
            int n = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            printWeeks(engine, n);
        }
    }

    private static int calculateCurrentWeekIndex() {
        LocalDate today = LocalDate.now();
        LocalDate upcomingMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7 == 0 ? 7 : (8 - today.getDayOfWeek().getValue()) % 7);

        long weeks = ChronoUnit.WEEKS.between(WEEK_1_START, upcomingMonday);
        int weekIndex = (int) weeks + 1;

        System.out.printf("[FINAL DEBUG] Today: %s | Next Monday: %s | Week: %d%n", today, upcomingMonday, weekIndex);
        return weekIndex;
    }

    private static void sendThisWeek(RotationEngine engine) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        if (webhook == null || webhook.isBlank()) {
            System.err.println("No webhook!");
            System.exit(1);
        }

        int weekIndex = calculateCurrentWeekIndex();
        LocalDate weekStart = WEEK_1_START.plusWeeks(weekIndex - 1);
        WeekSchedule sc = engine.getWeekSchedule(weekIndex);

        String message = """
            @everyone

            **Room-Ruster — Week %d (starting %s)**
            Dish Washing (2): %s
            Room Care (1): %s
            Shoe Washing (3): %s
            Free (%d): %s

            THIS MESSAGE WAS SENT BY THE FINAL FIXED VERSION — NOV 25 2025
            """.formatted(
                weekIndex,
                weekStart.format(DATE_FMT),
                String.join(", ", sc.dishWashers()),
                sc.roomCare(),
                String.join(", ", sc.shoeWashers()),
                sc.freePeople().size(),
                String.join(", ", sc.freePeople())
        );

        new DiscordNotifier(webhook).send(message);
        System.out.println("SENT WEEK " + weekIndex);
    }

    private static void printWeeks(RotationEngine engine, int count) {
        for (int i = 0; i < count; i++) {
            int wi = calculateCurrentWeekIndex() + i;
            LocalDate ws = WEEK_1_START.plusWeeks(wi - 1);
            WeekSchedule sc = engine.getWeekSchedule(wi);
            System.out.println(formatMessage(wi, ws, sc));
            System.out.println("─".repeat(60));
        }
    }

    private static String formatMessage(int weekIndex, LocalDate weekStart, WeekSchedule sc) {
        return """
            @everyone

            **Room-Ruster — Week %d (starting %s)**
            Dish Washing (2): %s
            Room Care (1): %s
            Shoe Washing (3): %s
            Free (%d): %s

            THIS MESSAGE WAS SENT BY THE FINAL FIXED VERSION — NOV 25 2025
            """.formatted(
                weekIndex, weekStart.format(DATE_FMT),
                String.join(", ", sc.dishWashers()), sc.roomCare(),
                String.join(", ", sc.shoeWashers()),
                sc.freePeople().size(), String.join(", ", sc.freePeople())
        );
    }
}
