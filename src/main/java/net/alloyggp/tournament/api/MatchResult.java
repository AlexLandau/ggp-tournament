package net.alloyggp.tournament.api;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

@Immutable
public class MatchResult {
    private final String matchId;
    //TODO: Eliminate this. This may require significant refactoring of the
    //SingleEliminationFormatRunner.
    private final ImmutableList<Player> players;
    private final Outcome outcome;
    private final Optional<ImmutableList<Integer>> goals;

    private MatchResult(String matchId, ImmutableList<Player> players,
            Outcome outcome, Optional<ImmutableList<Integer>> goals) {
        if (outcome == Outcome.COMPLETED) {
            Preconditions.checkArgument(goals.isPresent());
        }
        if (goals.isPresent()) {
            for (int goal : goals.get()) {
                Preconditions.checkArgument(goal >= 0 && goal <= 100);
            }
        }
        this.matchId = matchId;
        this.players = players;
        this.outcome = outcome;
        this.goals = goals;
    }

    public static MatchResult getAbortedMatchResult(String matchId, List<Player> players) {
        return new MatchResult(matchId, ImmutableList.copyOf(players),
                Outcome.ABORTED, Optional.absent());
    }

    public static MatchResult getSuccessfulMatchResult(String matchId, List<Player> players, List<Integer> goals) {
        return new MatchResult(matchId, ImmutableList.copyOf(players),
                Outcome.COMPLETED, Optional.of(ImmutableList.copyOf(goals)));
    }

    public String getMatchId() {
        return matchId;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public List<Integer> getGoals() {
        return goals.get();
    }

    public ImmutableList<Player> getPlayers() {
        return players;
    }

    public boolean wasAborted() {
        return outcome == Outcome.ABORTED;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((goals == null) ? 0 : goals.hashCode());
        result = prime * result + ((matchId == null) ? 0 : matchId.hashCode());
        result = prime * result + ((outcome == null) ? 0 : outcome.hashCode());
        result = prime * result + ((players == null) ? 0 : players.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MatchResult other = (MatchResult) obj;
        if (goals == null) {
            if (other.goals != null) {
                return false;
            }
        } else if (!goals.equals(other.goals)) {
            return false;
        }
        if (matchId == null) {
            if (other.matchId != null) {
                return false;
            }
        } else if (!matchId.equals(other.matchId)) {
            return false;
        }
        if (outcome != other.outcome) {
            return false;
        }
        if (players == null) {
            if (other.players != null) {
                return false;
            }
        } else if (!players.equals(other.players)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MatchResult [matchId=" + matchId + ", players=" + players + ", outcome=" + outcome + ", goals=" + goals
                + "]";
    }

    public static enum Outcome {
        COMPLETED,
        ABORTED
    }
}
