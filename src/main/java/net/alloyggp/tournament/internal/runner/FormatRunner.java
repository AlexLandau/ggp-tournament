package net.alloyggp.tournament.internal.runner;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.InternalMatchResult;
import net.alloyggp.tournament.internal.admin.InternalAdminAction;
import net.alloyggp.tournament.internal.spec.RoundSpec;

public interface FormatRunner {

    TNextMatchesResult getMatchesToRun(String tournamentInternalName, TSeeding initialSeeding,
            List<InternalAdminAction> adminActions,
            int stageNum, List<RoundSpec> rounds, Set<InternalMatchResult> resultsSoFar);

    List<TRanking> getStandingsHistory(String tournamentInternalName, TSeeding initialSeeding,
            List<InternalAdminAction> adminActions,
            int stageNum, List<RoundSpec> rounds, Set<InternalMatchResult> resultsSoFar);

    void validateRounds(ImmutableList<RoundSpec> rounds);

}
