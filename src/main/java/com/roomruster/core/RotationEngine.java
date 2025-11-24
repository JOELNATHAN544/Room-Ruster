package com.roomruster.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RotationEngine {
    private static final List<String> DISH_WASHERS = List.of("Maxwell", "Prosper");
    private static final List<String> ROTATION_ORDER = List.of(
            "Nathan", "Onel", "Derick", "Severian", "Nine"
    );

    public WeekSchedule getWeekSchedule(int weekIndex) {
        if (weekIndex < 1) throw new IllegalArgumentException("weekIndex must be >= 1");

        int offset = (weekIndex - 1) % ROTATION_ORDER.size();
        List<String> rotated = rotate(ROTATION_ORDER, offset);

        List<String> shoe = new ArrayList<>();
        shoe.add(rotated.get(0));
        shoe.add(rotated.get(1));
        shoe.add(rotated.get(2));

        String free = rotated.get(3);
        String roomCareNormally = rotated.get(4);

        String roomCare;
        List<String> freePeople = new ArrayList<>();
        freePeople.add(free);

        if (weekIndex % 8 == 0) {
            // Week 8 override: Frank takes room care, previous room-care person becomes free too
            roomCare = "Frank";
            freePeople.add(roomCareNormally);
        } else {
            roomCare = roomCareNormally;
        }

        return new WeekSchedule(
                weekIndex,
                DISH_WASHERS,
                shoe,
                roomCare,
                freePeople
        );
    }

    private static <T> List<T> rotate(List<T> list, int offset) {
        int n = list.size();
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(list.get((i + offset) % n));
        }
        return out;
    }
}
