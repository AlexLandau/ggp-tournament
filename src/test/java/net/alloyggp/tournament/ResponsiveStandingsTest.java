package net.alloyggp.tournament;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;
import net.alloyggp.tournament.internal.MatchIds;
import net.alloyggp.tournament.internal.spec.StageSpec;
import net.alloyggp.tournament.internal.spec.TournamentSpec;

/**
 * This is a fuzz test for the following properties of most
 * tournament formats:
 *
 * <p>When a player gets 100 in every match in a round, they
 * will not go down in the standings, unless they were newly
 * excluded.
 * <p>When a player gets 0 in every match in a round, they
 * will not go up in the standings, unless it's to pass a
 * newly excluded player.
 */
@RunWith(Parameterized.class)
public class ResponsiveStandingsTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return FuzzTests.getParameters();
    }

    private final int numPlayers;
    private final String testSpec;

    public ResponsiveStandingsTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testStandingsIncludeAllPlayers() {
        TTournament spec = TestSpecs.load(testSpec);
        for (long seed = 0L; seed < 100L; seed++) {
            Random random = new Random(seed);
            TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
            TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
            TRanking currentStandings = status.getCurrentStandings();
            int curNumStandings = status.getStandingsHistory().size();

            Set<TPlayer> hadResults = Sets.newHashSet();
            Set<TPlayer> hadOnly100s = Sets.newHashSet(initialSeeding.getPlayersBestFirst());
            Set<TPlayer> hadOnly0s = Sets.newHashSet(initialSeeding.getPlayersBestFirst());

            while (true) {
                Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
                if (nextMatches.isEmpty()) {
                    break;
                }
                //Pick one and choose a result for it
                TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
                TMatchResult result = FuzzTests.getResult(random, matchToResolve);
                if (!result.wasAborted()) {
                    for (int r = 0; r < matchToResolve.getPlayers().size(); r++) {
                        TPlayer player = matchToResolve.getPlayers().get(r);
                        hadResults.add(player);
                        int goal = result.getGoals().get(r);
                        if (goal != 100) {
                            hadOnly100s.remove(player);
                        }
                        if (goal != 0) {
                            hadOnly0s.remove(player);
                        }
                    }
                }
                status = status.withNewResult(result);
                //TODO: Simplify/reconcile these?
                TRanking newStandings = status.getCurrentStandings();
                int newNumStandings = status.getStandingsHistory().size();
                if (curNumStandings != newNumStandings) {
                    //Just finished a round
                    Set<TPlayer> newlyExcludedPlayers = getCurrentExcludedPlayers(status);
                    verify(currentStandings, newStandings, hadResults, hadOnly100s, hadOnly0s, newlyExcludedPlayers);
                    currentStandings = newStandings;
                    curNumStandings = newNumStandings;

                    hadResults = Sets.newHashSet();
                    hadOnly100s = Sets.newHashSet(initialSeeding.getPlayersBestFirst());
                    hadOnly0s = Sets.newHashSet(initialSeeding.getPlayersBestFirst());
                }
            }
        }
    }

    private Set<TPlayer> getCurrentExcludedPlayers(TTournamentStatus status) {
        TNextMatchesResult nextMatchesToRun = status.getNextMatchesToRun();
        if (nextMatchesToRun.getMatchesToRun().isEmpty()) {
            return ImmutableSet.of();
        }
        TMatchSetup matchSetup = nextMatchesToRun.getMatchesToRun().iterator().next();
        int stageNumber = MatchIds.parseStageNumber(matchSetup.getMatchId());
        TournamentSpec tournament = (TournamentSpec) status.getSpec();
        StageSpec stageSpec = tournament.getStages().get(stageNumber);
        return stageSpec.getExcludedPlayers();
    }

    private void verify(TRanking oldStandings, TRanking newStandings,
            Set<TPlayer> hadResults, Set<TPlayer> hadOnly100s,
            Set<TPlayer> hadOnly0s, Set<TPlayer> newlyExcludedPlayers) {
        for (TPlayer playerWith100s : Sets.intersection(hadOnly100s, hadResults)) {
            int oldRanking = oldStandings.getPlayersBestFirst().indexOf(playerWith100s);
            int newRanking = newStandings.getPlayersBestFirst().indexOf(playerWith100s);

            if (newRanking > oldRanking && !newlyExcludedPlayers.contains(playerWith100s)) {
                Assert.fail("Player fell in rankings despite only getting 100s");
            }
        }
        for (TPlayer playerWith0s : Sets.intersection(hadOnly0s, hadResults)) {
            List<TPlayer> oldStandingsAdjustedForLeavers = Lists.newArrayList();
            for (TPlayer player : oldStandings.getPlayersBestFirst()) {
                if (!newlyExcludedPlayers.contains(player)) {
                    oldStandingsAdjustedForLeavers.add(player);
                }
            }
            int oldRankingAdjustedForLeavers = oldStandingsAdjustedForLeavers.indexOf(playerWith0s);
            int newRanking = newStandings.getPlayersBestFirst().indexOf(playerWith0s);

            if (newRanking != -1 && newRanking < oldRankingAdjustedForLeavers) {
                Assert.fail("Player rose in rankings despite only getting 0s");
            }
        }
    }

}
