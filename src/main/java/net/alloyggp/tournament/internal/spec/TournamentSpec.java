package net.alloyggp.tournament.internal.spec;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.Immutable;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.alloyggp.escaperope.rope.ropify.ListRopeWeaver;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.escaperope.rope.ropify.RopeList;
import net.alloyggp.escaperope.rope.ropify.RopeWeaver;
import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TPlayerScore;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TScore;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentSpecParser;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.InternalMatchResult;
import net.alloyggp.tournament.internal.StandardNextMatchesResult;
import net.alloyggp.tournament.internal.StandardRanking;
import net.alloyggp.tournament.internal.TimeUtils;
import net.alloyggp.tournament.internal.YamlUtils;
import net.alloyggp.tournament.internal.rope.Weavers;

@Immutable
public class TournamentSpec implements TTournament {
    private final String tournamentInternalName;
    private final String tournamentDisplayName;
    private final ImmutableList<StageSpec> stages;

    private TournamentSpec(String tournamentInternalName, String tournamentDisplayName,
            ImmutableList<StageSpec> stages) {
        Preconditions.checkNotNull(tournamentInternalName);
        Preconditions.checkNotNull(tournamentDisplayName);
        Preconditions.checkArgument(!stages.isEmpty());
        Preconditions.checkArgument(tournamentInternalName.matches("[a-zA-Z0-9_]+"),
                "Tournament internal name should consist of alphanumerics and underscores, but was %s", tournamentInternalName);
        this.tournamentInternalName = tournamentInternalName;
        this.tournamentDisplayName = tournamentDisplayName;
        this.stages = stages;
    }

