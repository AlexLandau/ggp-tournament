package net.alloyggp.tournament.impl;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

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
    public static Set<MatchResult> filterByStage(Collection<MatchResult> inputs, final int stageNum) {
        return Sets.newHashSet(Collections2.filter(inputs,
                new Predicate<MatchResult>() {
            @Override
            public boolean apply(MatchResult input) {
                String matchId = input.getMatchId();
                int matchStage = MatchIds.parseStageNumber(matchId);
                return matchStage == stageNum;
            }
        }));
    }

    /**
     * Returns only those {@link MatchResult}s that are from a stage before the
     * one specified.
     */
    public static Set<MatchResult> getResultsPriorToStage(Collection<MatchResult> inputs, final int stageNum) {
        return Sets.newHashSet(Collections2.filter(inputs,
                new Predicate<MatchResult>() {
            @Override
            public boolean apply(MatchResult input) {
                String matchId = input.getMatchId();
                int matchStage = MatchIds.parseStageNumber(matchId);
                return matchStage < stageNum;
            }
        }));
    }

    /**
     * Returns a {@link SetMultimap} including all the {@link MatchResult}s from the given
     * stage. The results are grouped by their round number.
     */
    public static SetMultimap<Integer, MatchResult> mapByRound(Collection<MatchResult> resultsSoFar,
            int stageNum) {
        SetMultimap<Integer, MatchResult> mapped = HashMultimap.create();
        for (MatchResult result : filterByStage(resultsSoFar, stageNum)) {
            String matchId = result.getMatchId();
            int matchRound = MatchIds.parseRoundNumber(matchId);
            mapped.put(matchRound, result);
        }
        return mapped;
    }
}
