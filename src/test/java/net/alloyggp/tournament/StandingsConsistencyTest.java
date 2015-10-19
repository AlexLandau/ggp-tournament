package net.alloyggp.tournament;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.api.Tournament;
import net.alloyggp.tournament.api.TournamentSpecParser;
import net.alloyggp.tournament.api.TournamentStatus;
import net.alloyggp.tournament.impl.StandardRanking;

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
    private final File yamlFile;

    public StandingsConsistencyTest(int numPlayers, File yamlFile) {
        this.numPlayers = numPlayers;
        this.yamlFile = yamlFile;
    }

    @Test
    public void testStandingsHistoryNotRewritten() {
        Tournament spec = TournamentSpecParser.parseYamlFile(yamlFile);
        for (long seed = 0L; seed < 100L; seed++) {
            Random random = new Random(seed);
            Seeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
            TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
            List<Ranking> standingsSoFar = Lists.newArrayList();
            standingsSoFar.add(StandardRanking.createForSeeding(initialSeeding));
            verifyAndAddStandingsHistory(standingsSoFar, status.getStandingsHistory());
            while (true) {
                //TODO: Make these return a List or SortedSet or something?
                Set<MatchSetup> nextMatches = status.getNextMatchesToRun();
                if (nextMatches.isEmpty()) {
                    break;
                }
                //Pick one and choose a result for it
                MatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                MatchResult result = FuzzTests.getResult(random, matchToResolve);
                status = status.withNewResult(result);
                verifyAndAddStandingsHistory(standingsSoFar, status.getStandingsHistory());
            }
        }
    }

    private void verifyAndAddStandingsHistory(List<Ranking> standingsSoFar,
            List<Ranking> standingsHistory) {
        System.out.println("Standings so far: " + standingsSoFar);
        System.out.println("Standings history: " + standingsHistory);
        assertEquals(standingsSoFar, standingsHistory.subList(0, standingsSoFar.size()));
        if (standingsHistory.size() > standingsSoFar.size()) {
            assertEquals(1, standingsHistory.size() - standingsSoFar.size());
            Ranking newStandings = standingsHistory.get(standingsHistory.size() - 1);
            standingsSoFar.add(newStandings);
        }
    }

}
