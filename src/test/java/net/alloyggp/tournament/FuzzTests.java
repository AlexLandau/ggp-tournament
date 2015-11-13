package net.alloyggp.tournament;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.Game;
import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.Seeding;

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
                new Object[] {6, "singleElim"},
                new Object[] {8, "singleElim"},
                new Object[] {7, "singleElimTwoStages"},
                new Object[] {8, "singleElimTwoStages"},
                new Object[] {3, "swiss1SingleElimTest1"},
                new Object[] {4, "swiss1SingleElimTest1"},
                new Object[] {7, "swiss1SingleElimTest1"},
                new Object[] {8, "swiss1SingleElimTest1"},
                new Object[] {10, "swiss1test1"},
                new Object[] {9, "swiss1test1"},
                new Object[] {10, "swiss1test2"},
                new Object[] {9, "swiss1test2"},
                new Object[] {10, "swiss1test3"},
                new Object[] {9, "swiss1test3"},
                new Object[] {10, "swiss1test4"},
                new Object[] {9, "swiss1test4"},
                new Object[] {3, "swiss1test4"},
                new Object[] {10, "swiss1test5"},
                new Object[] {9, "swiss1test5"},
                new Object[] {3, "swiss1test5"},
                new Object[] {6, "swiss1test6"},
                new Object[] {7, "swiss1test6"},
                new Object[] {7, "swiss1test7"},
                new Object[] {8, "swiss1test7"}
                );
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
            return MatchResult.getAbortedMatchResult(match.getMatchId(), match.getPlayers());
        }
        List<Integer> goals = getGoals(random, match.getGame());
        return MatchResult.getSuccessfulMatchResult(match.getMatchId(), match.getPlayers(), goals);
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
        return pickAtRandom(random, sortedMatches);
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

    public static <T> T pickAtRandom(Random random, List<T> list) {
        int chosenIndex = random.nextInt(list.size());
        return list.get(chosenIndex);
    }

}
