package net.alloyggp.swiss;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
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

public class SwissFormat1Runner implements FormatRunner {
    private static final SwissFormat1Runner INSTANCE = new SwissFormat1Runner();

    private SwissFormat1Runner() {
        //Not instantiable
    }

    public static SwissFormat1Runner create() {
        return INSTANCE;
    }

    @NotThreadSafe
    private static class SwissFormatSimulator {
        private final String tournamentInternalName;
        private final int stageNum;
        private final Seeding initialSeeding;
        private final ImmutableList<RoundSpec> rounds;
        private final ImmutableList<MatchResult> resultsSoFar;
        private final Set<MatchSetup> matchesToRun = Sets.newHashSet();
        private final Map<Player, Integer> totalPointsScored = Maps.newHashMap();
        private final Map<Game, Map<Player, Integer>> pointsScoredByGame = Maps.newHashMap();
        private final Multiset<Set<Player>> totalMatchupsSoFar = HashMultiset.create();
        private final Map<Game, Multiset<Set<Player>>> matchupsSoFarByGame = Maps.newHashMap();
        private final Map<Integer, Multiset<Set<Player>>> nonFixedSumMatchupsSoFarByNumPlayers = Maps.newHashMap();
        private final Map<Integer, Map<Player, Multiset<Integer>>> nonFixedSumRoleAssignmentsSoFarByNumPlayers = Maps.newHashMap();

        private SwissFormatSimulator(String tournamentInternalName, int stageNum, Seeding initialSeeding,
                ImmutableList<RoundSpec> rounds, ImmutableList<MatchResult> resultsSoFar) {
            this.tournamentInternalName = tournamentInternalName;
            this.stageNum = stageNum;
            this.initialSeeding = initialSeeding;
            this.rounds = rounds;
            this.resultsSoFar = resultsSoFar;
        }

        public static SwissFormatSimulator createAndRun(String tournamentInternalName, int stageNum, Seeding initialSeeding,
                ImmutableList<RoundSpec> rounds, Set<MatchResult> resultsSoFar) {
            SwissFormatSimulator simulator = new SwissFormatSimulator(tournamentInternalName, stageNum, initialSeeding,
                    rounds, ImmutableList.copyOf(resultsSoFar));
            simulator.run();
            return simulator;
        }

        private void run() {
            setInitialTotalsToZero();

            SetMultimap<Integer, MatchResult> matchesByRound = MatchResults.mapByRound(resultsSoFar, stageNum);
            for (int roundNum = 0; roundNum < rounds.size(); roundNum++) {
                RoundSpec round = rounds.get(roundNum);
                Set<MatchResult> roundResults = matchesByRound.get(roundNum);
                runRound(round, roundNum, roundResults);
                if (!matchesToRun.isEmpty()) {
                    //We're still finishing up this round, not ready to assign matches in the next one
                    return;
                }
            }
        }

        private void runRound(RoundSpec round, int roundNum, Set<MatchResult> roundResults) {
            //...there should be only one match per round, I think?
            //Or at least they must involve the same game?
            Game game = getOnlyGame(round);
            //Figure out how to assign players
            List<List<Player>> playerGroups = getPlayerGroups(game);
            int maxScoreAchieved = 0;
            int scoreSum = 0;
            int scoreCount = 0;
            for (int groupNum = 0; groupNum < playerGroups.size(); groupNum++) {
                List<Player> players = playerGroups.get(groupNum);
                for (int matchNum = 0; matchNum < round.getMatches().size(); matchNum++) {
                    MatchSpec match = round.getMatches().get(matchNum);
                    Optional<Integer> attemptNum = getAttemptNumberIfUnfinished(groupNum, matchNum, roundResults);
                    if (attemptNum.isPresent()) {
                        String matchId = MatchIds.create(tournamentInternalName, stageNum,
                                roundNum, groupNum, matchNum, attemptNum.get());

                        matchesToRun.add(match.createMatchSetup(matchId, players));
                        break;
                    } else {
                        MatchResult result = getSuccessfulAttempt(groupNum, matchNum, roundResults);
                        //Add the results of the match to our point totals
                        List<Player> playersInRoleOrder = match.putInOrder(players);
                        for (int role = 0; role < players.size(); role++) {
                            Player player = playersInRoleOrder.get(role);
                            int goalValue = result.getGoals().get(role);

                            //TODO: Add to stats here, including stats yet to be introduced
                            //such as player-player meetings
                            addToSumWithKey(player, goalValue, totalPointsScored);
                            addToSumWithKey(player, goalValue, pointsScoredByGame.get(game));

                            maxScoreAchieved = Integer.max(maxScoreAchieved, goalValue);
                            scoreSum += goalValue;
                            scoreCount++;
                        }
                    }
                }
            }
            if (matchesToRun.isEmpty()) {
                //If we're at the end of a round and all the groups have gone and
                //we have a player left, manage the byes
                Set<Player> unassignedPlayers = getUnassignedPlayers(initialSeeding.getPlayersBestFirst(), playerGroups);
                if (!unassignedPlayers.isEmpty()) {
                    //Calculate bye score for the game
                    int byeScore = getByeScoreForRound(game, maxScoreAchieved, scoreSum, scoreCount);
                    Preconditions.checkState(byeScore >= 0 && byeScore <= 100);
                    for (Player player : unassignedPlayers) {
                        addToSumWithKey(player, byeScore, totalPointsScored);
                        addToSumWithKey(player, byeScore, pointsScoredByGame.get(game));
                    }
                }
            }
        }

