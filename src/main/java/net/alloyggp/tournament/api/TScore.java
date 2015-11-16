package net.alloyggp.tournament.api;

import javax.annotation.concurrent.Immutable;

/**
 * Represents a player's aggregate score at some point in the tournament.
 * This is used to turn match results into {@link TRanking}s.
 * The implementation may contain details for scoring that differ
 * by tournament format, and so is private.
 */
@Immutable
public interface TScore extends Comparable<TScore> {
    /**
     * Returns a human-readable description of the score that explains
     * the player's current position in the ranking.
     */
    String getDescription();
}
