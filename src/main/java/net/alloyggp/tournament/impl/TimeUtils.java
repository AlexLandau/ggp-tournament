package net.alloyggp.tournament.impl;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class TimeUtils {
    private TimeUtils() {
        //Not instantiable
    }

    public static long getSecondsToWaitUntilStartTime(Optional<ZonedDateTime> startTime) {
        if (!startTime.isPresent()) {
            return 0L;
        }
        LocalDateTime now = LocalDateTime.now();
        long seconds = now.until(startTime.get(), ChronoUnit.SECONDS);
        if (seconds < 0L) {
            return 0L;
        }
        return seconds;
    }
}
