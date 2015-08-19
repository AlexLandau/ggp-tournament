package net.alloyggp.swiss.api;

import java.util.Collection;

import com.google.common.collect.ImmutableSortedSet;

//TODO: Rename to Leaderboard? Maybe? Nah?
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
}