    private static final ImmutableSet<String> ALLOWED_KEYS = ImmutableSet.of(
            "games",
            "nameInternal",
            "nameDisplay",
            "stages"
            );
    /**
     * Parses an already-loaded YAML object containing a tournament specification.
     *
     * <p>For general use, see {@link TTournamentSpecParser} instead.
     */
    @SuppressWarnings("unchecked")
    public static TournamentSpec parseYamlRootObject(Object yamlRoot) {
        Map<String, Object> rootMap = (Map<String, Object>) yamlRoot;
        YamlUtils.validateKeys(rootMap, "root", ALLOWED_KEYS);
        Map<String, Game> games = parseGames(rootMap.get("games"));
        String tournamentInternalName = (String) rootMap.get("nameInternal");
        String tournamentDisplayName = (String) rootMap.get("nameDisplay");
        List<StageSpec> stages = Lists.newArrayList();
        AtomicInteger ongoingPlayerLimit = new AtomicInteger(Integer.MAX_VALUE);
        int stageNum = 0;
        for (Object yamlStage : (List<Object>) rootMap.get("stages")) {
            stages.add(StageSpec.parseYaml(yamlStage, stageNum, games, ongoingPlayerLimit));
            stageNum++;
        }
        return new TournamentSpec(tournamentInternalName, tournamentDisplayName,
                ImmutableList.copyOf(stages));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Game> parseGames(Object gamesYaml) {
        Preconditions.checkNotNull(gamesYaml, "The YAML file must have a 'games' section.");
        Map<String, Game> results = Maps.newHashMap();
        for (Object gameYaml : (List<Object>) gamesYaml) {
            Map<String, Object> gameMap = (Map<String, Object>) gameYaml;
            String name = (String) gameMap.get("name");
            String url = (String) gameMap.get("url");
            int numRoles = (int) gameMap.get("numRoles");
            boolean fixedSum = (boolean) gameMap.get("fixedSum");
            Game game = Game.create(name, url, numRoles, fixedSum);
            if (results.containsKey(name)) {
                throw new IllegalArgumentException("Can't have two games with the same name defined");
            }
            results.put(name, game);
        }
        return results;
    }

    @Override
    public String getInternalName() {
        return tournamentInternalName;
    }

    @Override
    public String getDisplayName() {
        return tournamentDisplayName;
    }

    public ImmutableList<StageSpec> getStages() {
        return stages;
    }

    @Override
    public TNextMatchesResult getMatchesToRun(TSeeding initialSeeding, Set<TMatchResult> clientResults) {
        Set<InternalMatchResult> resultsSoFar = InternalMatchResult.convertResults(clientResults);

        TRanking standings = null;
        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            TSeeding seeding;
            if (stageNum == 0) {
                seeding = initialSeeding;
            } else {
                seeding = stage.getSeedingsFromPreviousStandings(standings);
            }
            TNextMatchesResult matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    seeding, resultsSoFar);
            if (!matchesForStage.getMatchesToRun().isEmpty()) {
                return matchesForStage;
            }
            standings = stage.getCurrentStandings(tournamentInternalName,
                    seeding, resultsSoFar);
        }
        //No stages had matches left; the tournament is over
        return StandardNextMatchesResult.createEmpty();
    }

    @Override
    public TRanking getCurrentStandings(TSeeding initialSeeding,
            Set<TMatchResult> clientResults) {
        Set<InternalMatchResult> resultsSoFar = InternalMatchResult.convertResults(clientResults);

        TRanking standings = null;
        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            TSeeding seeding;
            if (stageNum == 0) {
                seeding = initialSeeding;
            } else {
                seeding = stage.getSeedingsFromPreviousStandings(standings);
            }
            TNextMatchesResult matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    seeding, resultsSoFar);
            standings = mixInStandings(standings,
                    stage.getCurrentStandings(tournamentInternalName, seeding, resultsSoFar));
            if (!matchesForStage.getMatchesToRun().isEmpty()) {
                return standings;
            }
        }
        //No stages had matches left; the tournament is over; use the last set of standings
        Preconditions.checkNotNull(standings);
        return standings;
    }

    private TRanking mixInStandings(TRanking oldStandings,
            TRanking newStandings) {
        if (oldStandings == null) {
            return newStandings;
        }
        //preserve old standings for players that didn't make the cut
        Set<TPlayerScore> allPlayerScores = Sets.newHashSet();
        Set<TPlayer> playersInNewerStandings = Sets.newHashSet();
        for (TPlayerScore score : newStandings.getScores()) {
            allPlayerScores.add(TPlayerScore.create(score.getPlayer(),
                    CutoffScore.madeCutoff(score),
                    score.getSeedFromRoundStart()));
            playersInNewerStandings.add(score.getPlayer());
        }
        for (TPlayerScore score : oldStandings.getScores()) {
            if (!playersInNewerStandings.contains(score.getPlayer())) {
                allPlayerScores.add(TPlayerScore.create(score.getPlayer(),
                        CutoffScore.failedCutoff(score),
                        score.getSeedFromRoundStart()));
            }
        }
        Preconditions.checkState(oldStandings.getPlayersBestFirst().size() == allPlayerScores.size());
        return StandardRanking.create(allPlayerScores);
    }

    private static class CutoffScore implements TScore {
        private final boolean madeCutoff;
        private final TScore score;

        private CutoffScore(boolean madeCutoff, TScore score) {
            this.madeCutoff = madeCutoff;
            this.score = score;
        }

        public static CutoffScore failedCutoff(TPlayerScore score) {
            return new CutoffScore(false, score.getScore());
        }

        public static CutoffScore madeCutoff(TPlayerScore score) {
            return new CutoffScore(true, score.getScore());
        }

        @Override
        public int compareTo(TScore other) {
            if (!(other instanceof CutoffScore)) {
                throw new ClassCastException("Expected a CutoffScore, was " + other.getClass());
            }
            CutoffScore otherCutoff = (CutoffScore) other;
            if (!madeCutoff && otherCutoff.madeCutoff) {
                return -1;
            } else if (madeCutoff && !otherCutoff.madeCutoff) {
                return 1;
            } else {
                return score.compareTo(otherCutoff.score);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (madeCutoff ? 1231 : 1237);
            result = prime * result + ((score == null) ? 0 : score.hashCode());
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
            CutoffScore other = (CutoffScore) obj;
            if (madeCutoff != other.madeCutoff) {
                return false;
            }
            if (score == null) {
                if (other.score != null) {
                    return false;
                }
            } else if (!score.equals(other.score)) {
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
            if (!madeCutoff) {
                return "eliminated";
            } else {
                return score.toString();
            }
        }
    }

    public static final RopeWeaver<TScore> CUTOFF_SCORE_WEAVER = new ListRopeWeaver<TScore>() {
        @Override
        protected void addToList(TScore object, RopeBuilder list) {
            CutoffScore score = (CutoffScore) object;
            list.add(score.madeCutoff);
            list.add(score.score, Weavers.SCORE);
        }

        @Override
        protected TScore fromRope(RopeList list) {
            boolean madeCutoff = list.getBoolean(0);
            TScore score = list.get(1, Weavers.SCORE);
            return new CutoffScore(madeCutoff, score);
        }
    };

    @Override
    public List<TRanking> getStandingsHistory(TSeeding initialSeeding, Set<TMatchResult> clientResults) {
        List<TRanking> result = Lists.newArrayList();
        result.add(StandardRanking.createForSeeding(initialSeeding));
        TRanking lastStageFinalRanking = null;

        Set<InternalMatchResult> resultsSoFar = InternalMatchResult.convertResults(clientResults);

        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            TSeeding seeding;
            if (stageNum == 0) {
                seeding = initialSeeding;
            } else {
                seeding = stage.getSeedingsFromPreviousStandings(lastStageFinalRanking);
            }
            TNextMatchesResult matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    seeding, resultsSoFar);
            for (TRanking ranking : stage.getStandingsHistory(tournamentInternalName, seeding, resultsSoFar)) {
                result.add(mixInStandings(lastStageFinalRanking, ranking));
            }
            if (!matchesForStage.getMatchesToRun().isEmpty()) {
                return result;
            }
            lastStageFinalRanking = mixInStandings(lastStageFinalRanking,
                    stage.getCurrentStandings(tournamentInternalName,
                    seeding, resultsSoFar));
        }
        return result;
    }

    @Override
    public Optional<DateTime> getInitialStartTime() {
        return stages.get(0).getRounds().get(0).getStartTime();
    }

    @Override
    public long getSecondsToWaitUntilInitialStartTime() {
        return TimeUtils.getSecondsToWaitUntilStartTime(getInitialStartTime());
    }
}
