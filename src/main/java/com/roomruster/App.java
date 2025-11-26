package com.roomruster;

import java.time.*;
import java.time.format.DateTimeFormatter;

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
        
        // Debug: Print environment variables
        System.out.println("Environment variables:");
        System.getenv().forEach((key, value) -> {
            if (key.contains("DISCORD")) {
                System.out.println(key + " = " + value);
            }
        });
        
        // Debug: Print system properties
        System.out.println("System properties:");
        System.getProperties().forEach((key, value) -> {
            if (key.toString().contains("discord")) {
                System.out.println(key + " = " + value);
            }
        });

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

    private static int getNextWeekIndex() throws java.io.IOException {
        java.nio.file.Path stateFile = java.nio.file.Paths.get("/app/state/last-posted-week.txt");
        int lastPostedWeek = 0;
        if (java.nio.file.Files.exists(stateFile)) {
            String content = java.nio.file.Files.readString(stateFile).trim();
            if (!content.isEmpty()) {
                lastPostedWeek = Integer.parseInt(content);
            }
        }
        return lastPostedWeek + 1;
    }

    private static void saveLastPostedWeek(int weekIndex) throws java.io.IOException {
        java.nio.file.Path stateDir = java.nio.file.Paths.get("/app/state");
        java.nio.file.Files.createDirectories(stateDir);
        java.nio.file.Path stateFile = stateDir.resolve("last-posted-week.txt");
        java.nio.file.Files.writeString(stateFile, String.valueOf(weekIndex));
    }

    private static void sendThisWeek(RotationEngine engine) throws Exception {
        String webhook = System.getenv("DISCORD_WEBHOOK_URL");
        if (webhook == null || webhook.isBlank()) {
            System.err.println("No webhook!");
            System.exit(1);
        }

        int weekIndex = getNextWeekIndex();
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
        saveLastPostedWeek(weekIndex);
        System.out.println("SENT AND SAVED WEEK " + weekIndex);
    }

    private static void printWeeks(RotationEngine engine, int count) throws java.io.IOException {
        int nextWeek = getNextWeekIndex();
        for (int i = 0; i < count; i++) {
            int wi = nextWeek + i;
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
