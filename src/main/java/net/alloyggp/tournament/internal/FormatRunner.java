package net.alloyggp.tournament.internal;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.spec.RoundSpec;

public interface FormatRunner {

    TNextMatchesResult getMatchesToRun(String tournamentInternalName, TSeeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<TMatchResult> resultsSoFar);

    List<TRanking> getStandingsHistory(String tournamentInternalName, TSeeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<TMatchResult> resultsSoFar);

    void validateRounds(ImmutableList<RoundSpec> rounds);

}
