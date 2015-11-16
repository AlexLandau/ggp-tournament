package net.alloyggp.tournament;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;

/**
 * This is a fuzz test for the following invariant:
 *
 * <p>Every set of standings returned includes all participants in
 * the tournament.
 */
@RunWith(Parameterized.class)
public class StandingsIncludeAllPlayersTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return FuzzTests.getParameters();
    }

    private final int numPlayers;
    private final String testSpec;

    public StandingsIncludeAllPlayersTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testStandingsIncludeAllPlayers() {
        TTournament spec = TestSpecs.load(testSpec);
        for (long seed = 0L; seed < 100L; seed++) {
            Random random = new Random(seed);
            TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
            TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
            verify(status.getStandingsHistory(), status.getCurrentStandings());
            while (true) {
                Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
                if (nextMatches.isEmpty()) {
                    break;
                }
                //Pick one and choose a result for it
                TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                TMatchResult result = FuzzTests.getResult(random, matchToResolve);
                status = status.withNewResult(result);
                verify(status.getStandingsHistory(), status.getCurrentStandings());
            }
        }
    }

    private void verify(List<TRanking> standingsHistory, TRanking currentStandings) {
        Assert.assertEquals(numPlayers, currentStandings.getPlayersBestFirst().size());
        Assert.assertEquals(numPlayers, currentStandings.getScores().size());
        for (TRanking ranking : standingsHistory) {
            Assert.assertEquals(numPlayers, ranking.getPlayersBestFirst().size());
            Assert.assertEquals(numPlayers, ranking.getScores().size());
        }
    }
}
