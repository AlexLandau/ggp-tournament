package net.alloyggp.tournament;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;
import net.alloyggp.tournament.internal.StandardRanking;

/**
 * This is a fuzz test for the following invariant:
 *
 * <p>Once a set of standings has appeared in the history, adding
 * additional match results to the tournament state will not cause
 * that set of standings to disappear or alter in future returned
 * histories.
 */
@RunWith(Parameterized.class)
public class StandingsConsistencyTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return FuzzTests.getParameters();
    }

    private final int numPlayers;
    private final String testSpec;

    public StandingsConsistencyTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testStandingsHistoryNotRewritten() {
        TTournament spec = TestSpecs.load(testSpec);
        for (long seed = 0L; seed < 100L; seed++) {
            Random random = new Random(seed);
            TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
            TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
            List<TRanking> standingsSoFar = Lists.newArrayList();
            standingsSoFar.add(StandardRanking.createForSeeding(initialSeeding));
            verifyAndAddStandingsHistory(standingsSoFar, status.getStandingsHistory(), status.getCurrentStandings());
            while (true) {
                Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
                if (nextMatches.isEmpty()) {
                    break;
                }
                //Pick one and choose a result for it
                TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                TMatchResult result = FuzzTests.getResult(random, matchToResolve);
                status = status.withNewResult(result);
                verifyAndAddStandingsHistory(standingsSoFar, status.getStandingsHistory(), status.getCurrentStandings());
            }
        }
    }

    private void verifyAndAddStandingsHistory(List<TRanking> standingsSoFar,
            List<TRanking> standingsHistory, TRanking currentStandings) {
        assertEquals(standingsSoFar, standingsHistory.subList(0, standingsSoFar.size()));
        //TODO: Have this be correct
//        assertEquals(currentStandings.toString(), standingsHistory.get(standingsHistory.size() - 1).toString());
        if (standingsHistory.size() > standingsSoFar.size()) {
            assertEquals(1, standingsHistory.size() - standingsSoFar.size());
            TRanking newStandings = standingsHistory.get(standingsHistory.size() - 1);
            standingsSoFar.add(newStandings);
        }
    }
}
