package net.alloyggp.tournament;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TAdminAction;
import net.alloyggp.tournament.api.TGame;
import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.MatchId;
import net.alloyggp.tournament.internal.admin.ReplaceGameAction;

/**
 * This tests in various ways that admin actions have their intended effects,
 * i.e. that they transform tournaments into their intended modified game
 * descriptions.
 */
@RunWith(Parameterized.class)
public class AdminActionModificationTest {
    private static final List<TAdminAction> NO_ACTIONS = ImmutableList.of();

    @Parameters(name = "{index}: {0} players, transforming {1} to {2}")
    public static Iterable<Object[]> data() {
        return ImmutableList.of(
                new Object[] {6, "swiss2SingleElimTest1", "swiss2SingleElimTestModifiedGames", getGameModifications()},
                new Object[] {7, "swiss2SingleElimTest1", "swiss2SingleElimTestModifiedGames", getGameModifications()}
                );
    }

    private static List<? extends TAdminAction> getGameModifications() {
        TGame game2 = Game.create("game2", "http://games.ggp.org/base/games/game2/", 2, true);
        TGame game4 = Game.create("game4", "http://games.ggp.org/base/games/game4/", 4, true);
        TGame curveball = Game.create("curveball", "http://games.ggp.org/base/games/curveball/", 2, true);
        return ImmutableList.of(
                ReplaceGameAction.create(0, 5, 0, game2),
                ReplaceGameAction.create(0, 8, 0, game4),
                ReplaceGameAction.create(0, 9, 0, game4),
                ReplaceGameAction.create(1, 1, 4, curveball)
                );
    }

    private final int numPlayers;
    private final String originalSpec;
    private final String newSpec;
    private final List<TAdminAction> actions;

    public AdminActionModificationTest(int numPlayers, String originalSpec, String newSpec,
            List<TAdminAction> actions) {
        this.numPlayers = numPlayers;
        this.originalSpec = originalSpec;
        this.newSpec = newSpec;
        this.actions = actions;
    }

