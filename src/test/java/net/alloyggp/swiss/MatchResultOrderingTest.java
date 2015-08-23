package net.alloyggp.swiss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.Player;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.TournamentSpec;
import net.alloyggp.swiss.api.TournamentSpecParser;
import net.alloyggp.swiss.api.TournamentStatus;

/**
 * This is a fuzz test for the following invariant:
 *
 * The order in which match results are submitted to the scheduler should
 * have no effect on the matches scheduled or the outcome of the tournament,
 * as long as the contents of the results are the same.
 */
@RunWith(Parameterized.class)
public class MatchResultOrderingTest {
	@Parameters(name = "{index}: {0} players, {1}")
	public static Iterable<Object[]> data() {
		return FuzzTests.getParameters();
	}

	private final int numPlayers;
	private final File yamlFile;

	public MatchResultOrderingTest(int numPlayers, File yamlFile) {
		this.numPlayers = numPlayers;
		this.yamlFile = yamlFile;
	}

	@Test
	public void testMatchSetupsDoNotDisappear() {
		TournamentSpec spec = TournamentSpecParser.parse(yamlFile);
		for (long seed = 0L; seed < 100L; seed++) {
			Random random = new Random(seed);
			Seeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
			TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
			Map<MatchSetup, MatchResult> resultsChosen = Maps.newHashMap();
			while (true) {
				//TODO: Make these return a List or SortedSet or something?
				Set<MatchSetup> nextMatches = status.getNextMatchesToRun();
				if (nextMatches.isEmpty()) {
					break;
				}
				//Pick one and choose a result for it
				MatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
				MatchResult result = FuzzTests.getResult(random, matchToResolve);
				resultsChosen.put(matchToResolve, result);
				status = status.withNewResult(result);
			}
			List<Player> finalStandings = status.getStandings().getPlayersBestFirst();

			//Now do the second run through. Reset the status...
			status = TournamentStatus.getInitialStatus(spec, initialSeeding);
			while (true) {
				Set<MatchSetup> nextMatches = status.getNextMatchesToRun();
				if (!resultsChosen.keySet().containsAll(nextMatches)) {
					fail("We have at least one match setup returned this time that wasn't returned last "
							+ "time: " + Sets.difference(nextMatches, resultsChosen.keySet()));
				}
				if (nextMatches.isEmpty()) {
					break;
				}
				//We'll normally pick a different one this time
				MatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
				MatchResult result = resultsChosen.get(matchToResolve);
				status = status.withNewResult(result);
			}
			//And make sure the final standings are the same
			assertEquals(finalStandings, status.getStandings().getPlayersBestFirst());
		}
	}

}
