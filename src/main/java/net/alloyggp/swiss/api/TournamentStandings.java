package net.alloyggp.swiss.api;

import java.util.Collection;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

public class TournamentStandings {
    //We may have multiple groups, which should be treated separately...
    //Let's ignore this case for now; we may not have to support group play
    private final ImmutableSortedSet<PlayerScore> scores;

    private TournamentStandings(ImmutableSortedSet<PlayerScore> scores) {
        this.scores = scores;
    }

    public static TournamentStandings create(Collection<PlayerScore> scores) {
        return new TournamentStandings(ImmutableSortedSet.copyOf(scores));
    }

    public ImmutableSortedSet<PlayerScore> getScores() {
        return scores;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int position = 1;
        for (PlayerScore score : scores) {
            sb.append(position).append(") ").append(score.getPlayer().getId())
              .append(" (").append(score.getScore()).append(")").append("\n");
            position++;
        }
        return sb.toString();
    }

    public ImmutableList<Player> getPlayersBestFirst() {
        return ImmutableList.copyOf(scores.stream()
                .map(PlayerScore::getPlayer)
                .collect(Collectors.toList()));
    }
}
