package net.alloyggp.tournament;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchResult.Outcome;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;

/**
 * This is a fuzz test for the following property of single-elimination
 * tournaments:
 *
 * <p>A player that never scores below 50 (i.e. a draw) in any game in a
 * single-elimination tournament should end up winning the tournament.
 */
@RunWith(Parameterized.class)
public class SingleEliminationVictoryTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return Iterables.filter(FuzzTests.getParameters(),
                new Predicate<Object[]>() {
                    @Override
                    public boolean apply(@Nonnull Object[] arguments) {
                        String testSpec = (String) arguments[1];
                        return testSpec.startsWith("singleElim");
                    }
                });
    }

    private final int numPlayers;
    private final String testSpec;

    public SingleEliminationVictoryTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testNonLosingPlayerAlwaysWins() {
        TTournament spec = TestSpecs.load(testSpec);
        for (long seed = 0L; seed < 100L; seed++) {
            List<TMatchResult> allResults = Lists.newArrayList();
            try {
                Random random = new Random(seed);
                TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
                TPlayer chosenPlayer = FuzzTests.pickAtRandom(random, initialSeeding.getPlayersBestFirst());
                System.out.println("Chosen player: " + chosenPlayer);
                TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
                while (true) {
                    Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
                    if (nextMatches.isEmpty()) {
                        break;
                    }
                    //Pick one and choose a result for it
                    TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                    TMatchResult result = FuzzTests.getResult(random, matchToResolve);
                    while (playerLosingInResult(chosenPlayer, matchToResolve, result)) {
                        result = FuzzTests.getResult(random, matchToResolve);
                    }
                    allResults.add(result);
                    System.out.println(matchToResolve);
                    System.out.println("Result: " + result);
                    status = status.withNewResult(result);
                }

                System.out.println(status.getCurrentStandings().getScores());
                assertEquals(chosenPlayer, status.getCurrentStandings().getPlayersBestFirst().get(0));
            } catch (Exception | AssertionError e) {
                System.out.println(allResults.toString().replaceAll(", ", "\n"));
                throw new RuntimeException("Seed was " + seed, e);
            }
        }
    }

    private boolean playerLosingInResult(TPlayer chosenPlayer, TMatchSetup matchToResolve, TMatchResult result) {
        if (result.getOutcome() == Outcome.ABORTED) {
            return false;
        }
        if (matchToResolve.getPlayers().get(0).equals(chosenPlayer)) {
            return result.getGoals().get(0) < 50;
        }
        if (matchToResolve.getPlayers().get(1).equals(chosenPlayer)) {
            return result.getGoals().get(1) < 50;
        }
        return false;
    }
}
