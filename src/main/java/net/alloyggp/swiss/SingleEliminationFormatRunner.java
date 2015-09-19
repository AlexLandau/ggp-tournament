package net.alloyggp.swiss;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.alloyggp.swiss.api.Game;
import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchResult.Outcome;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.Player;
import net.alloyggp.swiss.api.PlayerScore;
import net.alloyggp.swiss.api.Score;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.TournamentStandings;
import net.alloyggp.swiss.spec.MatchSpec;
import net.alloyggp.swiss.spec.RoundSpec;

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
public class SingleEliminationFormatRunner implements FormatRunner {
    private static final SingleEliminationFormatRunner INSTANCE = new SingleEliminationFormatRunner();

    private SingleEliminationFormatRunner() {
    }

    public static SingleEliminationFormatRunner create() {
        return INSTANCE;
    }

    //Mutable class; goes through the actual motions of the format and
    //records the relevant information as it goes
    @NotThreadSafe
    private static class SingleEliminationFormatSimulator {
        private final String tournamentInternalName;
        private final int stageNum;
        private final Seeding initialSeeding;
        private final ImmutableList<RoundSpec> rounds;
        private final ImmutableList<MatchResult> resultsSoFar;
        private final Set<MatchSetup> matchesToReturn = Sets.newHashSet();
        private final Map<Player, Integer> playerEliminationRounds = Maps.newHashMap();

        //Use createAndRun instead
        private SingleEliminationFormatSimulator(String tournamentInternalName, int stageNum, Seeding initialSeeding,
                ImmutableList<RoundSpec> rounds, ImmutableList<MatchResult> resultsSoFar) {
            this.tournamentInternalName = tournamentInternalName;
            this.stageNum = stageNum;
            this.initialSeeding = initialSeeding;
            this.rounds = rounds;
            this.resultsSoFar = resultsSoFar;
        }

        public static SingleEliminationFormatSimulator createAndRun(String tournamentInternalName,
                int stageNum, Seeding initialSeeding,
                ImmutableList<RoundSpec> rounds, Set<MatchResult> resultsSoFar) {
            SingleEliminationFormatSimulator simulator = new SingleEliminationFormatSimulator(
                    tournamentInternalName, stageNum, initialSeeding, rounds,
                    ImmutableList.copyOf(resultsSoFar));
            simulator.run();
            return simulator;
        }

