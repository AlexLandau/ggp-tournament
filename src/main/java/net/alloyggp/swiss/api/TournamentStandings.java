package net.alloyggp.swiss.api;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

import net.alloyggp.swiss.spec.TournamentSpec.EmptyScore;

@Immutable
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scores == null) ? 0 : scores.hashCode());
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
        TournamentStandings other = (TournamentStandings) obj;
        if (scores == null) {
            if (other.scores != null) {
                return false;
            }
        } else if (!scores.equals(other.scores)) {
            return false;
        }
        return true;
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

    //TODO: Can we keep this out of the API? Clients should only query standings,
    //not create them
    public static TournamentStandings createForSeeding(Seeding initialSeeding) {
        List<PlayerScore> scores = Lists.newArrayList();
        ImmutableList<Player> players = initialSeeding.getPlayersBestFirst();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            scores.add(PlayerScore.create(player, EmptyScore.create(), i));
        }
        return TournamentStandings.create(scores);
    }
}
