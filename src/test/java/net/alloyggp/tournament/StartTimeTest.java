package net.alloyggp.tournament;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TAdminAction;
import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;

public class StartTimeTest {
    private static final List<TAdminAction> NO_ACTIONS = ImmutableList.of();

    @Test
    public void testSingleElimFourPlayerStartTime() {
        testSingleElimHasStartTime(4);
    }

    @Test
    public void testSingleElimThreePlayerStartTime() {
        testSingleElimHasStartTime(3);
    }

    @Test
    public void testSingleElimFivePlayerStartTime() {
        testSingleElimHasStartTime(5);
    }

    @Test
    public void testSingleElimEightPlayerStartTime() {
        testSingleElimHasStartTime(8);
    }

    @Test
    public void testSingleElimTwoPlayerStartTime() {
        testSingleElimHasStartTime(2);
    }

    private void testSingleElimHasStartTime(int numPlayers) {
        TTournament tournament = TestSpecs.load("singleElim");
        assertTrue(tournament.getInitialStartTime(NO_ACTIONS).isPresent());

        Random random = new Random(0L);
        TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
        TNextMatchesResult matchesToRun = tournament.getMatchesToRun(initialSeeding, ImmutableSet.<TMatchResult>of(), NO_ACTIONS);
        assertTrue(matchesToRun.getEarliestAllowedStartTime().isPresent());
    }

    @Test
    public void testSwiss1Test1StartTime() {
        TTournament tournament = TestSpecs.load("swiss1test1");

        Random random = new Random(0L);
        TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, 2);
        Set<TMatchResult> matchResults = Sets.newHashSet();

        //Round 1: no start time yet
        TNextMatchesResult matchesToRun = tournament.getMatchesToRun(initialSeeding, matchResults, NO_ACTIONS);
        assertFalse(matchesToRun.getEarliestAllowedStartTime().isPresent());
        matchResults.add(finishOnlyMatch(matchesToRun.getMatchesToRun()));

        //Round 2: start time exists
        matchesToRun = tournament.getMatchesToRun(initialSeeding, matchResults, NO_ACTIONS);
        assertTrue(matchesToRun.getEarliestAllowedStartTime().isPresent());
        matchResults.add(finishOnlyMatch(matchesToRun.getMatchesToRun()));

        //Round 3: still exists
        matchesToRun = tournament.getMatchesToRun(initialSeeding, matchResults, NO_ACTIONS);
        assertTrue(matchesToRun.getEarliestAllowedStartTime().isPresent());
    }

    private TMatchResult finishOnlyMatch(ImmutableSet<TMatchSetup> matchesToRun) {
        TMatchSetup matchSetup = Iterables.getOnlyElement(matchesToRun);
        return TMatchResult.getSuccessfulMatchResult(
                matchSetup.getMatchId(),
                ImmutableList.of(100, 0));
    }

    //TODO: Test in multi-stage tournament
}
