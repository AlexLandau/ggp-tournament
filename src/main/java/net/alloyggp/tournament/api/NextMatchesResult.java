package net.alloyggp.tournament.api;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public interface NextMatchesResult {

    ImmutableSet<MatchSetup> getMatchesToRun();

    Optional<DateTime> getEarliestAllowedStartTime();

    /**
     * If a restriction on the start time for the matches is defined and
     * has not yet passed, returns the number of seconds left until
     * that start time. Otherwise, returns zero.
     */
    long getSecondsToWaitUntilAllowedStartTime();

}