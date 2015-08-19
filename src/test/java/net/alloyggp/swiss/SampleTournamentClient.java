package net.alloyggp.swiss;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.Player;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.TournamentSpec;
import net.alloyggp.swiss.api.TournamentStandings;
import net.alloyggp.swiss.api.TournamentStatus;

/*
 * As much as anything else, this is to help me get a better understanding of what
 * the API should look like from a client's perspective...
 */
public class SampleTournamentClient {
	@Test
	public void testSingleElimination() {
		TournamentSpec spec = TournamentSpec.parse(new File("singleElim.yaml"));
		Seeding initialSeeding = toSeeding("1", "2", "3", "4", "5");
		TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
		//Run matches until exhaustion...
		while (!status.isComplete()) {
			Set<MatchSetup> nextMatches = status.getNextMatchesToRun();
			List<MatchResult> results = getRandomOutcomes(nextMatches);
			status = status.withNewResults(results);
		}
		TournamentStandings standings = status.getStandings();
		System.out.println("Standings are: " + standings);
	}

	private Seeding toSeeding(String... playerNames) {
		List<Player> playersBestFirst = Arrays.stream(playerNames)
				.map(Player::create)
				.collect(Collectors.toList());
		return Seeding.create(playersBestFirst);
	}

	private List<MatchResult> getRandomOutcomes(Set<MatchSetup> nextMatches) {
		return nextMatches.stream()
		.map(setup -> MatchResult.getSuccessfulMatchResult(setup, getRandomGoals(), ImmutableList.of()))
		.collect(Collectors.toList());
	}

	private static final Random RAND = new Random();
	private List<Integer> getRandomGoals() {
		if (RAND.nextBoolean()) {
			return ImmutableList.of(100, 0);
		} else {
			return ImmutableList.of(0, 100);
		}
	}
}
