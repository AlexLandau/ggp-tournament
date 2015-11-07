package net.alloyggp.tournament;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.NextMatchesResult;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.api.Tournament;

public class StartTimeTest {
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
        Tournament tournament = TestSpecs.load("singleElim");

        Random random = new Random(0L);
        Seeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
        NextMatchesResult matchesToRun = tournament.getMatchesToRun(initialSeeding, ImmutableSet.of());
        assertTrue(matchesToRun.getEarliestAllowedStartTime().isPresent());
    }

    @Test
    public void testSwiss1Test1StartTime() {
        Tournament tournament = TestSpecs.load("swiss1test1");

        Random random = new Random(0L);
        Seeding initialSeeding = FuzzTests.createRandomSeeding(random, 2);
        Set<MatchResult> matchResults = Sets.newHashSet();

        //Round 1: no start time yet
        NextMatchesResult matchesToRun = tournament.getMatchesToRun(initialSeeding, matchResults);
        assertFalse(matchesToRun.getEarliestAllowedStartTime().isPresent());
        matchResults.add(finishOnlyMatch(matchesToRun.getMatchesToRun()));

        //Round 2: start time exists
        matchesToRun = tournament.getMatchesToRun(initialSeeding, matchResults);
        assertTrue(matchesToRun.getEarliestAllowedStartTime().isPresent());
        matchResults.add(finishOnlyMatch(matchesToRun.getMatchesToRun()));

        //Round 3: still exists
        matchesToRun = tournament.getMatchesToRun(initialSeeding, matchResults);
        assertTrue(matchesToRun.getEarliestAllowedStartTime().isPresent());
    }

    private MatchResult finishOnlyMatch(ImmutableSet<MatchSetup> matchesToRun) {
        return MatchResult.getSuccessfulMatchResult(
                Iterables.getOnlyElement(matchesToRun),
                ImmutableList.of(100, 0));
    }

    //TODO: Test in multi-stage tournament
}
