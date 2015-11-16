package net.alloyggp.tournament;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import net.alloyggp.tournament.internal.TimeUtils;
import net.alloyggp.tournament.internal.spec.RoundSpec;
import net.alloyggp.tournament.internal.spec.TournamentSpec;

public class TimeZoneTest {
    @Test
    public void testSampleMatchOneTimeComparison() {
        TournamentSpec tournament = TestSpecs.load("singleElim");
        RoundSpec firstRound = tournament.getStages().get(0).getRounds().get(0);

        DateTime startTime = firstRound.getStartTime().get();
        //Check that it equals the intended time in various time zones
        DateTime pstTime = TimeUtils.RFC1123_DATE_TIME_FORMATTER.parseDateTime("Fri, 4 Dec 2015 10:00:00 -0800 PST");
//        DateTime pstTime = DateTime.of(
//                LocalDate.of(2015, Months.TWELVE, 4),
//                LocalTime.of(10, 0),
//                ZoneId.of("America/Los_Angeles"));
        Assert.assertTrue(startTime.isEqual(pstTime));
        Assert.assertTrue(startTime.isBefore(pstTime.plusSeconds(1)));
        Assert.assertTrue(startTime.isAfter(pstTime.minusSeconds(1)));

//        DateTime utcTime = DateTime.of(
//                LocalDate.of(2015, Month.DECEMBER, 4),
//                LocalTime.of(18, 0),
//                ZoneId.of("UTC"));
        DateTime utcTime = TimeUtils.RFC1123_DATE_TIME_FORMATTER.parseDateTime("Fri, 4 Dec 2015 18:00:00 +0000 UTC");
        Assert.assertTrue(startTime.isEqual(utcTime));
    }
}
