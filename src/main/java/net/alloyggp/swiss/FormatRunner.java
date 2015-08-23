package net.alloyggp.swiss;

import java.util.Set;

import com.google.common.collect.ImmutableList;

import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.RoundSpec;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.TournamentStandings;

public interface FormatRunner {

	Set<MatchSetup> getMatchesToRun(Seeding initialSeeding, ImmutableList<RoundSpec> rounds,
			Set<MatchResult> resultsSoFar);

	TournamentStandings getStandingsSoFar(Seeding initialSeeding, ImmutableList<RoundSpec> rounds,
			Set<MatchResult> resultsSoFar);

}
