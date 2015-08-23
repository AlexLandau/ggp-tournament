package net.alloyggp.swiss;

import java.io.File;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Maps;

import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.TournamentSpec;
import net.alloyggp.swiss.api.TournamentSpecParser;
import net.alloyggp.swiss.api.TournamentStatus;

/**
 * This is a fuzz test for the following invariant:
 *
 * Once a match setup has been proposed for a given match ID,
 * no other matches will use that match ID.
 */
@RunWith(Parameterized.class)
public class MatchIdUniquenessTest {
	@Parameters(name = "{index}: {0} players, {1}")
	public static Iterable<Object[]> data() {
		return FuzzTests.getParameters();
	}

	private final int numPlayers;
	private final File yamlFile;

	public MatchIdUniquenessTest(int numPlayers, File yamlFile) {
		this.numPlayers = numPlayers;
		this.yamlFile = yamlFile;
	}

	@Test
	public void testMatchSetupsHaveUniqueIds() {
		TournamentSpec spec = TournamentSpecParser.parse(yamlFile);
		for (long seed = 0L; seed < 100L; seed++) {
			testWithSeed(spec, seed);
		}
	}

	private void testWithSeed(TournamentSpec spec, long seed) {
		Random random = new Random(seed);
		Seeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
		TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
		Map<String, MatchSetup> matchSetupsById = Maps.newHashMap();
		while (true) {
			//TODO: Make these return a List or SortedSet or something?
			Set<MatchSetup> nextMatches = status.getNextMatchesToRun();
			for (MatchSetup setup : nextMatches) {
				String matchId = setup.getMatchId();
				if (matchSetupsById.containsKey(matchId)) {
					Assert.assertEquals("With seed "+seed+", suggested setups with the same match ID should be the same",
							matchSetupsById.get(matchId), setup);
				} else {
					matchSetupsById.put(matchId, setup);
				}
			}
			if (nextMatches.isEmpty()) {
				break;
			}
			//Pick one and choose a result for it
			MatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
			MatchResult result = FuzzTests.getResult(random, matchToResolve);
			status = status.withNewResult(result);
		}
	}
}
