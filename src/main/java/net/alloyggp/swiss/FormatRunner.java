package net.alloyggp.swiss;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.TournamentStandings;
import net.alloyggp.swiss.spec.RoundSpec;

public interface FormatRunner {

    Set<MatchSetup> getMatchesToRun(String tournamentInternalName, Seeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<MatchResult> resultsSoFar);

    TournamentStandings getStandingsSoFar(String tournamentInternalName, Seeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<MatchResult> resultsSoFar);

    void validateRounds(ImmutableList<RoundSpec> rounds);

}
