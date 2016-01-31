package net.alloyggp.tournament.internal.runner;

import java.util.List;
import java.util.Set;

import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.InternalMatchResult;
import net.alloyggp.tournament.internal.spec.RoundSpec;

import com.google.common.collect.ImmutableList;

public interface FormatRunner {

    TNextMatchesResult getMatchesToRun(String tournamentInternalName, TSeeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<InternalMatchResult> resultsSoFar);

    List<TRanking> getStandingsHistory(String tournamentInternalName, TSeeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<InternalMatchResult> resultsSoFar);

    void validateRounds(ImmutableList<RoundSpec> rounds);

}
