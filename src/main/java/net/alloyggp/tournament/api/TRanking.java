package net.alloyggp.tournament.api;

import java.util.List;
import java.util.SortedSet;

public interface TRanking {
    /**
     * Returns the players with their corresponding scores for
     * the point in the tournament that this ranking represents.
     * It is guaranteed that no two players' scores will be the
     * same (though this may be due solely to differences in the
     * initial seeding).
     */
    SortedSet<TPlayerScore> getScores();

    /**
     * Returns the players in the order of this ranking,
     * with the best-performing player in the 0 index.
     */
    List<TPlayer> getPlayersBestFirst();

    /**
     * Returns the 1-indexed position of the player in the ranking,
     * such that the best-ranked player returns 1 and worse-ranked
     * players return incrementally higher values.
     *
     * <p>No two players will return the same position (though this
     * may be due solely to differences in the initial seeding).
     */
    int getPosition(TPlayer player);
}