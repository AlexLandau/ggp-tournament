package net.alloyggp.tournament.internal;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TNextMatchesResult;

/**
 * Contains a set of matches that should be scheduled for the
 * tournament if they are not already running. If the set of
 * matches is empty, the tournament is over.
 *
 * <p>Before scheduling the matches, the client should check if
 * there is a restriction on the start time for matches. (This can
 * be used by tournament organizers, for example, to spread
 * tournaments across multiple non-consecutive blocks, such as on
 * separate days.) This is most easily done with the
 * {@link #getSecondsToWaitUntilAllowedStartTime()} method; if this returns
 * a non-zero value, the client should wait instead of scheduling
 * additional matches.
 */
//TODO: Make the public-facing part an interface
public class StandardNextMatchesResult implements TNextMatchesResult {
    private final ImmutableSet<TMatchSetup> matchesToRun;
    //Note: These may be moved into individual matches in the future.
    private final Optional<DateTime> earliestAllowedStartTime;

    private StandardNextMatchesResult(ImmutableSet<TMatchSetup> matchesToRun,
            Optional<DateTime> earliestAllowedStartTime) {
        this.matchesToRun = matchesToRun;
        this.earliestAllowedStartTime = earliestAllowedStartTime;
    }

    public static TNextMatchesResult createEmpty() {
        return new StandardNextMatchesResult(ImmutableSet.<TMatchSetup>of(),
                Optional.<DateTime>absent());
    }

    public static TNextMatchesResult create(Iterable<TMatchSetup> matchesToRun,
            @Nullable DateTime earliestAllowedStartTime) {
        return new StandardNextMatchesResult(
                ImmutableSet.copyOf(matchesToRun),
                Optional.fromNullable(earliestAllowedStartTime));
    }

    @Override
    public ImmutableSet<TMatchSetup> getMatchesToRun() {
        return matchesToRun;
    }

    @Override
    public Optional<DateTime> getEarliestAllowedStartTime() {
        return earliestAllowedStartTime;
    }

    /**
     * If a restriction on the start time for the matches is defined and
     * has not yet passed, returns the number of seconds left until
     * that start time. Otherwise, returns zero.
     */
    @Override
    public long getSecondsToWaitUntilAllowedStartTime() {
        return TimeUtils.getSecondsToWaitUntilStartTime(earliestAllowedStartTime);
    }
}
