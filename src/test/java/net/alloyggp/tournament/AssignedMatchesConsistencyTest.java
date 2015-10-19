package net.alloyggp.tournament;

import java.io.File;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.api.Tournament;
import net.alloyggp.tournament.api.TournamentSpecParser;
import net.alloyggp.tournament.api.TournamentStatus;

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
    private final File yamlFile;

    public AssignedMatchesConsistencyTest(int numPlayers, File yamlFile) {
        this.numPlayers = numPlayers;
        this.yamlFile = yamlFile;
    }

    @Test
    public void testMatchSetupsDoNotDisappear() {
        Tournament spec = TournamentSpecParser.parseYamlFile(yamlFile);
        for (long seed = 0L; seed < 100L; seed++) {
            Random random = new Random(seed);
            Seeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
            TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
            Set<MatchSetup> matchesAlreadyProposed = Sets.newHashSet();
            while (true) {
                //TODO: Make these return a List or SortedSet or something?
                Set<MatchSetup> nextMatches = status.getNextMatchesToRun();
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
                MatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                matchesAlreadyProposed.remove(matchToResolve);
                MatchResult result = FuzzTests.getResult(random, matchToResolve);
                status = status.withNewResult(result);
            }
        }
    }

}