        private void run() {
            List<Player> playersByPosition = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
            //If we're not a power of two, handle that
            int numPlayers = playersByPosition.size();
            int numRoundsLeft = getNumRounds(numPlayers);
            if (numPlayers < getPlayersForNFullRounds(numRoundsLeft)) {
                RoundSpec round = getRoundForNumRoundsLeft(numRoundsLeft);
                int numNormalRounds = numRoundsLeft - 1;
                int numPlayinMatches = numPlayers - getPlayersForNFullRounds(numNormalRounds);
                for (int i = 0; i < numPlayinMatches; i++) {
                    int position1 = getPlayersForNFullRounds(numNormalRounds) - i - 1;
                    int position2 = getPlayersForNFullRounds(numNormalRounds) + i;
                    Player player1 = playersByPosition.get(position1);
                    Player player2 = playersByPosition.get(position2);
                    if (wonInRound(player1, numRoundsLeft, round)) {
                        playerEliminationRounds.put(player2, numRoundsLeft);
                    } else if (wonInRound(player2, numRoundsLeft, round)) {
                        playersByPosition.set(position1, player2);
                        playersByPosition.set(position2, player1);
                        playerEliminationRounds.put(player1, numRoundsLeft);
                    } else {
                        //TODO: Do we want to define roles according to seeding rather than bracket position?
                        //If so, pass in seeding here
                        matchesToReturn.add(getNextMatchForPairing(player1, player2, i, numRoundsLeft, round));
                    }
                }
                numRoundsLeft--;
                if (!matchesToReturn.isEmpty()) {
                    return; //still in this round
                }
            }
            //Now we handle the normal rounds
            while (numRoundsLeft > 0) {
                RoundSpec round = getRoundForNumRoundsLeft(numRoundsLeft);
                int numPlayersLeft = getPlayersForNFullRounds(numRoundsLeft);
                for (int i = 0; i < (numPlayersLeft / 2); i++) {
                    int position1 = i;
                    int position2 = numPlayersLeft - i - 1;
                    Player player1 = playersByPosition.get(position1);
                    Player player2 = playersByPosition.get(position2);
                    if (wonInRound(player1, numRoundsLeft, round)) {
                        playerEliminationRounds.put(player2, numRoundsLeft);
                    } else if (wonInRound(player2, numRoundsLeft, round)) {
                        playersByPosition.set(position1, player2);
                        playersByPosition.set(position2, player1);
                        playerEliminationRounds.put(player1, numRoundsLeft);
                    } else {
                        //TODO: Do we want to define roles according to seeding rather than bracket position?
                        //If so, pass in seeding here
                        matchesToReturn.add(getNextMatchForPairing(player1, player2, i, numRoundsLeft, round));
                    }
                }
                numRoundsLeft--;
                if (!matchesToReturn.isEmpty()) {
                    return; //still in this round
                }
            }
            //We're at the end of the tournament
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

        private MatchSetup getNextMatchForPairing(Player player1, Player player2,
                int pairingNum, int numRoundsLeft, RoundSpec round) {
            List<MatchResult> completedSoFar = Lists.newArrayList();
            List<MatchResult> abortedSoFar = Lists.newArrayList();
            //First, gather all the non-abandoned results so far
            for (MatchResult result : resultsSoFar) {
                //TODO: Replace with MatchIds logic?
                String matchId = result.getSetup().getMatchId();
                int roundNumber = MatchIds.parseRoundNumber(matchId);
                if (roundNumber != numRoundsLeft) {
                    continue;
                }
                if (!result.getSetup().getPlayers().contains(player1)) {
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
            for (MatchResult result : abortedSoFar) {
                if (MatchIds.parseMatchNumber(result.getSetup().getMatchId()) == matchNum) {
                    priorMatchAttempts++;
                }
            }

            Preconditions.checkNotNull(specToUse);
            //If we make it here, repeat the last match type
            String matchId = MatchIds.create(tournamentInternalName, stageNum, numRoundsLeft, pairingNum, matchNum, priorMatchAttempts);
            //TODO: Correctly define roles?
            //TODO: Accurate seeding
            //TODO: Alternate roles each time if we do have to repeat the last match type
            return specToUse.createMatchSetup(matchId, ImmutableList.of(player1, player2));
        }

        private boolean haveCompleted(int matchNumber, List<MatchResult> completedSoFar) {
            for (MatchResult result : completedSoFar) {
                Preconditions.checkArgument(result.getOutcome() == Outcome.COMPLETED);
                String matchId = result.getSetup().getMatchId();
                int resultMatchNumber = MatchIds.parseMatchNumber(matchId);
                if (matchNumber == resultMatchNumber) {
                    return true;
                }
            }
            return false;
        }

        private boolean wonInRound(Player player, int numRoundsLeft, RoundSpec round) {
            int gamesPlayed = 0;
            int pointsAboveOpponent = 0;
            for (MatchResult result : resultsSoFar) {
                //TODO: Replace with MatchIds logic?
                String matchId = result.getSetup().getMatchId();
                int roundNumber = MatchIds.parseRoundNumber(matchId);
                if (roundNumber != numRoundsLeft) {
                    continue;
                }
                if (!result.getSetup().getPlayers().contains(player)) {
                    continue;
                }
                if (result.getOutcome() == Outcome.ABORTED) {
                    continue;
                }
                gamesPlayed++;
                int playerIndex = result.getSetup().getPlayers().indexOf(player);
                int playerPoints = result.getGoals().get(playerIndex);
                Preconditions.checkState(playerIndex == 0 || playerIndex == 1);
                int oppIndex = 1 - playerIndex;
                int oppPoints = result.getGoals().get(oppIndex);
                pointsAboveOpponent += (playerPoints - oppPoints);
            }
            return wonInRound(round, gamesPlayed, pointsAboveOpponent);
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

        public Set<MatchSetup> getMatchesToRun() {
            return ImmutableSet.copyOf(matchesToReturn);
        }

        public Map<Player, Integer> getPlayerEliminationRounds() {
            return ImmutableMap.copyOf(playerEliminationRounds);
        }
    }

    @Override
    public Set<MatchSetup> getMatchesToRun(String tournamentInternalName, Seeding initialSeeding,
            int stageNum, List<RoundSpec> rounds, Set<MatchResult> resultsSoFar) {
        return SingleEliminationFormatSimulator.createAndRun(tournamentInternalName,
                stageNum, initialSeeding, ImmutableList.copyOf(rounds), resultsSoFar)
                .getMatchesToRun();
    }

    @Override
    public TournamentStandings getStandingsSoFar(String tournamentInternalName,
            Seeding initialSeeding, int stageNum, List<RoundSpec> rounds,
            Set<MatchResult> resultsSoFar) {
        ImmutableSortedSet.Builder<PlayerScore> playerScores = ImmutableSortedSet.naturalOrder();
        Map<Player, Integer> playerEliminationRounds = getPlayerEliminationRounds(
                tournamentInternalName, initialSeeding, stageNum, rounds, resultsSoFar);

        ImmutableList<Player> playersBestFirst = initialSeeding.getPlayersBestFirst();
        for (int i = 0; i < playersBestFirst.size(); i++) {
            Player player = playersBestFirst.get(i);
            Score score = new EliminationScore(playerEliminationRounds.getOrDefault(player, 0));
            playerScores.add(PlayerScore.create(player, score, i));
        }
        return TournamentStandings.create(playerScores.build());
    }

    private Map<Player, Integer> getPlayerEliminationRounds(String tournamentInternalName,
            Seeding initialSeeding, int stageNum, List<RoundSpec> rounds,
            Set<MatchResult> resultsSoFar) {
        return SingleEliminationFormatSimulator.createAndRun(tournamentInternalName,
                stageNum, initialSeeding, ImmutableList.copyOf(rounds), resultsSoFar)
                .getPlayerEliminationRounds();
    }

    private static class EliminationScore implements Score {
        private final int roundEliminated; //0 if not yet eliminated

        private EliminationScore(int roundEliminated) {
            this.roundEliminated = roundEliminated;
        }

        @Override
        public int compareTo(Score other) {
            if (!(other instanceof EliminationScore)) {
                throw new RuntimeException("Incomparable scores being compared");
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
            if (roundEliminated == 0) {
                return "in contention";
            }
            //TODO: This is confusing, change this (play-in round is round 1, etc.)
            return "eliminated in round " + roundEliminated;
        }
    }

    @Override
    public void validateRounds(ImmutableList<RoundSpec> rounds) {
        // Require that all games used be two-player and zero-sum
        for (RoundSpec round : rounds) {
            if (round.getMatches().isEmpty()) {
                throw new IllegalArgumentException("Single-elimination rounds must have "
                        + "at least one match.");
            }
            for (MatchSpec match : round.getMatches()) {
                Game game = match.getGame();
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