        private static int getByeScoreForRound(Game game, int maxScoreAchieved, int scoreSum, int scoreCount) {
            if (game.isFixedSum()) {
                if (game.getNumRoles() == 2) {
                    return maxScoreAchieved;
                } else {
                    throw new UnsupportedOperationException();
                }
            } else {
                if (scoreCount > 0) {
                    return (int) Math.round(scoreSum / (double) scoreCount);
                } else {
                    //Not enough players for the round
                    return 0;
                }
            }
        }

        private List<List<Player>> getPlayerGroups(Game game) {
            int numRoles = game.getNumRoles();

            if (numRoles == 1) {
                return initialSeeding.getPlayersBestFirst().stream()
                            .map(ImmutableList::of)
                            .collect(Collectors.toList());
            }
            if (game.isFixedSum()) {
                if (numRoles == 2) {
                    return getTwoPlayerFixedSumPlayerGroups(game);
                } else {
                    return getManyPlayerFixedSumPlayerGroups();
                }
            } else {
                //Use quasi-random pairings
                return getNonFixedSumPlayerGroups(game.getNumRoles());
            }
        }

        private List<List<Player>> getNonFixedSumPlayerGroups(int numRoles) {

            //Two-player: there's an elegant round-robin algorithm we
            //could use here, "rotate" all but one player in two rows

            //Do something naive for now
            Multiset<Set<Player>> matchupsSoFar = nonFixedSumMatchupsSoFarByNumPlayers.get(numRoles);
            Preconditions.checkNotNull(matchupsSoFar);
            Map<Player, Multiset<Integer>> roleAssignmentsSoFar = nonFixedSumRoleAssignmentsSoFarByNumPlayers.get(numRoles);
            List<Player> playersToAssign = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
            List<List<Player>> results = Lists.newArrayList();
            while (playersToAssign.size() >= numRoles) {
                Player player = playersToAssign.get(0);

                //Grab the first available players with the fewest previous matchups against us and each other
                List<Player> playersInGroup = Lists.newArrayList(player);
                playersToAssign.remove(player);
                while (playersInGroup.size() < numRoles) {
                    Ordering<Player> playerOrder = Ordering.from(
                            Comparator.<Player, Integer>comparing(p -> {
                                //Sum of matchups against players already in group
                                return playersInGroup.stream()
                                    .map(playerInGroup -> ImmutableSet.of(p, playerInGroup))
                                    .mapToInt(matchupsSoFar::count)
                                    .sum();
                            }).thenComparing(Comparator.comparing(initialSeeding.getPlayersBestFirst()::indexOf)));
                    Player playerToAdd = playerOrder.min(playersToAssign);
                    playersInGroup.add(playerToAdd);
                    playersToAssign.remove(playerToAdd);
                }
                //TODO: Shuffle the roles intelligently, somehow
                //Should role shuffling be per-game? Across the tournament?

                results.add(playersInGroup);
            }
            return results;
        }

        private List<List<Player>> getManyPlayerFixedSumPlayerGroups() {
            // TODO Implement
            throw new UnsupportedOperationException();
        }