    @Test
    public void testSameBehaviorWhenChangedImmediately() {
        TTournament originalTournament = TestSpecs.load(originalSpec);
        TTournament referenceTournament = TestSpecs.load(newSpec);

        for (int seed = 0; seed < 100; seed++) {
            Random random = new Random(seed);
            TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);

            Set<TMatchResult> originalResults = Sets.newHashSet();
            Set<TMatchResult> referenceResults = Sets.newHashSet();
            TNextMatchesResult originalNext = originalTournament.getMatchesToRun(initialSeeding, originalResults, actions);
            TNextMatchesResult referenceNext = referenceTournament.getMatchesToRun(initialSeeding, referenceResults, NO_ACTIONS);
            assertSameExceptIds(originalNext, referenceNext);
            assertStandingsIdentical(originalTournament, referenceTournament, initialSeeding, originalResults, referenceResults);
            while (!originalNext.getMatchesToRun().isEmpty()) {
                TMatchSetup originalMatchToResolve = FuzzTests.pickMatchAtRandom(random, originalNext.getMatchesToRun());
                TMatchSetup referenceMatchToResolve = findEquivalentMatch(originalMatchToResolve, referenceNext.getMatchesToRun());
                TMatchResult originalMatchResult = FuzzTests.getResult(random, originalMatchToResolve);
                TMatchResult referenceMatchResult = toEquivalentMatchResult(originalMatchResult, referenceMatchToResolve.getMatchId());
                originalResults.add(originalMatchResult);
                referenceResults.add(referenceMatchResult);

                originalNext = originalTournament.getMatchesToRun(initialSeeding, originalResults, actions);
                referenceNext = referenceTournament.getMatchesToRun(initialSeeding, referenceResults, NO_ACTIONS);
                assertSameExceptIds(originalNext, referenceNext);
                assertStandingsIdentical(originalTournament, referenceTournament, initialSeeding, originalResults, referenceResults);
            }
        }
    }

    private TMatchResult toEquivalentMatchResult(TMatchResult originalMatchResult,
            String newMatchId) {
        if (originalMatchResult.wasAborted()) {
            return TMatchResult.getAbortedMatchResult(newMatchId);
        } else {
            return TMatchResult.getSuccessfulMatchResult(newMatchId, originalMatchResult.getGoals());
        }
    }

    private void assertSameExceptIds(TNextMatchesResult originalNext, TNextMatchesResult referenceNext) {
        Assert.assertEquals(referenceNext.getSecondsToWaitUntilAllowedStartTime(), originalNext.getSecondsToWaitUntilAllowedStartTime());
        Assert.assertEquals(referenceNext.getEarliestAllowedStartTime(), originalNext.getEarliestAllowedStartTime());
        Assert.assertEquals(referenceNext.getMatchesToRun().size(), originalNext.getMatchesToRun().size());

        Map<PartialMatchId, TMatchSetup> originalSetups = putIntoMap(originalNext.getMatchesToRun());
        Map<PartialMatchId, TMatchSetup> referenceSetups = putIntoMap(referenceNext.getMatchesToRun());

        Assert.assertEquals(referenceSetups.keySet(), originalSetups.keySet());
        for (Entry<PartialMatchId, TMatchSetup> entry : referenceSetups.entrySet()) {
            PartialMatchId id = entry.getKey();
            TMatchSetup referenceSetup = entry.getValue();
            TMatchSetup originalSetup = originalSetups.get(id);

            Assert.assertEquals(referenceSetup.getGame(), originalSetup.getGame());
            Assert.assertEquals(referenceSetup.getPlayers(), originalSetup.getPlayers());
            Assert.assertEquals(referenceSetup.getPlayClock(), originalSetup.getPlayClock());
            Assert.assertEquals(referenceSetup.getStartClock(), originalSetup.getStartClock());
        }
    }

    private Map<PartialMatchId, TMatchSetup> putIntoMap(ImmutableSet<TMatchSetup> matchesToRun) {
        Map<PartialMatchId, TMatchSetup> map = Maps.newHashMap();
        for (TMatchSetup setup : matchesToRun) {
            map.put(new PartialMatchId(MatchId.create(setup.getMatchId())), setup);
        }
        return map;
    }

    private TMatchSetup findEquivalentMatch(TMatchSetup originalMatchToResolve,
            ImmutableSet<TMatchSetup> matchesToRun) {
        PartialMatchId originalId = PartialMatchId.create(originalMatchToResolve.getMatchId());
        for (TMatchSetup candidate : matchesToRun) {
            PartialMatchId candidateId = PartialMatchId.create(candidate.getMatchId());
            if (originalId.equals(candidateId)) {
                return candidate;
            }
        }
        throw new AssertionError("Equivalent match not found");
    }

    private void assertStandingsIdentical(TTournament originalTournament, TTournament referenceTournament,
            TSeeding initialSeeding, Set<TMatchResult> originalResults, Set<TMatchResult> referenceResults) {
        TRanking originalRanking = originalTournament.getCurrentStandings(initialSeeding, originalResults, actions);
        TRanking referenceRanking = referenceTournament.getCurrentStandings(initialSeeding, referenceResults, NO_ACTIONS);
        Assert.assertEquals(referenceRanking.getPlayersBestFirst(), originalRanking.getPlayersBestFirst());
        Assert.assertEquals(referenceRanking.getScores(), originalRanking.getScores());
    }

    private static class PartialMatchId {
        private final ImmutableList<Integer> idNumbers;

        public PartialMatchId(MatchId id) {
            this.idNumbers = ImmutableList.of(id.getStageNumber(), id.getRoundNumber(),
                    id.getPlayerMatchingNumber(), id.getMatchNumber(), id.getAttemptNumber());
        }

        public static PartialMatchId create(String matchIdString) {
            return new PartialMatchId(MatchId.create(matchIdString));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((idNumbers == null) ? 0 : idNumbers.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PartialMatchId other = (PartialMatchId) obj;
            if (idNumbers == null) {
                if (other.idNumbers != null)
                    return false;
            } else if (!idNumbers.equals(other.idNumbers))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return idNumbers.toString();
        }
    }
}
