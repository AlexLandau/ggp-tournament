package net.alloyggp.tournament.impl;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import net.alloyggp.tournament.api.MatchResult;

/**
 * Contains utility methods for dealing with {@link MatchResult}s.
 */
public class MatchResults {
    private MatchResults() {
        //Not instantiable
    }

    /**
     * Returns only those {@link MatchResult}s that are from the given stage.
     */
    public static Set<MatchResult> filterByStage(Collection<MatchResult> inputs, int stageNum) {
        return inputs.stream()
            .filter(result -> {
                    String matchId = result.getSetup().getMatchId();
                    int matchStage = MatchIds.parseStageNumber(matchId);
                    return matchStage == stageNum;
                })
            .collect(Collectors.toSet());
    }

    /**
     * Returns only those {@link MatchResult}s that are from a stage before the
     * one specified.
     */
    public static Set<MatchResult> getResultsPriorToStage(Collection<MatchResult> inputs, int stageNum) {
        return inputs.stream()
                .filter(result -> {
                        String matchId = result.getSetup().getMatchId();
                        int matchStage = MatchIds.parseStageNumber(matchId);
                        return matchStage < stageNum;
                    })
                .collect(Collectors.toSet());
    }

    /**
     * Returns a {@link SetMultimap} including all the {@link MatchResult}s from the given
     * stage. The results are grouped by their round number.
     */
    public static SetMultimap<Integer, MatchResult> mapByRound(Collection<MatchResult> resultsSoFar,
            int stageNum) {
        SetMultimap<Integer, MatchResult> mapped = HashMultimap.create();
        for (MatchResult result : filterByStage(resultsSoFar, stageNum)) {
            String matchId = result.getSetup().getMatchId();
            int matchRound = MatchIds.parseRoundNumber(matchId);
            mapped.put(matchRound, result);
        }
        return mapped;
    }
}
