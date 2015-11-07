package net.alloyggp.tournament.api;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

@Immutable
public class MatchResult {
    private final MatchSetup setup;
    private final Outcome outcome;
    private final Optional<ImmutableList<Integer>> goals;

    private MatchResult(MatchSetup setup, Outcome outcome, Optional<ImmutableList<Integer>> goals) {
        if (outcome == Outcome.COMPLETED) {
            Preconditions.checkArgument(goals.isPresent());
        }
        if (goals.isPresent()) {
            Preconditions.checkArgument(goals.get().size() == setup.getPlayers().size());
            for (int goal : goals.get()) {
                Preconditions.checkArgument(goal >= 0 && goal <= 100);
            }
        }
        this.setup = setup;
        this.outcome = outcome;
        this.goals = goals;
    }

    public static MatchResult getAbortedMatchResult(MatchSetup setup) {
        return new MatchResult(setup, Outcome.ABORTED, Optional.absent());
    }

    public static MatchResult getSuccessfulMatchResult(MatchSetup setup, List<Integer> goals) {
        return new MatchResult(setup, Outcome.COMPLETED, Optional.of(ImmutableList.copyOf(goals)));
    }

    public MatchSetup getSetup() {
        return setup;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public List<Integer> getGoals() {
        return goals.get();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((goals == null) ? 0 : goals.hashCode());
        result = prime * result + ((outcome == null) ? 0 : outcome.hashCode());
        result = prime * result + ((setup == null) ? 0 : setup.hashCode());
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
        if (outcome != other.outcome) {
            return false;
        }
        if (setup == null) {
            if (other.setup != null) {
                return false;
            }
        } else if (!setup.equals(other.setup)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MatchResult [setup=" + setup + ", outcome=" + outcome + ", goals=" + goals + "]";
    }

    public static enum Outcome {
        COMPLETED,
        ABORTED
    }
}
