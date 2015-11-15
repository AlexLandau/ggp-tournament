package net.alloyggp.tournament.impl;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.Game;
import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchResult.Outcome;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.NextMatchesResult;
import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.PlayerScore;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Score;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.spec.MatchSpec;
import net.alloyggp.tournament.spec.RoundSpec;

public class SwissFormat1Runner implements FormatRunner {
    private static final SwissFormat1Runner INSTANCE = new SwissFormat1Runner();

    private SwissFormat1Runner() {
        //Not instantiable
    }

    public static SwissFormat1Runner create() {
        return INSTANCE;
    }

    //TODO: Factor out the common elements between this and SingleEliminationFormatSimulator
    @NotThreadSafe
    private static class SwissFormatSimulator {
        private final String tournamentInternalName;
        private final int stageNum;
        private final Seeding initialSeeding;
        private final ImmutableList<RoundSpec> rounds;
        private final ImmutableSet<MatchResult> resultsFromEarlierStages;
        private final ImmutableSet<MatchResult> resultsInStage;
        private final Set<MatchSetup> matchesToRun = Sets.newHashSet();
        //TODO: Double-check that all these stats are updated appropriately
        private Game mostRecentGame = null; //of a fully completed round
        private final Map<Player, Double> totalPointsScored = Maps.newHashMap();
        private final Map<Game, Map<Player, Double>> pointsScoredByGame = Maps.newHashMap();
        private final Map<Player, Double> pointsFromByes = Maps.newHashMap();
        private final Multiset<Set<Player>> totalMatchupsSoFar = HashMultiset.create();
        private final Map<Game, Multiset<Set<Player>>> matchupsSoFarByGame = Maps.newHashMap();
        private final Map<Integer, Multiset<Set<Player>>> nonFixedSumMatchupsSoFarByNumPlayers = Maps.newHashMap();
//        private final Map<Integer, Map<Player, Multiset<Integer>>> nonFixedSumRoleAssignmentsSoFarByNumPlayers = Maps.newHashMap();
        private final List<Ranking> standingsHistory = Lists.newArrayList();
        private @Nullable ZonedDateTime latestStartTimeSeen = null;

        private SwissFormatSimulator(String tournamentInternalName, int stageNum, Seeding initialSeeding,
                ImmutableList<RoundSpec> rounds, ImmutableSet<MatchResult> resultsFromEarlierStages,
                ImmutableSet<MatchResult> resultsInStage) {
            this.tournamentInternalName = tournamentInternalName;
            this.stageNum = stageNum;
            this.initialSeeding = initialSeeding;
            this.rounds = rounds;
            this.resultsFromEarlierStages = resultsFromEarlierStages;
            this.resultsInStage = resultsInStage;
        }

        public static SwissFormatSimulator createAndRun(String tournamentInternalName, int stageNum, Seeding initialSeeding,
                ImmutableList<RoundSpec> rounds, Set<MatchResult> allResultsSoFar) {
            Set<MatchResult> resultsFromEarlierStages = MatchResults.getResultsPriorToStage(allResultsSoFar, stageNum);
            Set<MatchResult> resultsInStage = MatchResults.filterByStage(allResultsSoFar, stageNum);
            SwissFormatSimulator simulator = new SwissFormatSimulator(tournamentInternalName, stageNum, initialSeeding,
                    rounds, ImmutableSet.copyOf(resultsFromEarlierStages), ImmutableSet.copyOf(resultsInStage));
            simulator.run();
            return simulator;
        }

