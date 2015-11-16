package net.alloyggp.tournament.impl;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Optional;

public class TimeUtils {
    private TimeUtils() {
        //Not instantiable
    }

    public static long getSecondsToWaitUntilStartTime(Optional<DateTime> startTime) {
        if (!startTime.isPresent()) {
            return 0L;
        }
        DateTime now = DateTime.now();
        long seconds = Seconds.secondsBetween(now, startTime.get()).getSeconds();
        if (seconds < 0L) {
            return 0L;
        }
        return seconds;
    }

    public static final DateTimeFormatter RFC1123_DATE_TIME_FORMATTER =
            DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z z")
            .withZoneUTC().withLocale(Locale.US);
}
