package net.alloyggp.tournament.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

public interface Ranking {
    /**
     * Returns the players with their corresponding scores for
     * the point in the tournament that this ranking represents.
     * It is guaranteed that no two players' scores will be the
     * same (though this may be due solely to differences in the
     * initial seeding).
     */
    ImmutableSortedSet<PlayerScore> getScores();

    /**
     * Returns the players in the order of the current ranking,
     * with the best-performing player in the 0 index.
     */
    ImmutableList<Player> getPlayersBestFirst();
}