package net.alloyggp.swiss.api;

/**
 * Represents a player's aggregate score at some point in the tournament.
 * This is used to turn match results into {@link TournamentStandings}.
 * The implementation may contain details for tiebreakers that differ
 * by tournament format, and so is private.
 */
public interface Score extends Comparable<Score> {

}
