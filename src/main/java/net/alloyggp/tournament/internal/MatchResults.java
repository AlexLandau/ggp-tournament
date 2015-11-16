package net.alloyggp.tournament.internal;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TMatchResult;

/**
 * Contains utility methods for dealing with {@link TMatchResult}s.
 */
public class MatchResults {
    private MatchResults() {
        //Not instantiable
    }

    /**
     * Returns only those {@link TMatchResult}s that are from the given stage.
     */
    public static Set<TMatchResult> filterByStage(Collection<TMatchResult> inputs, final int stageNum) {
        return Sets.newHashSet(Collections2.filter(inputs,
                new Predicate<TMatchResult>() {
            @Override
            public boolean apply(@Nonnull TMatchResult input) {
                String matchId = input.getMatchId();
                int matchStage = MatchIds.parseStageNumber(matchId);
                return matchStage == stageNum;
            }
        }));
    }

    /**
     * Returns only those {@link TMatchResult}s that are from a stage before the
     * one specified.
     */
    public static Set<TMatchResult> getResultsPriorToStage(Collection<TMatchResult> inputs, final int stageNum) {
        return Sets.newHashSet(Collections2.filter(inputs,
                new Predicate<TMatchResult>() {
            @Override
            public boolean apply(@Nonnull TMatchResult input) {
                String matchId = input.getMatchId();
                int matchStage = MatchIds.parseStageNumber(matchId);
                return matchStage < stageNum;
            }
        }));
    }

    /**
     * Returns a {@link SetMultimap} including all the {@link TMatchResult}s from the given
     * stage. The results are grouped by their round number.
     */
    public static SetMultimap<Integer, TMatchResult> mapByRound(Collection<TMatchResult> resultsSoFar,
            int stageNum) {
        SetMultimap<Integer, TMatchResult> mapped = HashMultimap.create();
        for (TMatchResult result : filterByStage(resultsSoFar, stageNum)) {
            String matchId = result.getMatchId();
            int matchRound = MatchIds.parseRoundNumber(matchId);
            mapped.put(matchRound, result);
        }
        return mapped;
    }
}
