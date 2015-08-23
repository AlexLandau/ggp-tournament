package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Immutable
public class MatchResult {
    private final MatchSetup setup;
    private final Outcome outcome;
    private final Optional<ImmutableList<Integer>> goals;
    private final ImmutableList<ImmutableMap<Integer, String>> errorsByTurnByRole;

    private MatchResult(MatchSetup setup, Outcome outcome, Optional<ImmutableList<Integer>> goals,
            ImmutableList<ImmutableMap<Integer, String>> errorsByTurnByRole) {
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
        this.errorsByTurnByRole = errorsByTurnByRole;
    }

    public static MatchResult getAbortedMatchResult(MatchSetup setup, List<Map<Integer, String>> errorsByTurnByRole) {
        return new MatchResult(setup, Outcome.ABORTED, Optional.absent(), immutify(errorsByTurnByRole));
    }

    public static MatchResult getSuccessfulMatchResult(MatchSetup setup, List<Integer> goals,
            List<Map<Integer, String>> errorsByTurnByRole) {
        return new MatchResult(setup, Outcome.COMPLETED, Optional.of(ImmutableList.copyOf(goals)),
                immutify(errorsByTurnByRole));
    }

    private static ImmutableList<ImmutableMap<Integer, String>> immutify(
            List<Map<Integer, String>> input) {
        return ImmutableList.copyOf(input.stream()
                .map(ImmutableMap::copyOf)
                .collect(Collectors.toList()));
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

    public Map<Integer, String> getErrorsByTurn(int roleIndex) {
        return errorsByTurnByRole.get(roleIndex);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((errorsByTurnByRole == null) ? 0 : errorsByTurnByRole.hashCode());
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
        if (errorsByTurnByRole == null) {
            if (other.errorsByTurnByRole != null) {
                return false;
            }
        } else if (!errorsByTurnByRole.equals(other.errorsByTurnByRole)) {
            return false;
        }
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
        return "MatchResult [setup=" + setup + ", outcome=" + outcome + ", goals=" + goals + ", errorsByTurnByRole="
                + errorsByTurnByRole + "]";
    }

    public static enum Outcome {
        COMPLETED,
        ABORTED
    }
}
