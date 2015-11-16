package net.alloyggp.tournament;

import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;

/**
 * This is a fuzz test for the following invariant:
 *
 * <p>Once a match setup has been returned, adding additional match results
 * to the tournament state will not cause that match setup to disappear
 * or alter in future returned sets of match setups.
 */
@RunWith(Parameterized.class)
public class AssignedMatchesConsistencyTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return FuzzTests.getParameters();
    }

    private final int numPlayers;
    private final String testSpec;

    public AssignedMatchesConsistencyTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testMatchSetupsDoNotDisappear() {
        TTournament spec = TestSpecs.load(testSpec);
        for (long seed = 0L; seed < 100L; seed++) {
            Random random = new Random(seed);
            TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
            TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
            Set<TMatchSetup> matchesAlreadyProposed = Sets.newHashSet();
            while (true) {
                Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
                if (!nextMatches.containsAll(matchesAlreadyProposed)) {
                    Assert.fail("With seed " + seed + ", some match setups appeared and"
                            + " then disappeared without receiving results: "
                            + Sets.difference(matchesAlreadyProposed, nextMatches));
                }
                if (nextMatches.isEmpty()) {
                    break;
                }
                matchesAlreadyProposed.addAll(nextMatches);
                //Pick one and choose a result for it
                TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                matchesAlreadyProposed.remove(matchToResolve);
                TMatchResult result = FuzzTests.getResult(random, matchToResolve);
                status = status.withNewResult(result);
            }
        }
    }

}