        private void run() {
            setInitialTotalsToZero();
            int roundNum = 0;
            SetMultimap<Integer, MatchResult> matchesByRound = MatchResults.mapByRound(resultsInStage, stageNum);

            @Nullable EndOfRoundState endOfRoundState = TournamentStateCache.getLatestCachedEndOfRoundState(tournamentInternalName, initialSeeding, resultsFromEarlierStages, stageNum, resultsInStage);
            if (endOfRoundState != null) {
                Swiss1EndOfRoundState state = (Swiss1EndOfRoundState) endOfRoundState;
                roundNum = state.roundNum + 1;
                loadCachedState(state);
            }

            for (/* roundNum already set */; roundNum < rounds.size(); roundNum++) {
                RoundSpec round = rounds.get(roundNum);
                Set<MatchResult> roundResults = matchesByRound.get(roundNum);
                runRound(round, roundNum, roundResults);
                if (!matchesToRun.isEmpty()) {
                    //We're still finishing up this round, not ready to assign matches in the next one
                    return;
                }
                //If we didn't run the round due to the number of players being too low,
                //skip the standings and caching
                if (!roundResults.isEmpty()) {
                    standingsHistory.add(getStandings());
                    Swiss1EndOfRoundState state = Swiss1EndOfRoundState.create(roundNum,
                            mostRecentGame, totalPointsScored, pointsScoredByGame,
                            pointsFromByes, totalMatchupsSoFar, matchupsSoFarByGame,
                            nonFixedSumMatchupsSoFarByNumPlayers, standingsHistory,
                            latestStartTimeSeen);

                    TournamentStateCache.cacheEndOfRoundState(tournamentInternalName, initialSeeding, resultsFromEarlierStages, stageNum, resultsInStage, state);
                }
            }
        }

        private void loadCachedState(Swiss1EndOfRoundState state) {
            totalPointsScored.putAll(state.totalPointsScored);
            pointsFromByes.putAll(state.pointsFromByes);

            Set<Integer> possiblePlayerCounts = Sets.newHashSet();
            for (Game game : RoundSpec.getAllGames(rounds)) {
                Map<Player, Double> pointsScoredForGame = pointsScoredByGame.get(game);
                for (Player player : initialSeeding.getPlayersBestFirst()) {
                    pointsScoredForGame.put(player, state.pointsScoredByGame.get(game).get(player));
                }
                for (Entry<ImmutableSet<Player>> entry : state.matchupsSoFarByGame.get(game).entrySet()) {
                    matchupsSoFarByGame.get(game).add(Sets.newHashSet(entry.getElement()), entry.getCount());
                }
                possiblePlayerCounts.add(game.getNumRoles());
            }
            for (int playerCount : possiblePlayerCounts) {
                for (Entry<ImmutableSet<Player>> entry : state.nonFixedSumMatchupsSoFarByNumPlayers.get(playerCount).entrySet()) {
                    nonFixedSumMatchupsSoFarByNumPlayers.get(playerCount).add(Sets.newHashSet(entry.getElement()), entry.getCount());
                }
            }

            mostRecentGame = state.mostRecentGame;
            standingsHistory.addAll(state.standingsHistory);

            for (Entry<ImmutableSet<Player>> entry : state.totalMatchupsSoFar.entrySet()) {
                totalMatchupsSoFar.add(Sets.newHashSet(entry.getElement()), entry.getCount());
            }
            latestStartTimeSeen = state.latestStartTimeSeen;
        }