        private List<List<Player>> getTwoPlayerFixedSumPlayerGroups(Game game) {
            List<List<Player>> groups = Lists.newArrayList();

            Set<Player> assignedSoFar = Sets.newHashSet();
            List<Player> overallPlayerRankings = getPlayerRankingsForGame(game);
            int numPlayers = initialSeeding.getPlayersBestFirst().size();
            while (numPlayers - assignedSoFar.size() >= game.getNumRoles()) {
                //First, get the best player left according to the rankings so far
                Player firstPlayer = getFirstUnassignedPlayer(overallPlayerRankings, assignedSoFar);
                assignedSoFar.add(firstPlayer);
                //Now we look for the best opponent for that player
                List<Player> opponentRankings = getOpponentRankingsForPlayer(firstPlayer, game);
                Player opponent = getFirstUnassignedPlayer(opponentRankings, assignedSoFar);
                assignedSoFar.add(opponent);
                //Best seed goes first
                groups.add(ImmutableList.of(firstPlayer, opponent));
            }

            return groups;
        }

        private List<Player> getOpponentRankingsForPlayer(Player firstPlayer, Game game) {
            List<Player> allOpponents = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
            allOpponents.remove(firstPlayer);

            allOpponents.sort(Comparator.comparing(opponent -> {
                    //Higher points scored by game better
                    //but discount 100 points per matchup already played in this game
                    return pointsScoredByGame.get(game).get(opponent)
                        - 100 * matchupsSoFarByGame.get(game).count(ImmutableSet.of(firstPlayer, opponent));
                }
            ).thenComparing(Comparator.comparing(opponent -> {
                    //Higher total points scored better
                    //but discount 100 points per matchup already played in any Swiss rounds
                    return totalPointsScored.get(opponent)
                        - 100 * totalMatchupsSoFar.count(ImmutableSet.of(firstPlayer, opponent));
                }
            )).reversed()
                    .thenComparing(initialSeeding.getPlayersBestFirst()::indexOf));
            return allOpponents;
        }

        private List<Player> getPlayerRankingsForGame(Game game) {
            List<Player> players = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
            //Sort according to an appropriate comparator
            //We do want the best players at the beginning of the list...
            //We may accomplish that through an appropriate reversal
            //First
            players.sort(Comparator.comparing(pointsScoredByGame.get(game)::get)
                    .thenComparing(totalPointsScored::get)
                    .reversed()
                    .thenComparing(initialSeeding.getPlayersBestFirst()::indexOf));
            return players;
        }

        private Player getFirstUnassignedPlayer(List<Player> players, Set<Player> assignedSoFar) {
            for (Player player : players) {
                if (!assignedSoFar.contains(player)) {
                    return player;
                }
            }
            throw new IllegalArgumentException("No unassigned players left");
        }

        private <K> void addToSumWithKey(K key, int addend, Map<K, Integer> map) {
            map.put(key, addend + map.get(key));
        }

        private Optional<Integer> getAttemptNumberIfUnfinished(int groupNum, int matchNum,
                Set<MatchResult> roundResults) {
            int attemptsSoFar = 0;
            for (MatchResult result : roundResults) {
                String matchId = result.getSetup().getMatchId();
                if (groupNum == MatchIds.parsePlayerMatchingNumber(matchId)
                        && matchNum == MatchIds.parseMatchNumber(matchId)) {
                    if (result.getOutcome() == Outcome.ABORTED) {
                        attemptsSoFar++;
                    } else {
                        return Optional.absent();
                    }
                }
            }
            return Optional.of(attemptsSoFar);
        }

        private MatchResult getSuccessfulAttempt(int groupNum, int matchNum, Set<MatchResult> roundResults) {
            for (MatchResult result : roundResults) {
                if (result.getOutcome() == Outcome.COMPLETED) {
                    String matchId = result.getSetup().getMatchId();
                    if (groupNum == MatchIds.parsePlayerMatchingNumber(matchId)
                            && matchNum == MatchIds.parseMatchNumber(matchId)) {
                        return result;
                    }
                }
            }
            throw new IllegalArgumentException("No successful attempts found");
        }

