package net.alloyggp.tournament.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.PlayerScore;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Score;
import net.alloyggp.tournament.api.Seeding;

@Immutable
public class StandardRanking implements Ranking {
    //We may have multiple groups, which should be treated separately...
    //Let's ignore this case for now; we may not have to support group play
    private final ImmutableSortedSet<PlayerScore> scores;

    private StandardRanking(ImmutableSortedSet<PlayerScore> scores) {
        this.scores = scores;
    }

    public static StandardRanking create(Collection<PlayerScore> scores) {
        return new StandardRanking(ImmutableSortedSet.copyOf(scores));
    }

    /* (non-Javadoc)
     * @see net.alloyggp.swiss.api.Ranking#getScores()
     */
    @Override
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
        StandardRanking other = (StandardRanking) obj;
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

    /* (non-Javadoc)
     * @see net.alloyggp.swiss.api.Ranking#getPlayersBestFirst()
     */
    @Override
    public ImmutableList<Player> getPlayersBestFirst() {
        return ImmutableList.copyOf(scores.stream()
                .map(PlayerScore::getPlayer)
                .collect(Collectors.toList()));
    }

    public static StandardRanking createForSeeding(Seeding initialSeeding) {
        List<PlayerScore> scores = Lists.newArrayList();
        ImmutableList<Player> players = initialSeeding.getPlayersBestFirst();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            scores.add(PlayerScore.create(player, EmptyScore.create(), i));
        }
        return StandardRanking.create(scores);
    }

    public static class EmptyScore implements Score {
        private EmptyScore() {
            // Use create()
        }

        @Override
        public int compareTo(Score other) {
            if (!(other instanceof EmptyScore)) {
                throw new IllegalArgumentException("Incomparable scores being compared");
            }
            return 0;
        }

        public static Score create() {
            return new EmptyScore();
        }

        @Override
        public int hashCode() {
            return 1;
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
            return true;
        }

        @Override
        public String toString() {
            return "initial seeding for stage";
        }
    }
}
