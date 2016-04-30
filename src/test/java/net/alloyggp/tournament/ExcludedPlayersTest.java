package net.alloyggp.tournament;

import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;
import net.alloyggp.tournament.internal.MatchIds;
import net.alloyggp.tournament.internal.spec.StageSpec;
import net.alloyggp.tournament.internal.spec.TournamentSpec;

/**
 * This is a fuzz test for the following invariant:
 *
 * <p>A player that has been listed as excluded from a
 * stage will not play any matches in that stage or any
 * subsequent stage.
 */
@RunWith(Parameterized.class)
public class ExcludedPlayersTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return FuzzTests.getParameters();
    }

    private final int numPlayers;
    private final String testSpec;

    public ExcludedPlayersTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testPlayersExcludedFromStages() {
        TTournament spec = TestSpecs.load(testSpec);
        Assume.assumeTrue(containsPlayerExclusions(spec));
        SetMultimap<Integer, TPlayer> playersExcludedByRound = getPlayersExcludedByRound(spec);
        for (long seed = 0L; seed < 100L; seed++) {
            testWithSeed(spec, playersExcludedByRound, seed);
        }
    }

    private SetMultimap<Integer, TPlayer> getPlayersExcludedByRound(
            TTournament spec) {
        SetMultimap<Integer, TPlayer> cumulativeExcludedPlayers = HashMultimap.create();
        //We want the sets in the map to be cumulative: all players specified
        //in any round up to this point
        Set<TPlayer> playersExcludedSoFar = Sets.newHashSet();
        for (StageSpec stage : ((TournamentSpec) spec).getStages()) {
            playersExcludedSoFar.addAll(stage.getExcludedPlayers());
            cumulativeExcludedPlayers.putAll(stage.getStageNum(), playersExcludedSoFar);
        }
        return cumulativeExcludedPlayers;
    }

    private void testWithSeed(TTournament spec, SetMultimap<Integer, TPlayer> playersExcludedByRound, long seed) {
        Random random = new Random(seed);
        TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
        TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
        while (true) {
            Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
            for (TMatchSetup setup : nextMatches) {
                int stageNumber = MatchIds.parseStageNumber(setup.getMatchId());
                Set<TPlayer> excludedPlayers = playersExcludedByRound.get(stageNumber);
                for (TPlayer player : setup.getPlayers()) {
                    if (excludedPlayers.contains(player)) {
                        Assert.fail("The player " + player + " should be excluded from stage " + stageNumber + ", but was not. "
                                + "Match setup was: " + setup);
                    }
                }
            }
            if (nextMatches.isEmpty()) {
                break;
            }
            //Pick one and choose a result for it
            TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
            TMatchResult result = FuzzTests.getResult(random, matchToResolve);
            status = status.withNewResult(result);
        }
    }

    private boolean containsPlayerExclusions(TTournament spec) {
        for (StageSpec stage : ((TournamentSpec) spec).getStages()) {
            if (!stage.getExcludedPlayers().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
