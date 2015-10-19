package net.alloyggp.tournament.impl;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.spec.RoundSpec;

public interface FormatRunner {

    Set<MatchSetup> getMatchesToRun(String tournamentInternalName, Seeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<MatchResult> resultsSoFar);

    List<Ranking> getStandingsHistory(String tournamentInternalName, Seeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<MatchResult> resultsSoFar);

    void validateRounds(ImmutableList<RoundSpec> rounds);

}
