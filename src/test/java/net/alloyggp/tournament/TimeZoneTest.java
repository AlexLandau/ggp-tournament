package net.alloyggp.tournament;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Assert;
import org.junit.Test;

import net.alloyggp.tournament.spec.RoundSpec;
import net.alloyggp.tournament.spec.TournamentSpec;

public class TimeZoneTest {
    @Test
    public void testSampleMatchOneTimeComparison() {
        TournamentSpec tournament = TestSpecs.load("singleElim");
        RoundSpec firstRound = tournament.getStages().get(0).getRounds().get(0);

        ZonedDateTime startTime = firstRound.getStartTime().get();
        //Check that it equals the intended time in various time zones
        ZonedDateTime pstTime = ZonedDateTime.of(
                LocalDate.of(2015, Month.DECEMBER, 4),
                LocalTime.of(10, 0),
                ZoneId.of("America/Los_Angeles"));
        Assert.assertTrue(startTime.isEqual(pstTime));
        Assert.assertTrue(startTime.isBefore(pstTime.plusSeconds(1L)));
        Assert.assertTrue(startTime.isAfter(pstTime.minusSeconds(1L)));

        ZonedDateTime utcTime = ZonedDateTime.of(
                LocalDate.of(2015, Month.DECEMBER, 4),
                LocalTime.of(18, 0),
                ZoneId.of("UTC"));
        Assert.assertTrue(startTime.isEqual(utcTime));
    }
}
