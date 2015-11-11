package net.alloyggp.tournament.api;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The main interface for a tournament specification.
 *
 * <p>A Tournament object can be obtained from a YAML specification on
 * the file system via the {@link TournamentSpecParser}.
 */
public interface Tournament {

    /**
     * Returns a tournament name consisting of alphanumerics and underscores
     * that is suitable for internal use as a key identifying the tournament.
     * This name is specified by the tournament designer, so it may be necessary
     * to screen out duplicate names.
     */
    String getTournamentInternalName();

    /**
     * Returns a tournament name that is suitable for human-readable displays.
     */
    String getTournamentDisplayName();

    /**
     * Returns the set of matches that should be run in the given tournament state.
     */
    NextMatchesResult getMatchesToRun(Seeding initialSeeding, Set<MatchResult> resultsSoFar);

    /**
     * Returns the most recent standings in the given tournament state.
     */
    Ranking getCurrentStandings(Seeding initialSeeding, Set<MatchResult> resultsSoFar);

    /**
     * Returns a history of the standings throughout the tournament, starting with the initial
     * seeding and progressing through each round.
     */
    List<Ranking> getStandingsHistory(Seeding initialSeeding, Set<MatchResult> resultsSoFar);

    /**
     * Gets the start time for the first round of the tournament if it has
     * been defined.
     */
    Optional<ZonedDateTime> getInitialStartTime();

    /**
     * If a restriction on the start time for the tournament is defined and
     * has not yet passed, returns the number of seconds left until
     * that start time. Otherwise, returns zero.
     */
    long getSecondsToWaitUntilInitialStartTime();

}