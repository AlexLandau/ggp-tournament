package net.alloyggp.tournament;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;
import net.alloyggp.tournament.internal.MatchIds;

@RunWith(Parameterized.class)
public class AdminActionMatchIdsTest {
    @Parameters(name = "{index}: {0} players, {1}, round {2}")
    public static Iterable<Object[]> data() {
        return ImmutableList.of(
                new Object[] {4, "swiss2test2", 5},
                new Object[] {4, "singleElim", 1}
                );
    }

    private final int numPlayers;
    private final String testSpec;
    private final int roundNum;

    public AdminActionMatchIdsTest(int numPlayers, String testSpec, int roundNum) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
        this.roundNum = roundNum;
    }

    @Test
    public void testMatchIdsBeforeRoundAreDifferent() {
        TTournament spec = TestSpecs.load(testSpec);

        Random random = new Random(0L);
        TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
        TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);

        Set<TMatchSetup> originalMatchesToRun = status.getNextMatchesToRun().getMatchesToRun();

        assertFalse(status.getNextMatchesToRun().getMatchesToRun().isEmpty());
        //Apply an action for some later round
        status = status.apply(TestAdminAction.create(0, roundNum));

        assertEquals(originalMatchesToRun, status.getNextMatchesToRun().getMatchesToRun());
    }

    @Test
    public void testMatchIdsAfterRoundAreDifferent() {
        TTournament spec = TestSpecs.load(testSpec);

        Random random = new Random(0L);
        TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
        TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
        List<TMatchResult> matchResults = Lists.newArrayList();
        Set<String> originalMatchIds = Sets.newHashSet();
        while (true) {
            Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
            for (TMatchSetup setup : nextMatches) {
                originalMatchIds.add(setup.getMatchId());
            }
            if (nextMatches.isEmpty()) {
                break;
            }
            //Pick one and choose a result for it
            TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
            TMatchResult result = FuzzTests.getResult(random, matchToResolve);
            matchResults.add(result);
            status = status.withNewResult(result);
        }

        assertTrue(status.getNextMatchesToRun().getMatchesToRun().isEmpty());
        //Apply an action
        status = status.apply(TestAdminAction.create(0, roundNum));
        assertFalse(status.getNextMatchesToRun().getMatchesToRun().isEmpty());

        for (TMatchSetup setup : status.getNextMatchesToRun().getMatchesToRun()) {
            //Match IDs should not be from the original set of match IDs
            assertFalse(originalMatchIds.contains(setup.getMatchId()));
            assertTrue(setup.getMatchId().startsWith("ggpta-1-"));
            //Match IDs should be from the round that was affected
            assertEquals(roundNum, MatchIds.parseRoundNumber(setup.getMatchId()));
        }
    }
}
