package net.alloyggp.tournament;

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

import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.api.Tournament;
import net.alloyggp.tournament.api.TournamentSpecParser;
import net.alloyggp.tournament.api.TournamentStatus;
import net.alloyggp.tournament.impl.TournamentStateCache;

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
    private final File yamlFile;

    public CacheConsistencyTest(int numPlayers, File yamlFile) {
        this.numPlayers = numPlayers;
        this.yamlFile = yamlFile;
    }

    @Test
    public void testDisablingCacheDoesNotChangeResults() {
        Tournament spec = TournamentSpecParser.parseYamlFile(yamlFile);
        for (long seed = 0L; seed < 100L; seed++) {
            try {
                Random random = new Random(seed);
                Seeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
                TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
                Map<MatchSetup, MatchResult> resultsChosen = Maps.newHashMap();
                while (true) {
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
                List<Player> finalStandings = status.getCurrentStandings().getPlayersBestFirst();
                List<Ranking> standingsHistory = status.getStandingsHistory();

                //Now do the second run through. Disable the cache...
                TournamentStateCache.setEnabled(false);
                try {
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
