package net.alloyggp.swiss;

import java.io.File;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;

import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.RoundSpec;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.StageFormat;
import net.alloyggp.swiss.api.StageSpec;
import net.alloyggp.swiss.api.TournamentSpec;
import net.alloyggp.swiss.api.TournamentSpecParser;
import net.alloyggp.swiss.api.TournamentStatus;

/**
 * This is a fuzz test for the following invariant:
 *
 * <p>By the time no further matches are returned, at least one
 * match has been run in each stage/round pair of the tournament
 * description (under certain conditions).
 */
@RunWith(Parameterized.class)
public class AllRoundsAreRunTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return FuzzTests.getParameters();
    }

    private final int numPlayers;
    private final File yamlFile;

    public AllRoundsAreRunTest(int numPlayers, File yamlFile) {
        this.numPlayers = numPlayers;
        this.yamlFile = yamlFile;
    }

    @Test
    public void test() {
        TournamentSpec spec = TournamentSpecParser.parse(yamlFile);
        for (long seed = 0L; seed < 100L; seed++) {
            Random random = new Random(seed);
            Seeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
            TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
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
            }

            for (StageSpec stage : spec.getStages()) {
                int numRounds;
                if (stage.getFormat() == StageFormat.SINGLE_ELIMINATION) {
                    numRounds = getNumRoundsSingleElim(numPlayers);
                    for (int i = 1; i <= numRounds; i++) {
                        checkResultExists(status.getResultsSoFar(), stage.getStageNum(), i);
                    }
                } else {
                    numRounds = stage.getRounds().size();
                    for (int i = 0; i < numRounds; i++) {
                        //There might not be enough players to run the round, in which case,
                        //make sure we still get to later rounds
                        int minNumPlayers = getMinNumPlayers(stage.getRounds().get(i));
                        if (minNumPlayers <= numPlayers) {
                            checkResultExists(status.getResultsSoFar(), stage.getStageNum(), i);
                        }
                    }
                }
            }
        }
    }

    private int getMinNumPlayers(RoundSpec roundSpec) {
        return roundSpec.getMatches().stream()
            .mapToInt(match -> match.getGame().getNumRoles())
            .min().getAsInt();
    }

    private void checkResultExists(ImmutableList<MatchResult> resultsSoFar, int stageNum, int roundNum) {
        for (MatchResult result : resultsSoFar) {
            String matchId = result.getSetup().getMatchId();
            if (MatchIds.parseStageNumber(matchId) == stageNum
                    && MatchIds.parseRoundNumber(matchId) == roundNum) {
                return;
            }
        }
        Assert.fail("No result exists with stage num " + stageNum + " and round num " + roundNum);
    }

    private int getNumRoundsSingleElim(int numPlayers) {
        int numRounds = 0;
        int num = numPlayers;
        while (num > 1) {
            num = num >> 1;
            numRounds++;
        }
        if (numPlayers > (1 << numRounds)) {
            numRounds++;
        }
        return numRounds;
    }
}
