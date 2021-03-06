package net.alloyggp.tournament;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;
import net.alloyggp.tournament.internal.runner.TournamentStateCache;

/**
 * This is a fuzz test for the following invariant:
 *
 * <p>Results returned are the same regardless of whether or
 * not caching is enabled.
 */
@RunWith(Parameterized.class)
public class CacheConsistencyTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return FuzzTests.getParameters();
    }

    private final int numPlayers;
    private final String testSpec;

    public CacheConsistencyTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testDisablingCacheDoesNotChangeResults() {
        TTournament spec = TestSpecs.load(testSpec);
        for (long seed = 0L; seed < 100L; seed++) {
            try {
                Random random = new Random(seed);
                TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
                TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
                Map<TMatchSetup, TMatchResult> resultsChosen = Maps.newHashMap();
                while (true) {
                    Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
                    if (nextMatches.isEmpty()) {
                        break;
                    }
                    //Pick one and choose a result for it
                    TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                    TMatchResult result = FuzzTests.getResult(random, matchToResolve);
                    resultsChosen.put(matchToResolve, result);
                    status = status.withNewResult(result);
                }
                List<TPlayer> finalStandings = status.getCurrentStandings().getPlayersBestFirst();
                List<TRanking> standingsHistory = status.getStandingsHistory();

                //Now do the second run through. Disable the cache...
                TournamentStateCache.setEnabled(false);
                try {
                    status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
                    while (true) {
                        Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
                        if (!resultsChosen.keySet().containsAll(nextMatches)) {
                            fail("We have at least one match setup returned this time that wasn't returned last "
                                    + "time: " + Sets.difference(nextMatches, resultsChosen.keySet()));
                        }
                        if (nextMatches.isEmpty()) {
                            break;
                        }
                        //We'll normally pick a different one this time
                        TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                        TMatchResult result = resultsChosen.get(matchToResolve);
                        status = status.withNewResult(result);
                    }
                    //And make sure the final standings are the same
                    assertEquals(finalStandings, status.getCurrentStandings().getPlayersBestFirst());
                    assertEquals(standingsHistory, status.getStandingsHistory());
                } finally {
                    TournamentStateCache.setEnabled(true);
                }
            } catch (Exception | AssertionError e) {
                throw new RuntimeException("Seed was " + seed, e);
            }
        }
    }
}
