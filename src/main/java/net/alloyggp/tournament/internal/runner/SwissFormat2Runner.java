package net.alloyggp.tournament.internal.runner;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.joda.time.DateTime;

import com.google.common.base.Function;
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
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import net.alloyggp.escaperope.rope.ropify.ListRopeWeaver;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.escaperope.rope.ropify.RopeList;
import net.alloyggp.escaperope.rope.ropify.RopeWeaver;
import net.alloyggp.tournament.api.TGame;
import net.alloyggp.tournament.api.TMatchResult.Outcome;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TPlayerScore;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TScore;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.InternalMatchResult;
import net.alloyggp.tournament.internal.MatchId;
import net.alloyggp.tournament.internal.MatchIds;
import net.alloyggp.tournament.internal.MatchResults;
import net.alloyggp.tournament.internal.StandardNextMatchesResult;
import net.alloyggp.tournament.internal.StandardRanking;
import net.alloyggp.tournament.internal.quasirandom.QuasiRandomMatchGenerator;
import net.alloyggp.tournament.internal.quasirandom.RolesFirstImpl3p2;
import net.alloyggp.tournament.internal.spec.MatchSpec;
import net.alloyggp.tournament.internal.spec.RoundSpec;

public class SwissFormat2Runner implements FormatRunner {
    private static final SwissFormat2Runner INSTANCE = new SwissFormat2Runner();

    private static final QuasiRandomMatchGenerator RANDOM_MATCH_GENERATOR = new RolesFirstImpl3p2();

    private SwissFormat2Runner() {
        //Not instantiable
    }

    public static SwissFormat2Runner create() {
        return INSTANCE;
    }

    //TODO: Factor out the common elements between this and SingleEliminationFormatSimulator
    @NotThreadSafe
    private static class SwissFormatSimulator {
        private final String tournamentInternalName;
        private final int stageNum;
        private final TSeeding initialSeeding;
        private final ImmutableList<RoundSpec> rounds;
        private final ImmutableSet<InternalMatchResult> resultsFromEarlierStages;
        private final ImmutableSet<InternalMatchResult> resultsInStage;
        private final Set<TMatchSetup> matchesToRun = Sets.newHashSet();

        private TGame mostRecentGame = null; //of a fully completed round
        private final Map<TPlayer, Double> totalPointsScored = Maps.newHashMap();
        private final Map<Game, Map<TPlayer, Double>> pointsScoredByGame = Maps.newHashMap();
        private final Map<TPlayer, Double> pointsFromByes = Maps.newHashMap();
        private final Multiset<Set<TPlayer>> totalMatchupsSoFar = HashMultiset.create();
        private final Map<Game, Multiset<Set<TPlayer>>> matchupsSoFarByGame = Maps.newHashMap();

        private final ImmutableMap<Integer, List<List<TPlayer>>> randomMatchGroupsByRound;
        private final List<TRanking> standingsHistory = Lists.newArrayList();
        private @Nullable DateTime latestStartTimeSeen = null;

        private SwissFormatSimulator(
                String tournamentInternalName,
                int stageNum,
                TSeeding initialSeeding,
                ImmutableList<RoundSpec> rounds,
                ImmutableSet<InternalMatchResult> resultsFromEarlierStages,
                ImmutableSet<InternalMatchResult> resultsInStage,
                ImmutableMap<Integer, List<List<TPlayer>>> randomMatchGroupsByRound) {
            this.tournamentInternalName = tournamentInternalName;
            this.stageNum = stageNum;
            this.initialSeeding = initialSeeding;
            this.rounds = rounds;
            this.resultsFromEarlierStages = resultsFromEarlierStages;
            this.resultsInStage = resultsInStage;
            this.randomMatchGroupsByRound = randomMatchGroupsByRound;
        }

        public static SwissFormatSimulator createAndRun(String tournamentInternalName, int stageNum, TSeeding initialSeeding,
                ImmutableList<RoundSpec> rounds, Set<InternalMatchResult> allResultsSoFar) {
            Set<InternalMatchResult> resultsFromEarlierStages = MatchResults.getResultsPriorToStage(allResultsSoFar, stageNum);
            Set<InternalMatchResult> resultsInStage = MatchResults.filterByStage(allResultsSoFar, stageNum);
            SwissFormatSimulator simulator = new SwissFormatSimulator(tournamentInternalName, stageNum, initialSeeding,
                    rounds, ImmutableSet.copyOf(resultsFromEarlierStages), ImmutableSet.copyOf(resultsInStage),
                    ImmutableMap.copyOf(getRandomMatchGroupsByRound(rounds, initialSeeding.getPlayersBestFirst())));
            simulator.run();
            return simulator;
        }

