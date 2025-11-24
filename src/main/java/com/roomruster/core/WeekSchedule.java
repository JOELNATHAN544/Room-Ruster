package com.roomruster.core;

import java.util.List;

public record WeekSchedule(
        int weekIndex,
        List<String> dishWashers,
        List<String> shoeWashers,
        String roomCare,
        List<String> freePeople
) {}
