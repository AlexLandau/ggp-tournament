package net.alloyggp.tournament.api;

import java.util.List;
import java.util.Set;

/**
 * The main interface for a tournament specification.
 *
 * <p>A Tournament object can be obtained from a YAML specification on
 * the file system via the {@link TournamentSpecParser}.
 */
public interface Tournament {

    String getTournamentInternalName();

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

}