package net.alloyggp.swiss.api;

import java.util.Set;

import com.google.common.base.Optional;

/**
 * A TournamentRunner embodies an algorithm for arranging matches in a tournament.
 * Given
 */
public interface TournamentRunner {
	Set<MatchSetup> getMatchesReadyToRun(TournamentStatus status);

	Optional<TournamentResult> getResultIfFinished(TournamentStatus status);

	TournamentStandings getStandings(TournamentStatus status);
}
