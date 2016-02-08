package net.alloyggp.tournament;

import static org.junit.Assert.fail;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;

/**
 * This is a fuzz test for the following property of Swiss tournaments:
 *
 * <p>A player that does not appear in any matches in a round (i.e. has a bye)
 * of a fixed-sum game does not fall in the standings after that round.
 *
 * <p>This is a regression test for a bug from the swiss1 format.
 */
@RunWith(Parameterized.class)
public class SwissByeTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return Iterables.filter(FuzzTests.getParameters(),
                new Predicate<Object[]>() {
                    @Override
                    public boolean apply(@Nonnull Object[] arguments) {
                        String testSpec = (String) arguments[1];
                        return testSpec.startsWith("swiss")
                                //This is a bug in the stable swiss1 format.
                                && !testSpec.startsWith("swiss1");
                    }
                });
    }

    private final int numPlayers;
    private final String testSpec;

    public SwissByeTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testNonLosingPlayerAlwaysWins() {
        TTournament spec = TestSpecs.load(testSpec);
        for (long seed = 0L; seed < 100L; seed++) {
            try {
                Random random = new Random(seed);
                TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
                TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
                Set<TPlayer> playersInRound = Sets.newHashSet();
                boolean nonFixedSumGamePlayed = false;
                TRanking standings = status.getCurrentStandings();
                while (true) {
                    Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
                    if (nextMatches.isEmpty()) {
                        break;
                    }

                    //To keep this deterministic, we need to go through the matches in sorted order
                    SortedSet<TMatchSetup> sortedMatches = ImmutableSortedSet.copyOf(MatchSetups.COMPARATOR, nextMatches);
                    List<TMatchResult> results = Lists.newArrayList();
                    for (TMatchSetup match : sortedMatches) {
                        playersInRound.addAll(match.getPlayers());
                        nonFixedSumGamePlayed |= !match.getGame().isFixedSum();

                        results.add(FuzzTests.getResult(random, match));
                    }

                    status = status.withNewResults(results);

                    //Standings change when the round shifts
                    TRanking newStandings = status.getCurrentStandings();
                    if (!standings.equals(newStandings)) {
                        //Check that players with byes didn't go down in the rankings
                        //if it was a fixed-sum game
                        if (!nonFixedSumGamePlayed) {
                            for (TPlayer player : initialSeeding.getPlayersBestFirst()) {
                                if (!playersInRound.contains(player)) {
                                    int oldPosition = standings.getPosition(player);
                                    int newPosition = newStandings.getPosition(player);
                                    //Lower numbers are better
                                    if (newPosition > oldPosition) {
                                        fail("Player " + player + " fell from " + oldPosition + " to " + newPosition + " despite having a bye");
                                    }
                                }
                            }
                        }

                        //Reset the players we examine
                        nonFixedSumGamePlayed = false;
                        playersInRound.clear();
                        standings = newStandings;
                    }
                }

            } catch (Exception | AssertionError e) {
                throw new RuntimeException("Seed was " + seed, e);
            }
        }
    }

}
