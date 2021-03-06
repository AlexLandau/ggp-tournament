package net.alloyggp.tournament.internal.runner;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import net.alloyggp.escaperope.rope.ropify.ListWeaver;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.escaperope.rope.ropify.RopeList;
import net.alloyggp.escaperope.rope.ropify.Weaver;
import net.alloyggp.tournament.api.TGame;
import net.alloyggp.tournament.api.TMatchResult.Outcome;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TPlayerScore;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TScore;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.InternalMatchResult;
import net.alloyggp.tournament.internal.MatchId;
import net.alloyggp.tournament.internal.MatchIds;
import net.alloyggp.tournament.internal.MatchResults;
import net.alloyggp.tournament.internal.StandardNextMatchesResult;
import net.alloyggp.tournament.internal.StandardRanking;
import net.alloyggp.tournament.internal.admin.InternalAdminAction;
import net.alloyggp.tournament.internal.spec.MatchSpec;
import net.alloyggp.tournament.internal.spec.RoundSpec;
import net.alloyggp.tournament.internal.spec.StageFormat;

/*
 * Non-obvious aspects of this format implementation:
 * - In general, we use the system of creating a fixed bracket that pairs
 *   opponents such that the highest-ranked players would play the lowest-ranked
 *   (remaining) opponents in each round if the higher-seeded player always wins.
 * - If the number of players is not a power of 2, we cut to a power of two in a
 *   non-standard first round. Byes are awarded to the highest-ranked players first.
 * - This could be used in tournaments where the number of players is unknown when
 *   the tournament is planned, and so the number of rounds defined is unsuitable.
 *   Thus, we use the rounds in a special way: We always play such that the last
 *   round defined is the final round, the second-to-last is the semifinals, and
 *   so on. If the number of rounds needed exceeds the number of rounds defined,
 *   the first round defined is reused for the additional rounds.
 */
public class SingleEliminationFormat1Runner implements FormatRunner {

    private static final SingleEliminationFormat1Runner INSTANCE = new SingleEliminationFormat1Runner();

    private SingleEliminationFormat1Runner() {
    }

    public static SingleEliminationFormat1Runner create() {
        return INSTANCE;
    }

    //Mutable class; goes through the actual motions of the format and
    //records the relevant information as it goes
    @NotThreadSafe
    private static class SingleEliminationFormatSimulator {
        private final String tournamentInternalName;
        private final int stageNum;
        private final TSeeding initialSeeding;
        private final ImmutableList<InternalAdminAction> adminActions;
        private final ImmutableList<RoundSpec> rounds;
        private final ImmutableSet<InternalMatchResult> resultsFromEarlierStages;
        private final ImmutableSet<InternalMatchResult> resultsSoFarInStage;
        private final Set<TMatchSetup> matchesToReturn = Sets.newHashSet();
        private final Map<TPlayer, Integer> playerEliminationRounds = Maps.newHashMap();
        private @Nullable DateTime latestStartTimeSeen = null;

        private final List<TRanking> standingsHistory = Lists.newArrayList();

        //Use createAndRun instead
        private SingleEliminationFormatSimulator(String tournamentInternalName, int stageNum, TSeeding initialSeeding,
                ImmutableList<InternalAdminAction> adminActions,
                ImmutableList<RoundSpec> rounds, ImmutableSet<InternalMatchResult> resultsFromEarlierStages,
                ImmutableSet<InternalMatchResult> resultsSoFarInStage) {
            this.tournamentInternalName = tournamentInternalName;
            this.stageNum = stageNum;
            this.initialSeeding = initialSeeding;
            this.adminActions = adminActions;
            this.rounds = rounds;
            this.resultsFromEarlierStages = resultsFromEarlierStages;
            this.resultsSoFarInStage = resultsSoFarInStage;
        }

        public static SingleEliminationFormatSimulator createAndRun(String tournamentInternalName,
                int stageNum, TSeeding initialSeeding, ImmutableList<InternalAdminAction> adminActions,
                ImmutableList<RoundSpec> rounds, Set<InternalMatchResult> allResultsSoFar) {
            Set<InternalMatchResult> resultsFromEarlierStages = MatchResults.getResultsPriorToStage(allResultsSoFar, stageNum);
            Set<InternalMatchResult> resultsSoFarInStage = MatchResults.filterByStage(allResultsSoFar, stageNum);
            SingleEliminationFormatSimulator simulator = new SingleEliminationFormatSimulator(
                    tournamentInternalName, stageNum, initialSeeding, adminActions, rounds,
                    ImmutableSet.copyOf(resultsFromEarlierStages), ImmutableSet.copyOf(resultsSoFarInStage));
            simulator.run();
            return simulator;
        }

