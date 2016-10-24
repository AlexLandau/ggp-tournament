package net.alloyggp.tournament.internal;

import java.util.Collection;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

import net.alloyggp.escaperope.rope.ropify.CoreWeavers;
import net.alloyggp.escaperope.rope.ropify.Weaver;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TPlayerScore;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TScore;
import net.alloyggp.tournament.api.TSeeding;

@Immutable
public class StandardRanking implements TRanking {
    //We may have multiple groups, which should be treated separately...
    //Let's ignore this case for now; we may not have to support group play
    private final ImmutableSortedSet<TPlayerScore> scores;

    private StandardRanking(ImmutableSortedSet<TPlayerScore> scores) {
        this.scores = scores;
    }

    public static StandardRanking create(Collection<TPlayerScore> scores) {
        return new StandardRanking(ImmutableSortedSet.copyOf(scores));
    }

    /* (non-Javadoc)
     * @see net.alloyggp.swiss.api.Ranking#getScores()
     */
    @Override
    public ImmutableSortedSet<TPlayerScore> getScores() {
        return scores;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scores == null) ? 0 : scores.hashCode());
        return result;
    }

    /*
     * Note: Why do we convert to a list in order to do this comparison?
     * Because ImmutableSortedSet will use compareTo() instead of equals()
     * to do its equality check, and this is undesirable when we're comparing
     * scores, which will throw an exception when compared to an incomparable
     * score. The list implementation instead compares corresponding indices
     * with equals(), which is valid for incomparable scores and gives us the
     * same result anyway.
     */
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
        } else if (!scores.asList().equals(other.scores.asList())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int position = 1;
        for (TPlayerScore score : scores) {
            sb.append(position).append(") ").append(score.getPlayer().getId())
              .append(" (").append(score.getScore()).append(")").append("\n");
            position++;
        }
        return sb.toString();
    }

    @Override
    public ImmutableList<TPlayer> getPlayersBestFirst() {
        ImmutableList.Builder<TPlayer> builder = ImmutableList.builder();
        for (TPlayerScore score : scores) {
            builder.add(score.getPlayer());
        }
        return builder.build();
    }

    public static StandardRanking createForSeeding(TSeeding initialSeeding) {
        List<TPlayerScore> scores = Lists.newArrayList();
        ImmutableList<TPlayer> players = initialSeeding.getPlayersBestFirst();
        for (int i = 0; i < players.size(); i++) {
            TPlayer player = players.get(i);
            scores.add(TPlayerScore.create(player, EmptyScore.create(), i));
        }
        return StandardRanking.create(scores);
    }

    public static class EmptyScore implements TScore {
        public static final Weaver<TScore> WEAVER = CoreWeavers.singletonOf(EmptyScore.create());

        private EmptyScore() {
            // Use create()
        }

        @Override
        public int compareTo(TScore other) {
            if (!(other instanceof EmptyScore)) {
                throw new IllegalArgumentException("Incomparable scores being compared");
            }
            return 0;
        }

        public static TScore create() {
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
            return getDescription();
        }

        @Override
        public String getDescription() {
            return "initial seeding for stage";
        }
    }

    @Override
    public int getPosition(TPlayer player) {
        Preconditions.checkNotNull(player);
        ImmutableList<TPlayerScore> list = scores.asList();
        for (int i = 0; i < scores.size(); i++) {
            if (list.get(i).getPlayer().equals(player)) {
                //Convert to 1-indexing
                return i + 1;
            }
        }
        throw new IllegalArgumentException("The player " + player + " is not in the ranking: " + toString());
    }
}