        private static Map<Integer, List<List<TPlayer>>> getRandomMatchGroupsByRound(ImmutableList<RoundSpec> rounds, ImmutableList<TPlayer> players) {
            Multiset<Integer> roundsNeedingRandomGroupCounts = HashMultiset.create();
            for (int roundNum = 0; roundNum < rounds.size(); roundNum++) {
                RoundSpec round = rounds.get(roundNum);
                TGame game = getOnlyGame(round);
                if (game.getNumRoles() > 1 && !game.isFixedSum()) {
                    roundsNeedingRandomGroupCounts.add(game.getNumRoles());
                }
            }

            Map<Integer, List<List<TPlayer>>> matchupsByRound = Maps.newHashMap();
            for (Multiset.Entry<Integer> entry : roundsNeedingRandomGroupCounts.entrySet()) {
                int playersPerMatch = entry.getElement();
                int numMatches = entry.getCount();
                if (playersPerMatch > players.size()) {
                    //We won't be playing these rounds
                    continue;
                }
                Deque<List<List<TPlayer>>> matchups = Queues.newArrayDeque(RANDOM_MATCH_GENERATOR.generateMatchups(players, playersPerMatch, numMatches));
                for (int roundNum = 0; roundNum < rounds.size(); roundNum++) {
                    RoundSpec round = rounds.get(roundNum);
                    TGame game = getOnlyGame(round);
                    if (game.getNumRoles() == playersPerMatch && !game.isFixedSum()) {
                        matchupsByRound.put(roundNum, matchups.removeFirst());
                    }
                }
            }
            return matchupsByRound;
        }