        private void run() {
            List<TPlayer> playersByPosition = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
            //If we're not a power of two, handle that
            int numPlayers = playersByPosition.size();
            int numRoundsLeft = getNumRounds(numPlayers);

            EndOfRoundState state = TournamentStateCache.getLatestCachedEndOfRoundState(tournamentInternalName, initialSeeding,
                    adminActions, resultsFromEarlierStages, stageNum, resultsSoFarInStage);
            if (state != null) {
                SingleEliminationRoundStatus status = (SingleEliminationRoundStatus) state;
                playersByPosition = Lists.newArrayList(status.playersByPosition);
                matchesToReturn.addAll(status.matchesToReturn);
                playerEliminationRounds.putAll(status.playerEliminationRounds);
                standingsHistory.addAll(status.standingsHistory);
                latestStartTimeSeen = status.latestStartTimeSeen;
                numRoundsLeft = status.numRoundsLeft - 1;
            }

            handleStartTimesForRoundsBefore(numRoundsLeft);
            if (numPlayers < getPlayersForNFullRounds(numRoundsLeft)) {
                runInitialPlayInRound(playersByPosition, numPlayers, numRoundsLeft);
                if (!matchesToReturn.isEmpty()) {
                    return; //still in this round
                }
                standingsHistory.add(getStandings());
                cacheEndOfRoundStatus(numRoundsLeft, playersByPosition);
                numRoundsLeft--;
            }
            //Now we handle the normal rounds
            while (numRoundsLeft > 0) {
                runNormalRound(playersByPosition, numRoundsLeft);
                if (!matchesToReturn.isEmpty()) {
                    return; //still in this round
                }
                standingsHistory.add(getStandings());
                cacheEndOfRoundStatus(numRoundsLeft, playersByPosition);
                numRoundsLeft--;
            }
            //We're at the end of the tournament
            TRanking finalStandings = standingsHistory.get(standingsHistory.size() - 1);
            finalStandings = labelWinner(finalStandings);
            standingsHistory.set(standingsHistory.size() - 1, finalStandings);
        }

        private TRanking labelWinner(TRanking finalStandings) {
            return StandardRanking.create(labelWinner(finalStandings.getScores()));
        }

        private Collection<TPlayerScore> labelWinner(SortedSet<TPlayerScore> originalScores) {
            SortedSet<TPlayerScore> scores = new TreeSet<>(originalScores);
            TPlayerScore firstPlace = scores.first();
            EliminationScore originalScore = (EliminationScore) firstPlace.getScore();
            EliminationScore replacementScore = new EliminationScore(-1, originalScore.totalNumRounds);
            TPlayerScore newFirstPlace = TPlayerScore.create(firstPlace.getPlayer(), replacementScore, firstPlace.getSeedFromRoundStart());
            scores.remove(firstPlace);
            scores.add(newFirstPlace);
            return scores;
        }

        public void runInitialPlayInRound(List<TPlayer> playersByPosition, int numPlayers, int numRoundsLeft) {
            RoundSpec round = getRoundForNumRoundsLeft(numRoundsLeft);
            handleStartTimeForRound(round);
            int numNormalRounds = numRoundsLeft - 1;
            int numPlayinMatches = numPlayers - getPlayersForNFullRounds(numNormalRounds);
            for (int i = 0; i < numPlayinMatches; i++) {
                int position1 = getPlayersForNFullRounds(numNormalRounds) - i - 1;
                int position2 = getPlayersForNFullRounds(numNormalRounds) + i;
                runMatchesForPositions(playersByPosition, numRoundsLeft, round, i, position1, position2);
            }
        }

        public void runNormalRound(List<TPlayer> playersByPosition, int numRoundsLeft) {
            RoundSpec round = getRoundForNumRoundsLeft(numRoundsLeft);
            handleStartTimeForRound(round);
            int numPlayersLeft = getPlayersForNFullRounds(numRoundsLeft);
            for (int i = 0; i < (numPlayersLeft / 2); i++) {
                int position1 = i;
                int position2 = numPlayersLeft - i - 1;
                runMatchesForPositions(playersByPosition, numRoundsLeft, round, i, position1, position2);
            }
        }

