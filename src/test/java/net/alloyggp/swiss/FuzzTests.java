package net.alloyggp.swiss;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.alloyggp.swiss.api.Game;
import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.Player;
import net.alloyggp.swiss.api.Seeding;

/**
 * Contains utilities for writing fuzz tests that use a single random seed for all
 * divergence in their behavior. To keep test failures reproducible, these tests
 * should have deterministic behavior if their seed is kept constant.
 */
public class FuzzTests {
    private FuzzTests() {
        //Not instantiable
    }

    /**
     * Returns the combinations of player counts and tournament specification files that
     * are used for parameterized fuzz testing across different tournament types.
     */
    public static Iterable<Object[]> getParameters() {
        return ImmutableList.of(
                new Object[] {6, testSpecFile("singleElim.yaml")},
                new Object[] {8, testSpecFile("singleElim.yaml")},
                new Object[] {7, testSpecFile("singleElimTwoStages.yaml")},
                new Object[] {8, testSpecFile("singleElimTwoStages.yaml")}
                );
    }

    private static File testSpecFile(String filename) {
        return new File(new File("testSpecs"), filename);
    }

    /**
     * Returns a randomized result for the given match, using randomness from the
     * supplied {@link Random} object.
     *
     * <p>The match result may be either aborted or successful. If the game is
     * zero-sum, the goal values will reflect that.
     */
    public static MatchResult getResult(Random random, MatchSetup match) {
        if (random.nextDouble() > 0.7) {
            return MatchResult.getAbortedMatchResult(match, Lists.newArrayList());
        }
        List<Integer> goals = getGoals(random, match.getGame());
        return MatchResult.getSuccessfulMatchResult(match, goals, Lists.newArrayList());
    }

    private static List<Integer> getGoals(Random random, Game game) {
        if (game.getNumRoles() == 1) {
            return ImmutableList.of(getOneGoalValue(random));
        } else if (game.getNumRoles() == 2) {
            if (game.isFixedSum()) {
                int goal = getOneGoalValue(random);
                return ImmutableList.of(goal, 100 - goal);
            } else {
                return ImmutableList.of(getOneGoalValue(random), getOneGoalValue(random));
            }
        } else {
            int numRoles = game.getNumRoles();
            if (game.isFixedSum()) {
                int winner = random.nextInt(numRoles);
                List<Integer> goals = Lists.newArrayList(Collections.nCopies(numRoles, 0));
                goals.set(winner, 100);
                return goals;
            } else {
                List<Integer> goals = Lists.newArrayList();
                for (int i = 0; i < numRoles; i++) {
                    goals.add(getOneGoalValue(random));
                }
                return goals;
            }
        }
    }

    private static int getOneGoalValue(Random random) {
        double roll = random.nextDouble();
        if (roll > 0.6) {
            return 100;
        } else if (roll > 0.2) {
            return 0;
        } else {
            return random.nextInt(101);
        }
    }

    /**
     * Selects one match from the given set at random, using the given source of
     * randomness.
     *
     * <p>This is necessary to ensure that fuzz tests work the same way every time
     * when their PRNG seed is the same. If the next random value is the same, the
     * match chosen will be the same. By contrast, selecting the first match from
     * the set iterator may give different results on different runs.
     */
    public static MatchSetup pickMatchAtRandom(Random random, Set<MatchSetup> nextMatches) {
        List<MatchSetup> sortedMatches = Lists.newArrayList(nextMatches);
        sortedMatches.sort(Comparator.comparing(MatchSetup::getMatchId));
        int chosenIndex = random.nextInt(sortedMatches.size());
        return sortedMatches.get(chosenIndex);
    }

    /**
     * Creates a random seeding with the given number of players using the given
     * source of randomness.
     *
     * <p>Players are named "1", "2", "3", etc.
     */
    public static Seeding createRandomSeeding(Random random, int numPlayers) {
        List<Player> players = IntStream.range(1, numPlayers + 1)
                .mapToObj(Integer::toString)
                .map(Player::create)
                .collect(Collectors.toList());
        return Seeding.createRandomSeeding(random, players);
    }

}
