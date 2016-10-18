package net.alloyggp.tournament.internal;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchResult.Outcome;

/**
 * This is used internally to avoid repeated parsing of the match ID string.
 */
public class InternalMatchResult {
    private final TMatchResult result;
    private final MatchId matchId;

    private InternalMatchResult(TMatchResult result, MatchId matchId) {
        Preconditions.checkArgument(result.getMatchId().equals(matchId.toString()));
        this.result = result;
        this.matchId = matchId;
    }

    public static InternalMatchResult create(TMatchResult result) {
        return new InternalMatchResult(result, MatchId.create(result.getMatchId()));
    }

    public MatchId getMatchId() {
        return matchId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((matchId == null) ? 0 : matchId.hashCode());
        result = prime * result
                + ((this.result == null) ? 0 : this.result.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InternalMatchResult other = (InternalMatchResult) obj;
        if (matchId == null) {
            if (other.matchId != null)
                return false;
        } else if (!matchId.equals(other.matchId))
            return false;
        if (result == null) {
            if (other.result != null)
                return false;
        } else if (!result.equals(other.result))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "InternalMatchResult [result=" + result + ", matchId=" + matchId
                + "]";
    }

    public Outcome getOutcome() {
        return result.getOutcome();
    }

    public List<Integer> getGoals() {
        return result.getGoals();
    }

    //TODO: Consider caching created results?
    public static Set<InternalMatchResult> convertResults(
            Collection<TMatchResult> clientResults) {
        ImmutableSet.Builder<InternalMatchResult> builder = ImmutableSet.builder();
        for (TMatchResult result : clientResults) {
            builder.add(create(result));
        }
        return builder.build();
    }

}