        private void runRound(RoundSpec round, int roundNum, Set<MatchResult> roundResults) {
            handleStartTimeForRound(round);
            //...there should be only one match per round, I think?
            //Or at least they must involve the same game?
            Game game = getOnlyGame(round);
            //Figure out how to assign players
            List<List<Player>> playerGroups = getPlayerGroups(game);
            double maxScoreAchieved = 0;
            double scoreSum = 0;
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
                            double goalValue = result.getGoals().get(role) * match.getWeight();

                            //TODO: Add to stats here, including stats yet to be introduced
                            //such as player-player meetings
                            addToSumWithKey(player, goalValue, totalPointsScored);
                            addToSumWithKey(player, goalValue, pointsScoredByGame.get(game));

                            maxScoreAchieved = Double.max(maxScoreAchieved, goalValue);
                            scoreSum += goalValue;
                            scoreCount++;
                            this.mostRecentGame = game;
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
                    double byeScore = getByeScoreForRound(game, maxScoreAchieved, scoreSum, scoreCount);
                    Preconditions.checkState(byeScore >= 0 && byeScore <= 100);
                    for (Player player : unassignedPlayers) {
                        addToSumWithKey(player, byeScore, totalPointsScored);
                        addToSumWithKey(player, byeScore, pointsScoredByGame.get(game));
                        addToSumWithKey(player, byeScore, pointsFromByes);
                    }
                }
                //Also...
                updateMatchupStats(game, playerGroups);
            }
        }

        private void handleStartTimeForRound(RoundSpec round) {
            if (round.getStartTime().isPresent()) {
                ZonedDateTime roundStartTime = round.getStartTime().get();
                if (latestStartTimeSeen == null
                        || latestStartTimeSeen.isBefore(roundStartTime)) {
                    latestStartTimeSeen = roundStartTime;
                }
            }
        }

        private void updateMatchupStats(Game game, List<List<Player>> playerGroups) {
            for (List<Player> players : playerGroups) {
                if (game.isFixedSum()) {
                    if (game.getNumRoles() == 2) {
                        matchupsSoFarByGame.get(game).add(ImmutableSet.of(players.get(0), players.get(1)));
                        totalMatchupsSoFar.add(ImmutableSet.of(players.get(0), players.get(1)));
                    } else {
                        for (int p1 = 0; p1 < players.size(); p1++) {
                            for (int p2 = p1 + 1; p2 < players.size(); p2++) {
                                matchupsSoFarByGame.get(game)
                                    .add(ImmutableSet.of(players.get(p1), players.get(p2)));
                                totalMatchupsSoFar.add(ImmutableSet.of(players.get(p1), players.get(p2)));
                            }
                        }
                    }
                } else {
                    //TODO: Fix this
                    for (int p1 = 0; p1 < players.size(); p1++) {
                        for (int p2 = p1 + 1; p2 < players.size(); p2++) {
                            nonFixedSumMatchupsSoFarByNumPlayers.get(game.getNumRoles())
                                .add(ImmutableSet.of(players.get(p1), players.get(p2)));
                        }
                    }
//                    nonFixedSumRoleAssignmentsSoFarByNumPlayers
                }
            }
        }

        private static double getByeScoreForRound(Game game, double maxScoreAchieved, double scoreSum, int scoreCount) {
            if (game.isFixedSum()) {
                if (game.getNumRoles() == 2) {
                    return maxScoreAchieved;
                } else {
                    return maxScoreAchieved;
                }
            } else {
                if (scoreCount > 0) {
                    return scoreSum / scoreCount;
                } else {
                    //Not enough players for the round
                    return 0.0;
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
                    return getManyPlayerFixedSumPlayerGroups(game);
                }
            } else {
                //Use quasi-random pairings
                return getNonFixedSumPlayerGroups(game.getNumRoles());
            }
        }

        //TODO: Consider prioritizing certain players for byes? e.g. players
        //doing worse in the tournament, those with fewer byes already?
        private List<List<Player>> getNonFixedSumPlayerGroups(int numRoles) {

            //Two-player: there's an elegant round-robin algorithm we
            //could use here, "rotate" all but one player in two rows

            //Do something naive for now
            Multiset<Set<Player>> matchupsSoFar = nonFixedSumMatchupsSoFarByNumPlayers.get(numRoles);
            Preconditions.checkNotNull(matchupsSoFar);
//            Map<Player, Multiset<Integer>> roleAssignmentsSoFar = nonFixedSumRoleAssignmentsSoFarByNumPlayers.get(numRoles);
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
                                })
                            .thenComparing(Comparator.comparing(
                                    initialSeeding.getPlayersBestFirst()::indexOf)));
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

        private List<List<Player>> getManyPlayerFixedSumPlayerGroups(Game game) {
            List<List<Player>> groups = Lists.newArrayList();

            Set<Player> assignedSoFar = Sets.newHashSet();
            List<Player> overallPlayerRankings = getPlayerRankingsForGame(game);
            int numPlayers = initialSeeding.getPlayersBestFirst().size();
            while (numPlayers - assignedSoFar.size() >= game.getNumRoles()) {
                List<Player> curGroup = Lists.newArrayList();
                //First, get the best player left according to the rankings so far
                Player firstPlayer = getFirstUnassignedPlayer(overallPlayerRankings, assignedSoFar);
                curGroup.add(firstPlayer);
                assignedSoFar.add(firstPlayer);
                while (curGroup.size() < game.getNumRoles()) {
                    //Now we look for the best opponent for those players
                    List<Player> opponentRankings = getOpponentRankingsForPlayers(curGroup, game);
                    Player opponent = getFirstUnassignedPlayer(opponentRankings, assignedSoFar);
                    curGroup.add(opponent);
                    assignedSoFar.add(opponent);
                }
                groups.add(ImmutableList.copyOf(curGroup));
            }

            return groups;
        }

        //TODO: Avoid awarding another bye to a player; when only one player in
        //assignedSoFar has NOT had a bye, (and the total number of players is
        //odd,) remove that player and reserve them for a bye
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
            return getOpponentRankingsForPlayers(ImmutableList.of(firstPlayer), game);
        }

        private List<Player> getOpponentRankingsForPlayers(
                List<Player> curGroup, Game game) {
            List<Player> allOpponents = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
            allOpponents.removeAll(curGroup);

            allOpponents.sort(Comparator.comparing(opponent -> {
                    //Higher points scored by game better
                    //but discount 100/n points per matchup already played in this game
                    double pointsScored = pointsScoredByGame.get(game).get(opponent);
                    for (Player player : curGroup) {
                        pointsScored -= (100.0 / curGroup.size())
                                * matchupsSoFarByGame.get(game).count(ImmutableSet.of(player, opponent));
                    }
                    return pointsScored;
                }
            ).thenComparing(Comparator.comparing(opponent -> {
                    //Higher total points scored better
                    //but discount 100/n points per matchup already played in any Swiss rounds
                    double pointsScored = totalPointsScored.get(opponent);
                    for (Player player : curGroup) {
                        pointsScored -= (100.0 / curGroup.size())
                                * totalMatchupsSoFar.count(ImmutableSet.of(player, opponent));
                    }
                    return pointsScored;
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

        private <K> void addToSumWithKey(K key, double addend, Map<K, Double> map) {
            map.put(key, addend + map.get(key));
        }

        private Optional<Integer> getAttemptNumberIfUnfinished(int groupNum, int matchNum,
                Set<MatchResult> roundResults) {
            int attemptsSoFar = 0;
            for (MatchResult result : roundResults) {
                String matchId = result.getMatchId();
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
                    String matchId = result.getMatchId();
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
                totalPointsScored.put(player, 0.0);
                pointsFromByes.put(player, 0.0);
            }
            Set<Integer> possiblePlayerCounts = Sets.newHashSet();
            for (Game game : RoundSpec.getAllGames(rounds)) {
                HashMap<Player, Double> pointsScoredForGame = Maps.newHashMap();
                pointsScoredByGame.put(game, pointsScoredForGame);
                for (Player player : initialSeeding.getPlayersBestFirst()) {
                    pointsScoredForGame.put(player, 0.0);
                }
                matchupsSoFarByGame.put(game, HashMultiset.create());
                possiblePlayerCounts.add(game.getNumRoles());
            }
            for (int playerCount : possiblePlayerCounts) {
                nonFixedSumMatchupsSoFarByNumPlayers.put(playerCount, HashMultiset.create());
//                Map<Player, Multiset<Integer>> nonFixedSumRoleAssignmentsSoFar = Maps.newHashMap();
//                for (Player player : initialSeeding.getPlayersBestFirst()) {
//                    nonFixedSumRoleAssignmentsSoFar.put(player, HashMultiset.create());
//                }
//                nonFixedSumRoleAssignmentsSoFarByNumPlayers.put(playerCount, nonFixedSumRoleAssignmentsSoFar);
            }

        }

        public NextMatchesResult getMatchesToRun() {
            return StandardNextMatchesResult.create(ImmutableSet.copyOf(matchesToRun),
                    latestStartTimeSeen);
        }

        private Ranking getStandings() {
            Set<PlayerScore> scores = Sets.newHashSet();
            ImmutableList<Player> playersBestFirst = initialSeeding.getPlayersBestFirst();
            for (int i = 0; i < playersBestFirst.size(); i++) {
                Player player = playersBestFirst.get(i);
                double mostRecentGamePoints = 0;
                String mostRecentGameName = null;
                if (mostRecentGame != null) {
                    mostRecentGamePoints = pointsScoredByGame.get(mostRecentGame).get(player);
                    mostRecentGameName = mostRecentGame.getId();
                }
                Score score = new SwissScore(totalPointsScored.get(player),
                        mostRecentGameName, mostRecentGamePoints,
                        pointsFromByes.get(player));
                scores.add(PlayerScore.create(player, score, i));
            }
            return StandardRanking.create(scores);
        }

        public List<Ranking> getStandingsHistory() {
            return ImmutableList.copyOf(standingsHistory);
        }

    }

    private static class SwissScore implements Score {
        //T1K stands for "times 1000".
        private final long pointsSoFarT1K;
        //Just for display purposes; this makes it more obvious why matchups are selected
        private final @Nullable String mostRecentGameName;
        private final long pointsInMostRecentGameT1K;
        private final long pointsFromByesT1K;

        public SwissScore(double pointsSoFar, @Nullable String mostRecentGameName,
                double pointsInMostRecentGame, double pointsFromByes) {
            this.pointsSoFarT1K = roundToThreePlacesT1K(pointsSoFar);
            this.mostRecentGameName = mostRecentGameName;
            this.pointsInMostRecentGameT1K = roundToThreePlacesT1K(pointsInMostRecentGame);
            this.pointsFromByesT1K = roundToThreePlacesT1K(pointsFromByes);
        }

        private static long roundToThreePlacesT1K(double value) {
            return Math.round(value * 1000.0);
        }

        @Override
        public int compareTo(Score other) {
            if (!(other instanceof SwissScore)) {
                throw new IllegalArgumentException();
            }
            return Long.compare(pointsSoFarT1K, ((SwissScore)other).pointsSoFarT1K);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (pointsSoFarT1K ^ (pointsSoFarT1K >>> 32));
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
            if (pointsSoFarT1K != other.pointsSoFarT1K) {
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
            String string;
            if (mostRecentGameName == null) {
                string = addDecimalPoint(pointsSoFarT1K) + " total";
            } else {
                string = addDecimalPoint(pointsSoFarT1K) + " total, " + addDecimalPoint(pointsInMostRecentGameT1K) + " in " + mostRecentGameName;
            }
            if (pointsFromByesT1K > 0L) {
                string += ", " + addDecimalPoint(pointsFromByesT1K) + " from byes";
            }
            return string;
        }

        private String addDecimalPoint(long value) {
            return String.format("%d.%03d", value / 1000, value % 1000);
        }
    }

    @Override
    public NextMatchesResult getMatchesToRun(String tournamentInternalName, Seeding initialSeeding, int stageNum,
            List<RoundSpec> rounds, Set<MatchResult> allResultsSoFar) {
        return SwissFormatSimulator.createAndRun(tournamentInternalName, stageNum, initialSeeding,
                ImmutableList.copyOf(rounds), allResultsSoFar).getMatchesToRun();
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
    public List<Ranking> getStandingsHistory(String tournamentInternalName, Seeding initialSeeding, int stageNum,
            List<RoundSpec> rounds, Set<MatchResult> allResultsSoFar) {
        return SwissFormatSimulator.createAndRun(tournamentInternalName, stageNum, initialSeeding,
                ImmutableList.copyOf(rounds), allResultsSoFar).getStandingsHistory();
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
            //Validates all matches in the round are the same game
            getOnlyGame(round);
        }
    }

    @Immutable
    private static class Swiss1EndOfRoundState implements EndOfRoundState {
        private final int roundNum;
        private final Game mostRecentGame;
        private final ImmutableMap<Player, Double> totalPointsScored;
        private final ImmutableMap<Game, ImmutableMap<Player, Double>> pointsScoredByGame;
        private final ImmutableMap<Player, Double> pointsFromByes;
        private final ImmutableMultiset<ImmutableSet<Player>> totalMatchupsSoFar;
        private final ImmutableMap<Game, ImmutableMultiset<ImmutableSet<Player>>> matchupsSoFarByGame;
        private final ImmutableMap<Integer, ImmutableMultiset<ImmutableSet<Player>>> nonFixedSumMatchupsSoFarByNumPlayers;
        private final ImmutableList<Ranking> standingsHistory;
        private final @Nullable ZonedDateTime latestStartTimeSeen;

        private Swiss1EndOfRoundState(int roundNum, Game mostRecentGame, ImmutableMap<Player, Double> totalPointsScored,
                ImmutableMap<Game, ImmutableMap<Player, Double>> pointsScoredByGame,
                ImmutableMap<Player, Double> pointsFromByes, ImmutableMultiset<ImmutableSet<Player>> totalMatchupsSoFar,
                ImmutableMap<Game, ImmutableMultiset<ImmutableSet<Player>>> matchupsSoFarByGame,
                ImmutableMap<Integer, ImmutableMultiset<ImmutableSet<Player>>> nonFixedSumMatchupsSoFarByNumPlayers,
                ImmutableList<Ranking> standingsHistory, @Nullable ZonedDateTime latestStartTimeSeen) {
            this.roundNum = roundNum;
            this.mostRecentGame = mostRecentGame;
            this.totalPointsScored = totalPointsScored;
            this.pointsScoredByGame = pointsScoredByGame;
            this.pointsFromByes = pointsFromByes;
            this.totalMatchupsSoFar = totalMatchupsSoFar;
            this.matchupsSoFarByGame = matchupsSoFarByGame;
            this.nonFixedSumMatchupsSoFarByNumPlayers = nonFixedSumMatchupsSoFarByNumPlayers;
            this.standingsHistory = standingsHistory;
            this.latestStartTimeSeen = latestStartTimeSeen;
        }

        public static Swiss1EndOfRoundState create(int roundNum,
                Game mostRecentGame,
                Map<Player, Double> totalPointsScored,
                Map<Game, Map<Player, Double>> pointsScoredByGame,
                Map<Player, Double> pointsFromByes,
                Multiset<Set<Player>> totalMatchupsSoFar,
                Map<Game, Multiset<Set<Player>>> matchupsSoFarByGame,
                Map<Integer, Multiset<Set<Player>>> nonFixedSumMatchupsSoFarByNumPlayers,
                List<Ranking> standingsHistory,
                @Nullable ZonedDateTime latestStartTimeSeen) {
            return new Swiss1EndOfRoundState(roundNum,
                    mostRecentGame,
                    ImmutableMap.copyOf(totalPointsScored),
                    toImmutableMapValuedMap(pointsScoredByGame),
                    ImmutableMap.copyOf(pointsFromByes),
                    toImmutableSetEntriedMultiset(totalMatchupsSoFar),
                    toImmutableMultisetOfSetsValuedMap(matchupsSoFarByGame),
                    toImmutableMultisetOfSetsValuedMap(nonFixedSumMatchupsSoFarByNumPlayers),
                    ImmutableList.copyOf(standingsHistory),
                    latestStartTimeSeen);
        }

        private static <K,T> ImmutableMap<K, ImmutableMultiset<ImmutableSet<T>>> toImmutableMultisetOfSetsValuedMap(
                Map<K, Multiset<Set<T>>> map) {
            return ImmutableMap.copyOf(Maps.transformValues(map, Swiss1EndOfRoundState::toImmutableSetEntriedMultiset));
        }

        private static <T> ImmutableMultiset<ImmutableSet<T>> toImmutableSetEntriedMultiset(
                Multiset<Set<T>> multiset) {
            ImmutableMultiset.Builder<ImmutableSet<T>> builder = ImmutableMultiset.builder();
            for (Entry<Set<T>> entry : multiset.entrySet()) {
                builder.addCopies(ImmutableSet.copyOf(entry.getElement()), entry.getCount());
            }
            return builder.build();
        }

        private static <K1,K2,V2> ImmutableMap<K1, ImmutableMap<K2, V2>> toImmutableMapValuedMap(
                Map<K1, Map<K2, V2>> map) {
            return ImmutableMap.copyOf(Maps.transformValues(map, ImmutableMap::copyOf));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((latestStartTimeSeen == null) ? 0 : latestStartTimeSeen.hashCode());
            result = prime * result + ((matchupsSoFarByGame == null) ? 0 : matchupsSoFarByGame.hashCode());
            result = prime * result + ((mostRecentGame == null) ? 0 : mostRecentGame.hashCode());
            result = prime * result + ((nonFixedSumMatchupsSoFarByNumPlayers == null) ? 0
                    : nonFixedSumMatchupsSoFarByNumPlayers.hashCode());
            result = prime * result + ((pointsFromByes == null) ? 0 : pointsFromByes.hashCode());
            result = prime * result + ((pointsScoredByGame == null) ? 0 : pointsScoredByGame.hashCode());
            result = prime * result + roundNum;
            result = prime * result + ((standingsHistory == null) ? 0 : standingsHistory.hashCode());
            result = prime * result + ((totalMatchupsSoFar == null) ? 0 : totalMatchupsSoFar.hashCode());
            result = prime * result + ((totalPointsScored == null) ? 0 : totalPointsScored.hashCode());
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
            Swiss1EndOfRoundState other = (Swiss1EndOfRoundState) obj;
            if (latestStartTimeSeen == null) {
                if (other.latestStartTimeSeen != null) {
                    return false;
                }
            } else if (!latestStartTimeSeen.equals(other.latestStartTimeSeen)) {
                return false;
            }
            if (matchupsSoFarByGame == null) {
                if (other.matchupsSoFarByGame != null) {
                    return false;
                }
            } else if (!matchupsSoFarByGame.equals(other.matchupsSoFarByGame)) {
                return false;
            }
            if (mostRecentGame == null) {
                if (other.mostRecentGame != null) {
                    return false;
                }
            } else if (!mostRecentGame.equals(other.mostRecentGame)) {
                return false;
            }
            if (nonFixedSumMatchupsSoFarByNumPlayers == null) {
                if (other.nonFixedSumMatchupsSoFarByNumPlayers != null) {
                    return false;
                }
            } else if (!nonFixedSumMatchupsSoFarByNumPlayers.equals(other.nonFixedSumMatchupsSoFarByNumPlayers)) {
                return false;
            }
            if (pointsFromByes == null) {
                if (other.pointsFromByes != null) {
                    return false;
                }
            } else if (!pointsFromByes.equals(other.pointsFromByes)) {
                return false;
            }
            if (pointsScoredByGame == null) {
                if (other.pointsScoredByGame != null) {
                    return false;
                }
            } else if (!pointsScoredByGame.equals(other.pointsScoredByGame)) {
                return false;
            }
            if (roundNum != other.roundNum) {
                return false;
            }
            if (standingsHistory == null) {
                if (other.standingsHistory != null) {
                    return false;
                }
            } else if (!standingsHistory.equals(other.standingsHistory)) {
                return false;
            }
            if (totalMatchupsSoFar == null) {
                if (other.totalMatchupsSoFar != null) {
                    return false;
                }
            } else if (!totalMatchupsSoFar.equals(other.totalMatchupsSoFar)) {
                return false;
            }
            if (totalPointsScored == null) {
                if (other.totalPointsScored != null) {
                    return false;
                }
            } else if (!totalPointsScored.equals(other.totalPointsScored)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Swiss1EndOfRoundState [roundNum=" + roundNum + ", mostRecentGame=" + mostRecentGame
                    + ", totalPointsScored=" + totalPointsScored + ", pointsScoredByGame=" + pointsScoredByGame
                    + ", pointsFromByes=" + pointsFromByes + ", totalMatchupsSoFar=" + totalMatchupsSoFar
                    + ", matchupsSoFarByGame=" + matchupsSoFarByGame + ", nonFixedSumMatchupsSoFarByNumPlayers="
                    + nonFixedSumMatchupsSoFarByNumPlayers + ", standingsHistory=" + standingsHistory
                    + ", latestStartTimeSeen=" + latestStartTimeSeen + "]";
        }
    }
}