        public void runMatchesForPositions(List<TPlayer> playersByPosition, int numRoundsLeft,
                RoundSpec round, int pairingNum, int position1, int position2) {
            Preconditions.checkArgument(position1 < position2);
            TPlayer player1 = playersByPosition.get(position1);
            TPlayer player2 = playersByPosition.get(position2);

            List<TPlayer> playersBestFirst = Lists.newArrayList(player1, player2);
            Collections.sort(playersBestFirst, Ordering.explicit(initialSeeding.getPlayersBestFirst()));

            if (wonInRound(playersBestFirst.indexOf(player1), pairingNum, numRoundsLeft, round)) {
                playerEliminationRounds.put(player2, numRoundsLeft);
            } else if (wonInRound(playersBestFirst.indexOf(player2), pairingNum, numRoundsLeft, round)) {
                playersByPosition.set(position1, player2);
                playersByPosition.set(position2, player1);
                playerEliminationRounds.put(player1, numRoundsLeft);
            } else {
                matchesToReturn.add(getNextMatchForPairing(player1, player2, pairingNum, numRoundsLeft, round));
            }
        }

        private void handleStartTimeForRound(RoundSpec round) {
            if (round.getStartTime().isPresent()) {
                DateTime roundStartTime = round.getStartTime().get();
                if (latestStartTimeSeen == null
                        || latestStartTimeSeen.isBefore(roundStartTime)) {
                    latestStartTimeSeen = roundStartTime;
                }
            }
        }

        private void handleStartTimesForRoundsBefore(int numRoundsLeft) {
            int curRound = rounds.size();
            while (curRound > numRoundsLeft) {
                RoundSpec round = getRoundForNumRoundsLeft(curRound);
                handleStartTimeForRound(round);
                curRound--;
            }
        }

        private void cacheEndOfRoundStatus(int roundNum, List<TPlayer> playersByPosition) {
            TournamentStateCache.cacheEndOfRoundState(
                    tournamentInternalName,
                    initialSeeding,
                    adminActions,
                    resultsFromEarlierStages,
                    stageNum,
                    resultsSoFarInStage,
                    SingleEliminationRoundStatus.create(
                            playersByPosition,
                            matchesToReturn,
                            playerEliminationRounds,
                            standingsHistory,
                            latestStartTimeSeen,
                            roundNum));
        }

        //1 -> 2 for finals
        //2 -> 4 for semis
        public int getPlayersForNFullRounds(int numRoundsLeft) {
            return 1 << numRoundsLeft;
        }

        private RoundSpec getRoundForNumRoundsLeft(int numRoundsLeft) {
            //Count from the end, not the beginning (so the last round is always the finals)
            int index = rounds.size() - numRoundsLeft;
            if (index < 0) {
                //Repeat the first round until we hit the schedule
                index = 0;
            }
            return rounds.get(index);
        }

        private TMatchSetup getNextMatchForPairing(TPlayer player1, TPlayer player2,
                int pairingNum, int numRoundsLeft, RoundSpec round) {
            List<InternalMatchResult> completedSoFar = Lists.newArrayList();
            List<InternalMatchResult> abortedSoFar = Lists.newArrayList();
            //First, gather all the non-abandoned results so far
            for (InternalMatchResult result : resultsSoFarInStage) {
                MatchId matchId = result.getMatchId();
                if (numRoundsLeft != matchId.getRoundNumber()) {
                    continue;
                }
                if (pairingNum != matchId.getPlayerMatchingNumber()) {
                    continue;
                }
                if (result.getOutcome() == Outcome.ABORTED) {
                    abortedSoFar.add(result);
                } else {
                    completedSoFar.add(result);
                }
            }

            ImmutableList<MatchSpec> matches = round.getMatches();
            MatchSpec specToUse = null;
            for (int i = 0; i < matches.size(); i++) {
                specToUse = matches.get(i);
                if (haveCompleted(i, completedSoFar)) {
                    continue;
                } else {
                    break;
                }
            }
            int matchNum = 0;
            while (haveCompleted(matchNum, completedSoFar)) {
                matchNum++;
            }
            int priorMatchAttempts = 0;
            for (InternalMatchResult result : abortedSoFar) {
                if (result.getMatchId().getMatchNumber() == matchNum) {
                    priorMatchAttempts++;
                }
            }

            Preconditions.checkNotNull(specToUse);
            //If we make it here, repeat the last match type
            String matchId = MatchIds.create(adminActions, StageFormat.SINGLE_ELIMINATION1, stageNum, numRoundsLeft, pairingNum, matchNum, priorMatchAttempts).toString();
            //TODO: Alternate roles each time if we do have to repeat the last match type
            //(Also needs to be done in rolesSwapped)
            List<TPlayer> playersBestFirst = Lists.newArrayList(player1, player2);
            Collections.sort(playersBestFirst, Ordering.explicit(initialSeeding.getPlayersBestFirst()));
            return specToUse.createMatchSetup(matchId, playersBestFirst);
        }