        private void run() {
            setInitialTotalsToZero();
            int roundNum = 0;
            SetMultimap<Integer, InternalMatchResult> matchesByRound = MatchResults.mapByRound(resultsInStage, stageNum);

            @Nullable EndOfRoundState endOfRoundState = TournamentStateCache.getLatestCachedEndOfRoundState(tournamentInternalName, initialSeeding, resultsFromEarlierStages, stageNum, resultsInStage);
            if (endOfRoundState != null) {
                Swiss1EndOfRoundState state = (Swiss1EndOfRoundState) endOfRoundState;
                roundNum = state.roundNum + 1;
                loadCachedState(state);
            }

            for (/* roundNum already set */; roundNum < rounds.size(); roundNum++) {
                RoundSpec round = rounds.get(roundNum);
                Set<InternalMatchResult> roundResults = matchesByRound.get(roundNum);
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
                            standingsHistory,
                            latestStartTimeSeen);

                    TournamentStateCache.cacheEndOfRoundState(tournamentInternalName, initialSeeding, resultsFromEarlierStages, stageNum, resultsInStage, state);
                }
            }
        }

        private void loadCachedState(Swiss1EndOfRoundState state) {
            totalPointsScored.putAll(state.totalPointsScored);
            pointsFromByes.putAll(state.pointsFromByes);

            Set<Integer> possiblePlayerCounts = Sets.newHashSet();
            for (TGame game : RoundSpec.getAllGames(rounds)) {
                Map<TPlayer, Double> pointsScoredForGame = pointsScoredByGame.get(game);
                for (TPlayer player : initialSeeding.getPlayersBestFirst()) {
                    pointsScoredForGame.put(player, state.pointsScoredByGame.get(game).get(player));
                }
                for (Entry<ImmutableSet<TPlayer>> entry : state.matchupsSoFarByGame.get(game).entrySet()) {
                    matchupsSoFarByGame.get(game).add(Sets.newHashSet(entry.getElement()), entry.getCount());
                }
                possiblePlayerCounts.add(game.getNumRoles());
            }

            mostRecentGame = state.mostRecentGame;
            standingsHistory.addAll(state.standingsHistory);

            for (Entry<ImmutableSet<TPlayer>> entry : state.totalMatchupsSoFar.entrySet()) {
                totalMatchupsSoFar.add(Sets.newHashSet(entry.getElement()), entry.getCount());
            }
            latestStartTimeSeen = state.latestStartTimeSeen;
        }

        private void runRound(RoundSpec round, int roundNum, Set<InternalMatchResult> roundResults) {
            handleStartTimeForRound(round);
            //...there should be only one match per round, I think?
            //Or at least they must involve the same game?
            TGame game = getOnlyGame(round);
            //Figure out how to assign players
            List<List<TPlayer>> playerGroups = getPlayerGroups(game, roundNum);
            double maxScoreAchieved = 0;
            double scoreSum = 0;
            int scoreCount = 0;
            for (int groupNum = 0; groupNum < playerGroups.size(); groupNum++) {
                List<TPlayer> players = playerGroups.get(groupNum);
                for (int matchNum = 0; matchNum < round.getMatches().size(); matchNum++) {
                    MatchSpec match = round.getMatches().get(matchNum);
                    Optional<Integer> attemptNum = getAttemptNumberIfUnfinished(groupNum, matchNum, roundResults);
                    if (attemptNum.isPresent()) {
                        String matchId = MatchIds.create(stageNum,
                                roundNum, groupNum, matchNum, attemptNum.get());

                        matchesToRun.add(match.createMatchSetup(matchId, players));
                        break;
                    } else {
                        InternalMatchResult result = getSuccessfulAttempt(groupNum, matchNum, roundResults);
                        //Add the results of the match to our point totals
                        List<TPlayer> playersInRoleOrder = match.putInOrder(players);
                        for (int role = 0; role < players.size(); role++) {
                            TPlayer player = playersInRoleOrder.get(role);
                            double goalValue = result.getGoals().get(role) * match.getWeight();

                            addToSumWithKey(player, goalValue, totalPointsScored);
                            addToSumWithKey(player, goalValue, pointsScoredByGame.get(game));

                            maxScoreAchieved = maxScoreAchieved > goalValue ? maxScoreAchieved : goalValue;
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
                Set<TPlayer> unassignedPlayers = getUnassignedPlayers(initialSeeding.getPlayersBestFirst(), playerGroups);
                if (!unassignedPlayers.isEmpty()) {
                    //Calculate bye score for the game
                    double byeScore = getByeScoreForRound(game, maxScoreAchieved, scoreSum, scoreCount);
                    Preconditions.checkState(byeScore >= 0 && byeScore <= 100);
                    for (TPlayer player : unassignedPlayers) {
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
                DateTime roundStartTime = round.getStartTime().get();
                if (latestStartTimeSeen == null
                        || latestStartTimeSeen.isBefore(roundStartTime)) {
                    latestStartTimeSeen = roundStartTime;
                }
            }
        }

        private void updateMatchupStats(TGame game, List<List<TPlayer>> playerGroups) {
            for (List<TPlayer> players : playerGroups) {
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
                }
            }
        }

        private static double getByeScoreForRound(TGame game, double maxScoreAchieved, double scoreSum, int scoreCount) {
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

        private List<List<TPlayer>> getPlayerGroups(TGame game, int roundNum) {
            int numRoles = game.getNumRoles();

            if (numRoles == 1) {
                ImmutableList.Builder<List<TPlayer>> builder = ImmutableList.builder();
                for (TPlayer player : initialSeeding.getPlayersBestFirst()) {
                    builder.add(ImmutableList.of(player));
                }
                return builder.build();
            }
            if (game.isFixedSum()) {
                if (numRoles == 2) {
                    return getTwoPlayerFixedSumPlayerGroups(game);
                } else {
                    return getManyPlayerFixedSumPlayerGroups(game);
                }
            } else {
                //Use quasi-random pairings
                return getNonFixedSumPlayerGroups(roundNum);
            }
        }

        //TODO: Consider prioritizing certain players for byes? e.g. players
        //doing worse in the tournament, those with fewer byes already?
        private List<List<TPlayer>> getNonFixedSumPlayerGroups(int roundNum) {
            List<List<TPlayer>> groups = randomMatchGroupsByRound.get(roundNum);
            if (groups == null) {
                return ImmutableList.of();
            }
            return groups;
        }

        private List<List<TPlayer>> getManyPlayerFixedSumPlayerGroups(TGame game) {
            List<List<TPlayer>> groups = Lists.newArrayList();

            Set<TPlayer> assignedSoFar = Sets.newHashSet();
            List<TPlayer> overallPlayerRankings = getPlayerRankingsForGame(game);
            int numPlayers = initialSeeding.getPlayersBestFirst().size();
            while (numPlayers - assignedSoFar.size() >= game.getNumRoles()) {
                List<TPlayer> curGroup = Lists.newArrayList();
                //First, get the best player left according to the rankings so far
                TPlayer firstPlayer = getFirstUnassignedPlayer(overallPlayerRankings, assignedSoFar);
                curGroup.add(firstPlayer);
                assignedSoFar.add(firstPlayer);
                while (curGroup.size() < game.getNumRoles()) {
                    //Now we look for the best opponent for those players
                    List<TPlayer> opponentRankings = getOpponentRankingsForPlayers(curGroup, game);
                    TPlayer opponent = getFirstUnassignedPlayer(opponentRankings, assignedSoFar);
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
        private List<List<TPlayer>> getTwoPlayerFixedSumPlayerGroups(TGame game) {
            List<List<TPlayer>> groups = Lists.newArrayList();

            Set<TPlayer> assignedSoFar = Sets.newHashSet();
            List<TPlayer> overallPlayerRankings = getPlayerRankingsForGame(game);
            int numPlayers = initialSeeding.getPlayersBestFirst().size();
            while (numPlayers - assignedSoFar.size() >= game.getNumRoles()) {
                //First, get the best player left according to the rankings so far
                TPlayer firstPlayer = getFirstUnassignedPlayer(overallPlayerRankings, assignedSoFar);
                assignedSoFar.add(firstPlayer);
                //Now we look for the best opponent for that player
                List<TPlayer> opponentRankings = getOpponentRankingsForPlayer(firstPlayer, game);
                TPlayer opponent = getFirstUnassignedPlayer(opponentRankings, assignedSoFar);
                assignedSoFar.add(opponent);
                //Best seed goes first
                groups.add(ImmutableList.of(firstPlayer, opponent));
            }

            return groups;
        }

        private List<TPlayer> getOpponentRankingsForPlayer(TPlayer firstPlayer, TGame game) {
            return getOpponentRankingsForPlayers(ImmutableList.of(firstPlayer), game);
        }

        private List<TPlayer> getOpponentRankingsForPlayers(
                final List<TPlayer> curGroup, final TGame game) {
            List<TPlayer> allOpponents = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
            allOpponents.removeAll(curGroup);

            Collections.sort(allOpponents, Ordering.from(
            new Comparator<TPlayer>() {
                @Override
                public int compare(TPlayer o1, TPlayer o2) {
                    double score1 = compute(o1);
                    double score2 = compute(o2);
                    return Double.compare(score1, score2);
                }
                //Higher points scored by game better
                //but discount 100/n points per matchup already played in this game
                private double compute(TPlayer opponent) {
                    double pointsScored = pointsScoredByGame.get(game).get(opponent);
                    for (TPlayer player : curGroup) {
                        pointsScored -= (100.0 / curGroup.size())
                                * matchupsSoFarByGame.get(game).count(ImmutableSet.of(player, opponent));
                    }
                    return pointsScored;
                }
            }
            ).compound(
                    new Comparator<TPlayer>() {
                        @Override
                        public int compare(TPlayer o1, TPlayer o2) {
                            double score1 = compute(o1);
                            double score2 = compute(o2);
                            return Double.compare(score1, score2);
                        }

                        private double compute(TPlayer opponent) {
                            //Higher total points scored better
                            //but discount 100/n points per matchup already played in any Swiss rounds
                            double pointsScored = totalPointsScored.get(opponent);
                            for (TPlayer player : curGroup) {
                                pointsScored -= (100.0 / curGroup.size())
                                        * totalMatchupsSoFar.count(ImmutableSet.of(player, opponent));
                            }
                            return pointsScored;
                        }
                    }
            ).reverse()
                    .compound(new Comparator<TPlayer>() {
                                @Override
                                public int compare(TPlayer p1, TPlayer p2) {
                                    int seed1 = initialSeeding.getPlayersBestFirst().indexOf(p1);
                                    int seed2 = initialSeeding.getPlayersBestFirst().indexOf(p2);
                                    return Integer.compare(seed1, seed2);
                                }
                    }));
            return allOpponents;
        }

        private List<TPlayer> getPlayerRankingsForGame(final TGame game) {
            List<TPlayer> players = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
            //Sort according to an appropriate comparator
            //We do want the best players at the beginning of the list...
            //We may accomplish that through an appropriate reversal
            //First
            Collections.sort(players,
                    Ordering.from(new Comparator<TPlayer>() {
                        @Override
                        public int compare(TPlayer o1, TPlayer o2) {
                            Double score1 = pointsScoredByGame.get(game).get(o1);
                            Double score2 = pointsScoredByGame.get(game).get(o2);
                            return Double.compare(score1, score2);
                        }
                    }).compound(new Comparator<TPlayer>() {
                        @Override
                        public int compare(TPlayer o1, TPlayer o2) {
                            Double score1 = totalPointsScored.get(o1);
                            Double score2 = totalPointsScored.get(o2);
                            return Double.compare(score1, score2);
                        }
                    }).reverse()
                    .compound(new Comparator<TPlayer>() {
                        @Override
                        public int compare(TPlayer o1, TPlayer o2) {
                            int score1 = initialSeeding.getPlayersBestFirst().indexOf(o1);
                            int score2 = initialSeeding.getPlayersBestFirst().indexOf(o2);
                            return Integer.compare(score1, score2);
                        }
                    }));
            return players;
        }

        private TPlayer getFirstUnassignedPlayer(List<TPlayer> players, Set<TPlayer> assignedSoFar) {
            for (TPlayer player : players) {
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
                Set<InternalMatchResult> roundResults) {
            int attemptsSoFar = 0;
            for (InternalMatchResult result : roundResults) {
                MatchId matchId = result.getMatchId();
                if (groupNum == matchId.getPlayerMatchingNumber()
                        && matchNum == matchId.getMatchNumber()) {
                    if (result.getOutcome() == Outcome.ABORTED) {
                        attemptsSoFar++;
                    } else {
                        return Optional.absent();
                    }
                }
            }
            return Optional.of(attemptsSoFar);
        }

        private InternalMatchResult getSuccessfulAttempt(int groupNum, int matchNum, Set<InternalMatchResult> roundResults) {
            for (InternalMatchResult result : roundResults) {
                if (result.getOutcome() == Outcome.COMPLETED) {
                    MatchId matchId = result.getMatchId();
                    if (groupNum == matchId.getPlayerMatchingNumber()
                            && matchNum == matchId.getMatchNumber()) {
                        return result;
                    }
                }
            }
            throw new IllegalArgumentException("No successful attempts found");
        }

        private void setInitialTotalsToZero() {
            for (TPlayer player : initialSeeding.getPlayersBestFirst()) {
                totalPointsScored.put(player, 0.0);
                pointsFromByes.put(player, 0.0);
            }
            Set<Integer> possiblePlayerCounts = Sets.newHashSet();
            for (Game game : RoundSpec.getAllGames(rounds)) {
                HashMap<TPlayer, Double> pointsScoredForGame = Maps.newHashMap();
                pointsScoredByGame.put(game, pointsScoredForGame);
                for (TPlayer player : initialSeeding.getPlayersBestFirst()) {
                    pointsScoredForGame.put(player, 0.0);
                }
                matchupsSoFarByGame.put(game, HashMultiset.<Set<TPlayer>>create());
                possiblePlayerCounts.add(game.getNumRoles());
            }

        }

        public TNextMatchesResult getMatchesToRun() {
            return StandardNextMatchesResult.create(ImmutableSet.copyOf(matchesToRun),
                    latestStartTimeSeen);
        }

        private TRanking getStandings() {
            Set<TPlayerScore> scores = Sets.newHashSet();
            ImmutableList<TPlayer> playersBestFirst = initialSeeding.getPlayersBestFirst();
            for (int i = 0; i < playersBestFirst.size(); i++) {
                TPlayer player = playersBestFirst.get(i);
                double mostRecentGamePoints = 0;
                String mostRecentGameName = null;
                if (mostRecentGame != null) {
                    mostRecentGamePoints = pointsScoredByGame.get(mostRecentGame).get(player);
                    mostRecentGameName = mostRecentGame.getId();
                }
                TScore score = SwissScore.create(totalPointsScored.get(player),
                        mostRecentGameName, mostRecentGamePoints,
                        pointsFromByes.get(player));
                scores.add(TPlayerScore.create(player, score, i));
            }
            return StandardRanking.create(scores);
        }

        public List<TRanking> getStandingsHistory() {
            return ImmutableList.copyOf(standingsHistory);
        }

    }

    private static class SwissScore implements TScore {
        //T1K stands for "times 1000".
        private final long pointsSoFarT1K;
        //Just for display purposes; this makes it more obvious why matchups are selected
        private final @Nullable String mostRecentGameName;
        private final long pointsInMostRecentGameT1K;
        private final long pointsFromByesT1K;

        private SwissScore(long pointsSoFarT1K, String mostRecentGameName, long pointsInMostRecentGameT1K,
                long pointsFromByesT1K) {
            this.pointsSoFarT1K = pointsSoFarT1K;
            this.mostRecentGameName = mostRecentGameName;
            this.pointsInMostRecentGameT1K = pointsInMostRecentGameT1K;
            this.pointsFromByesT1K = pointsFromByesT1K;
        }

        public static SwissScore create(double pointsSoFar, @Nullable String mostRecentGameName,
                double pointsInMostRecentGame, double pointsFromByes) {
            long pointsSoFarT1K = roundToThreePlacesT1K(pointsSoFar);
            long pointsInMostRecentGameT1K = roundToThreePlacesT1K(pointsInMostRecentGame);
            long pointsFromByesT1K = roundToThreePlacesT1K(pointsFromByes);
            return new SwissScore(pointsSoFarT1K, mostRecentGameName, pointsInMostRecentGameT1K, pointsFromByesT1K);
        }

        private static long roundToThreePlacesT1K(double value) {
            return Math.round(value * 1000.0);
        }

        @Override
        public int compareTo(TScore other) {
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

    public static final RopeWeaver<TScore> SCORE_WEAVER = new ListRopeWeaver<TScore>() {
        @Override
        protected void addToList(TScore object, RopeBuilder list) {
            SwissScore score = (SwissScore) object;
            list.add(score.pointsSoFarT1K);
            list.add(score.mostRecentGameName);
            list.add(score.pointsInMostRecentGameT1K);
            list.add(score.pointsFromByesT1K);
        }

        @Override
        protected TScore fromRope(RopeList list) {
            long pointsSoFarT1K = list.getLong(0);
            String mostRecentGameName = list.getString(1);
            long pointsInMostRecentGameT1K = list.getLong(2);
            long pointsFromByesT1K = list.getLong(3);
            return new SwissScore(pointsSoFarT1K, mostRecentGameName, pointsInMostRecentGameT1K, pointsFromByesT1K);
        }
    };

    @Override
    public TNextMatchesResult getMatchesToRun(String tournamentInternalName, TSeeding initialSeeding, int stageNum,
            List<RoundSpec> rounds, Set<InternalMatchResult> allResultsSoFar) {
        return SwissFormatSimulator.createAndRun(tournamentInternalName, stageNum, initialSeeding,
                ImmutableList.copyOf(rounds), allResultsSoFar).getMatchesToRun();
    }

    public static Set<TPlayer> getUnassignedPlayers(Collection<TPlayer> allPlayers, List<List<TPlayer>> playerGroups) {
        Set<TPlayer> results = Sets.newHashSet(allPlayers);
        for (List<TPlayer> group : playerGroups) {
            for (TPlayer player : group) {
                results.remove(player);
            }
        }
        return results;
    }

    @Override
    public List<TRanking> getStandingsHistory(String tournamentInternalName, TSeeding initialSeeding, int stageNum,
            List<RoundSpec> rounds, Set<InternalMatchResult> allResultsSoFar) {
        return SwissFormatSimulator.createAndRun(tournamentInternalName, stageNum, initialSeeding,
                ImmutableList.copyOf(rounds), allResultsSoFar).getStandingsHistory();
    }

    private static TGame getOnlyGame(RoundSpec round) {
        if (round.getMatches().isEmpty()) {
            throw new IllegalArgumentException("Swiss rounds must have at least one match");
        }
        TGame game = round.getMatches().get(0).getGame();
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
        for (RoundSpec round : rounds) {
            //Validates all matches in the round are the same game
            getOnlyGame(round);
        }
    }

    @Immutable
    private static class Swiss1EndOfRoundState implements EndOfRoundState {
        private final int roundNum;
        private final TGame mostRecentGame;
        private final ImmutableMap<TPlayer, Double> totalPointsScored;
        private final ImmutableMap<Game, ImmutableMap<TPlayer, Double>> pointsScoredByGame;
        private final ImmutableMap<TPlayer, Double> pointsFromByes;
        private final ImmutableMultiset<ImmutableSet<TPlayer>> totalMatchupsSoFar;
        private final ImmutableMap<Game, ImmutableMultiset<ImmutableSet<TPlayer>>> matchupsSoFarByGame;
        private final ImmutableList<TRanking> standingsHistory;
        private final @Nullable DateTime latestStartTimeSeen;

        private Swiss1EndOfRoundState(int roundNum, TGame mostRecentGame, ImmutableMap<TPlayer, Double> totalPointsScored,
                ImmutableMap<Game, ImmutableMap<TPlayer, Double>> pointsScoredByGame,
                ImmutableMap<TPlayer, Double> pointsFromByes, ImmutableMultiset<ImmutableSet<TPlayer>> totalMatchupsSoFar,
                ImmutableMap<Game, ImmutableMultiset<ImmutableSet<TPlayer>>> matchupsSoFarByGame,
                ImmutableList<TRanking> standingsHistory, @Nullable DateTime latestStartTimeSeen) {
            this.roundNum = roundNum;
            this.mostRecentGame = mostRecentGame;
            this.totalPointsScored = totalPointsScored;
            this.pointsScoredByGame = pointsScoredByGame;
            this.pointsFromByes = pointsFromByes;
            this.totalMatchupsSoFar = totalMatchupsSoFar;
            this.matchupsSoFarByGame = matchupsSoFarByGame;
            this.standingsHistory = standingsHistory;
            this.latestStartTimeSeen = latestStartTimeSeen;
        }

        public static Swiss1EndOfRoundState create(int roundNum,
                TGame mostRecentGame,
                Map<TPlayer, Double> totalPointsScored,
                Map<Game, Map<TPlayer, Double>> pointsScoredByGame,
                Map<TPlayer, Double> pointsFromByes,
                Multiset<Set<TPlayer>> totalMatchupsSoFar,
                Map<Game, Multiset<Set<TPlayer>>> matchupsSoFarByGame,
                List<TRanking> standingsHistory,
                @Nullable DateTime latestStartTimeSeen) {
            return new Swiss1EndOfRoundState(roundNum,
                    mostRecentGame,
                    ImmutableMap.copyOf(totalPointsScored),
                    toImmutableMapValuedMap(pointsScoredByGame),
                    ImmutableMap.copyOf(pointsFromByes),
                    toImmutableSetEntriedMultiset(totalMatchupsSoFar),
                    toImmutableMultisetOfSetsValuedMap(matchupsSoFarByGame),
                    ImmutableList.copyOf(standingsHistory),
                    latestStartTimeSeen);
        }

        private static <K,T> ImmutableMap<K, ImmutableMultiset<ImmutableSet<T>>> toImmutableMultisetOfSetsValuedMap(
                Map<K, Multiset<Set<T>>> map) {
            return ImmutableMap.copyOf(Maps.transformValues(map, new Function<Multiset<Set<T>>, ImmutableMultiset<ImmutableSet<T>>>() {
                @Override
                public ImmutableMultiset<ImmutableSet<T>> apply(@Nonnull Multiset<Set<T>> input) {
                    return toImmutableSetEntriedMultiset(input);
                }
            }));
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
            return ImmutableMap.copyOf(Maps.transformValues(map, new Function<Map<K2, V2>, ImmutableMap<K2, V2>>() {
                @Override
                public ImmutableMap<K2, V2> apply(@Nonnull Map<K2, V2> input) {
                    return ImmutableMap.copyOf(input);
                }
            }));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((latestStartTimeSeen == null) ? 0 : latestStartTimeSeen.hashCode());
            result = prime * result + ((matchupsSoFarByGame == null) ? 0 : matchupsSoFarByGame.hashCode());
            result = prime * result + ((mostRecentGame == null) ? 0 : mostRecentGame.hashCode());
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
                    + ", matchupsSoFarByGame=" + matchupsSoFarByGame + ", standingsHistory=" + standingsHistory
                    + ", latestStartTimeSeen=" + latestStartTimeSeen + "]";
        }
    }
}