        private void setInitialTotalsToZero() {
            for (Player player : initialSeeding.getPlayersBestFirst()) {
                totalPointsScored.put(player, 0);
            }
            Set<Integer> possiblePlayerCounts = Sets.newHashSet();
            for (Game game : RoundSpec.getAllGames(rounds)) {
                HashMap<Player, Integer> pointsScoredForGame = Maps.newHashMap();
                pointsScoredByGame.put(game, pointsScoredForGame);
                for (Player player : initialSeeding.getPlayersBestFirst()) {
                    pointsScoredForGame.put(player, 0);
                }
                matchupsSoFarByGame.put(game, HashMultiset.create());
                possiblePlayerCounts.add(game.getNumRoles());
            }
            for (int playerCount : possiblePlayerCounts) {
                nonFixedSumMatchupsSoFarByNumPlayers.put(playerCount, HashMultiset.create());
                Map<Player, Multiset<Integer>> nonFixedSumRoleAssignmentsSoFar = Maps.newHashMap();
                for (Player player : initialSeeding.getPlayersBestFirst()) {
                    nonFixedSumRoleAssignmentsSoFar.put(player, HashMultiset.create());
                }
                nonFixedSumRoleAssignmentsSoFarByNumPlayers.put(playerCount, nonFixedSumRoleAssignmentsSoFar);
            }

        }

        public Set<MatchSetup> getMatchesToRun() {
            return ImmutableSet.copyOf(matchesToRun);
        }

        public TournamentStandings getStandings() {
            Set<PlayerScore> scores = Sets.newHashSet();
            ImmutableList<Player> playersBestFirst = initialSeeding.getPlayersBestFirst();
            for (int i = 0; i < playersBestFirst.size(); i++) {
                Player player = playersBestFirst.get(i);
                Score score = new SwissScore(totalPointsScored.get(player));
                scores.add(PlayerScore.create(player, score, i));
            }
            return TournamentStandings.create(scores);
        }

    }

    private static class SwissScore implements Score {
        private final int pointsSoFar;

        public SwissScore(int pointsSoFar) {
            this.pointsSoFar = pointsSoFar;
        }

        @Override
        public int compareTo(Score other) {
            if (!(other instanceof SwissScore)) {
                throw new IllegalArgumentException();
            }
            return Integer.compare(pointsSoFar, ((SwissScore)other).pointsSoFar);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + pointsSoFar;
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
            SwissScore other = (SwissScore) obj;
            if (pointsSoFar != other.pointsSoFar) {
                return false;
            }
            return true;
        }
    }

    @Override
    public Set<MatchSetup> getMatchesToRun(String tournamentInternalName, Seeding initialSeeding, int stageNum,
            List<RoundSpec> rounds, Set<MatchResult> resultsSoFar) {
        return SwissFormatSimulator.createAndRun(tournamentInternalName, stageNum, initialSeeding,
                ImmutableList.copyOf(rounds), resultsSoFar).getMatchesToRun();
    }

    public static Set<Player> getUnassignedPlayers(Collection<Player> allPlayers, List<List<Player>> playerGroups) {
        Set<Player> results = Sets.newHashSet(allPlayers);
        for (List<Player> group : playerGroups) {
            for (Player player : group) {
                results.remove(player);
            }
        }
        return results;
    }

    @Override
    public TournamentStandings getStandingsSoFar(String tournamentInternalName, Seeding initialSeeding, int stageNum,
            List<RoundSpec> rounds, Set<MatchResult> resultsSoFar) {
        return SwissFormatSimulator.createAndRun(tournamentInternalName, stageNum, initialSeeding,
                ImmutableList.copyOf(rounds), resultsSoFar).getStandings();
    }

    private static Game getOnlyGame(RoundSpec round) {
        if (round.getMatches().isEmpty()) {
            throw new IllegalArgumentException("Swiss rounds must have at least one match");
        }
        Game game = round.getMatches().get(0).getGame();
        for (MatchSpec match : round.getMatches()) {
            if (!game.equals(match.getGame())) {
                throw new IllegalArgumentException("Swiss rounds in Swiss variant 1 must use "
                        + "the same game in each match, and frequently have one match per round");
            }
        }
        return game;
    }

    @Override
    public void validateRounds(ImmutableList<RoundSpec> rounds) {
        //TODO: Implement
        for (RoundSpec round : rounds) {
            //Validates all matches are the same game
            getOnlyGame(round);
        }
    }
}