        private boolean haveCompleted(int matchNumber, List<InternalMatchResult> completedSoFar) {
            for (InternalMatchResult result : completedSoFar) {
                Preconditions.checkArgument(result.getOutcome() == Outcome.COMPLETED);
                int resultMatchNumber = result.getMatchId().getMatchNumber();
                if (matchNumber == resultMatchNumber) {
                    return true;
                }
            }
            return false;
        }

        private boolean wonInRound(int pairingLevelPlayerIndex, int pairingNumber, int numRoundsLeft, RoundSpec round) {
            int gamesPlayed = 0;
            int pointsAboveOpponent = 0;
            for (InternalMatchResult result : resultsSoFarInStage) {
                MatchId matchId = result.getMatchId();
                if (numRoundsLeft != matchId.getRoundNumber()) {
                    continue;
                }
                if (pairingNumber != matchId.getPlayerMatchingNumber()) {
                    continue;
                }
                if (result.getOutcome() == Outcome.ABORTED) {
                    continue;
                }
                gamesPlayed++;
                //This is the match-level player index, which may not be the
                //pairing-level player index. i.e. if roles are swapped, these
                //are opposites.
                final int playerIndex;
                if (rolesSwapped(matchId, round)) {
                    playerIndex = 1 - pairingLevelPlayerIndex;
                } else {
                    playerIndex = pairingLevelPlayerIndex;
                }
                Preconditions.checkState(playerIndex == 0 || playerIndex == 1);
                int playerPoints = result.getGoals().get(playerIndex);
                int oppIndex = 1 - playerIndex;
                int oppPoints = result.getGoals().get(oppIndex);
                pointsAboveOpponent += (playerPoints - oppPoints);
            }
            return wonInRound(round, gamesPlayed, pointsAboveOpponent);
        }

        private boolean rolesSwapped(MatchId matchId, RoundSpec round) {
            ImmutableList<MatchSpec> matches = round.getMatches();

            int matchNumber = matchId.getMatchNumber();
            if (matchNumber >= matches.size()) {
                matchNumber = matches.size() - 1;
            }
            MatchSpec spec = matches.get(matchNumber);

            return spec.getPlayerSeedOrder().get(0) == 1;
        }

        private boolean wonInRound(RoundSpec round, int gamesPlayed, int pointsAboveOpponent) {
            //We assume that any game can return 100/0...
            //We end the round once one player has enough points to guarantee they're ahead at or after the
            //end of the last scheduled match
            //TODO: Maybe assign a threshold so TOs can schedule follow-up games with more flexibility?
            if (gamesPlayed >= round.getMatches().size()) {
                if (pointsAboveOpponent > 0) {
                    return true;
                }
            } else {
                //Can we end the round early?
                int gamesLeft = round.getMatches().size() - gamesPlayed;
                if (pointsAboveOpponent > (100 * gamesLeft)) {
                    return true;
                }
            }
            return false;
        }

        private int getNumRounds(int numPlayers) {
            int numRounds = 0;
            int num = numPlayers;
            while (num > 1) {
                num = num >> 1;
                numRounds++;
            }
            if (numPlayers > getPlayersForNFullRounds(numRounds)) {
                numRounds++;
            }
            return numRounds;
        }

