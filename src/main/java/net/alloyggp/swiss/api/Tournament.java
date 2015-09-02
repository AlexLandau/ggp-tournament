package net.alloyggp.swiss.api;

import java.util.Set;

import com.google.common.collect.ImmutableList;

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
    Set<MatchSetup> getMatchesToRun(Seeding initialSeeding, ImmutableList<MatchResult> resultsSoFar);

    /**
     * Returns the standings in the given tournament state.
     */
    TournamentStandings getCurrentStandings(Seeding initialSeeding, ImmutableList<MatchResult> resultsSoFar);

}