package net.alloyggp.tournament.internal;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * Contains utility methods for dealing with {@link InternalMatchResult}s.
 */
public class MatchResults {
    private MatchResults() {
        //Not instantiable
    }

    /**
     * Returns only those {@link InternalMatchResult}s that are from the given stage.
     */
    public static Set<InternalMatchResult> filterByStage(Collection<InternalMatchResult> inputs, final int stageNum) {
        return Sets.newHashSet(Collections2.filter(inputs,
                new Predicate<InternalMatchResult>() {
            @Override
            public boolean apply(@Nonnull InternalMatchResult input) {
                int matchStage = input.getMatchId().getStageNumber();
                return matchStage == stageNum;
            }
        }));
    }

    /**
     * Returns only those {@link InternalMatchResult}s that are from a stage before the
     * one specified.
     */
    public static Set<InternalMatchResult> getResultsPriorToStage(Collection<InternalMatchResult> inputs, final int stageNum) {
        return Sets.newHashSet(Collections2.filter(inputs,
                new Predicate<InternalMatchResult>() {
            @Override
            public boolean apply(@Nonnull InternalMatchResult input) {
                int matchStage = input.getMatchId().getStageNumber();
                return matchStage < stageNum;
            }
        }));
    }

    /**
     * Returns a {@link SetMultimap} including all the {@link InternalMatchResult}s from the given
     * stage. The results are grouped by their round number.
     */
    public static SetMultimap<Integer, InternalMatchResult> mapByRound(Collection<InternalMatchResult> resultsSoFar,
            int stageNum) {
        SetMultimap<Integer, InternalMatchResult> mapped = HashMultimap.create();
        for (InternalMatchResult result : filterByStage(resultsSoFar, stageNum)) {
            int matchRound = result.getMatchId().getRoundNumber();
            mapped.put(matchRound, result);
        }
        return mapped;
    }
}