        public TNextMatchesResult getMatchesToRun() {
            return StandardNextMatchesResult.create(ImmutableSet.copyOf(matchesToReturn),
                    latestStartTimeSeen);
        }

        private TRanking getStandings() {
            ImmutableSortedSet.Builder<TPlayerScore> playerScores = ImmutableSortedSet.naturalOrder();

            ImmutableList<TPlayer> playersBestFirst = initialSeeding.getPlayersBestFirst();
            for (int i = 0; i < playersBestFirst.size(); i++) {
                TPlayer player = playersBestFirst.get(i);
                Integer mapValue = playerEliminationRounds.get(player);
                TScore score = new EliminationScore(mapValue != null ? mapValue : 0, getNumRounds(playersBestFirst.size()));
                playerScores.add(TPlayerScore.create(player, score, i));
            }
            return StandardRanking.create(playerScores.build());
        }

        public List<TRanking> getStandingsHistory() {
            return ImmutableList.copyOf(standingsHistory);
        }
    }

    @Immutable
    private static class SingleEliminationRoundStatus implements EndOfRoundState {
        public final ImmutableList<TPlayer> playersByPosition;
        public final ImmutableSet<TMatchSetup> matchesToReturn;
        public final ImmutableMap<TPlayer, Integer> playerEliminationRounds;
        public final ImmutableList<TRanking> standingsHistory;
        public final @Nullable DateTime latestStartTimeSeen;
        public final int numRoundsLeft;

        private SingleEliminationRoundStatus(ImmutableList<TPlayer> playersByPosition,
                ImmutableSet<TMatchSetup> matchesToReturn, ImmutableMap<TPlayer, Integer> playerEliminationRounds,
                ImmutableList<TRanking> standingsHistory, DateTime latestStartTimeSeen, int numRoundsLeft) {
            this.playersByPosition = playersByPosition;
            this.matchesToReturn = matchesToReturn;
            this.playerEliminationRounds = playerEliminationRounds;
            this.standingsHistory = standingsHistory;
            this.latestStartTimeSeen = latestStartTimeSeen;
            this.numRoundsLeft = numRoundsLeft;
        }

