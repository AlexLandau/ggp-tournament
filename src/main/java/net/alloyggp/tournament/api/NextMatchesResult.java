package net.alloyggp.tournament.api;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;

public interface NextMatchesResult {

    ImmutableSet<MatchSetup> getMatchesToRun();

    Optional<ZonedDateTime> getEarliestAllowedStartTime();

    /**
     * If a restriction on the start time for the matches is defined and
     * has not yet passed, returns the number of seconds left until
     * that start time. Otherwise, returns zero.
     */
    long getSecondsToWaitUntilAllowedStartTime();

}