        public static SingleEliminationRoundStatus create(List<TPlayer> playersByPosition,
                Set<TMatchSetup> matchesToReturn, Map<TPlayer, Integer> playerEliminationRounds,
                List<TRanking> standingsHistory, DateTime latestStartTimeSeen, int numRoundsLeft) {
            return new SingleEliminationRoundStatus(
                    ImmutableList.copyOf(playersByPosition),
                    ImmutableSet.copyOf(matchesToReturn),
                    ImmutableMap.copyOf(playerEliminationRounds),
                    ImmutableList.copyOf(standingsHistory),
                    latestStartTimeSeen,
                    numRoundsLeft);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((latestStartTimeSeen == null) ? 0 : latestStartTimeSeen.hashCode());
            result = prime * result + ((matchesToReturn == null) ? 0 : matchesToReturn.hashCode());
            result = prime * result + numRoundsLeft;
            result = prime * result + ((playerEliminationRounds == null) ? 0 : playerEliminationRounds.hashCode());
            result = prime * result + ((playersByPosition == null) ? 0 : playersByPosition.hashCode());
            result = prime * result + ((standingsHistory == null) ? 0 : standingsHistory.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SingleEliminationRoundStatus other = (SingleEliminationRoundStatus) obj;
            if (latestStartTimeSeen == null) {
                if (other.latestStartTimeSeen != null) {
                    return false;
                }
            } else if (!latestStartTimeSeen.equals(other.latestStartTimeSeen)) {
                return false;
            }
            if (matchesToReturn == null) {
                if (other.matchesToReturn != null) {
                    return false;
                }
            } else if (!matchesToReturn.equals(other.matchesToReturn)) {
                return false;
            }
            if (numRoundsLeft != other.numRoundsLeft) {
                return false;
            }
            if (playerEliminationRounds == null) {
                if (other.playerEliminationRounds != null) {
                    return false;
                }
            } else if (!playerEliminationRounds.equals(other.playerEliminationRounds)) {
                return false;
            }
            if (playersByPosition == null) {
                if (other.playersByPosition != null) {
                    return false;
                }
            } else if (!playersByPosition.equals(other.playersByPosition)) {
                return false;
            }
            if (standingsHistory == null) {
                if (other.standingsHistory != null) {
                    return false;
                }
            } else if (!standingsHistory.equals(other.standingsHistory)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "SingleEliminationRoundStatus [playersByPosition=" + playersByPosition + ", matchesToReturn="
                    + matchesToReturn + ", playerEliminationRounds=" + playerEliminationRounds + ", standingsHistory="
                    + standingsHistory + ", latestStartTimeSeen=" + latestStartTimeSeen + ", numRoundsLeft="
                    + numRoundsLeft + "]";
        }
    }

    @Override
    public TNextMatchesResult getMatchesToRun(String tournamentInternalName, TSeeding initialSeeding,
            List<InternalAdminAction> adminActions,
            int stageNum, List<RoundSpec> rounds, Set<InternalMatchResult> allResultsSoFar) {
        return SingleEliminationFormatSimulator.createAndRun(tournamentInternalName,
                stageNum, initialSeeding, ImmutableList.copyOf(adminActions), ImmutableList.copyOf(rounds), allResultsSoFar)
                .getMatchesToRun();
    }

    @Override
    public List<TRanking> getStandingsHistory(String tournamentInternalName,
            TSeeding initialSeeding, List<InternalAdminAction> adminActions, int stageNum, List<RoundSpec> rounds,
            Set<InternalMatchResult> allResultsSoFar) {
        return SingleEliminationFormatSimulator.createAndRun(tournamentInternalName,
                stageNum, initialSeeding, ImmutableList.copyOf(adminActions), ImmutableList.copyOf(rounds), allResultsSoFar)
                .getStandingsHistory();
    }

    private static class EliminationScore implements TScore {
        private final int roundEliminated; //0 if not yet eliminated, -1 if winner
        private final int totalNumRounds; //just for human-friendly output

        private EliminationScore(int roundEliminated, int totalNumRounds) {
            this.roundEliminated = roundEliminated;
            this.totalNumRounds = totalNumRounds;
        }

        @Override
        public int compareTo(TScore other) {
            if (!(other instanceof EliminationScore)) {
                throw new IllegalArgumentException("Expected an EliminationScore, but was a " + other.getClass());
            }
            //Higher scores should be better; but lower is better here, so flip
            return Integer.compare(((EliminationScore)other).roundEliminated, roundEliminated);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + roundEliminated;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            EliminationScore other = (EliminationScore) obj;
            if (roundEliminated != other.roundEliminated) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return getDescription();
        }

        @Override
        public String getDescription() {
            if (roundEliminated == -1) {
                return "winner";
            }
            if (roundEliminated == 0) {
                return "in contention";
            }
            // 2 rounds: 2 -> 1, 1 -> 2
            // 3 rounds: 3 -> 1, 2 -> 2, 1 -> 3
            // etc.
            return "eliminated in round " + (totalNumRounds - roundEliminated + 1);
        }
    }

    public static final Weaver<TScore> SCORE_WEAVER = new ListWeaver<TScore>() {
        @Override
        protected void addToList(TScore object, RopeBuilder list) {
            EliminationScore score = (EliminationScore) object;
            list.add(score.roundEliminated);
            list.add(score.totalNumRounds);
        }

        @Override
        protected TScore fromRope(RopeList list) {
            int roundEliminated = list.getInt(0);
            int totalNumRounds = list.getInt(1);
            return new EliminationScore(roundEliminated, totalNumRounds);
        }
    };

    @Override
    public void validateRounds(ImmutableList<RoundSpec> rounds) {
        // Require that all games used be two-player and zero-sum
        for (RoundSpec round : rounds) {
            if (round.getMatches().isEmpty()) {
                throw new IllegalArgumentException("Single-elimination rounds must have "
                        + "at least one match.");
            }
            for (MatchSpec match : round.getMatches()) {
                TGame game = match.getGame();
                if (game.getNumRoles() != 2) {
                    throw new IllegalArgumentException("Only two-player games should "
                            + "be used in a single-elimination format.");
                }
                if (!game.isFixedSum()) {
                    throw new IllegalArgumentException("Only fixed-sum games should "
                            + "be used in a single-elimination format.");
                }
                if (match.getWeight() != 1.0) {
                    throw new IllegalArgumentException("Custom match weights are not "
                            + "currently allowed in a single-elimination format.");
                }
            }
        }
    }
}
