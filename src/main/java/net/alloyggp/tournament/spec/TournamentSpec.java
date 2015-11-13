package net.alloyggp.tournament.spec;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.Game;
import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.NextMatchesResult;
import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.PlayerScore;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Score;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.api.Tournament;
import net.alloyggp.tournament.api.TournamentSpecParser;
import net.alloyggp.tournament.impl.StandardNextMatchesResult;
import net.alloyggp.tournament.impl.StandardRanking;
import net.alloyggp.tournament.impl.TimeUtils;
import net.alloyggp.tournament.impl.YamlUtils;

@Immutable
public class TournamentSpec implements Tournament {
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
     * <p>For general use, see {@link TournamentSpecParser} instead.
     */
    @SuppressWarnings("unchecked")
    public static TournamentSpec parseYamlRootObject(Object yamlRoot) {
        Map<String, Object> rootMap = (Map<String, Object>) yamlRoot;
        YamlUtils.validateKeys(rootMap, "root", ALLOWED_KEYS);
        Map<String, Game> games = parseGames(rootMap.get("games"));
        String tournamentInternalName = (String) rootMap.get("nameInternal");
        String tournamentDisplayName = (String) rootMap.get("nameDisplay");
        List<StageSpec> stages = Lists.newArrayList();
        int stageNum = 0;
        for (Object yamlStage : (List<Object>) rootMap.get("stages")) {
            stages.add(StageSpec.parseYaml(yamlStage, stageNum, games));
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
            String repository = (String) gameMap.get("repository");
            int numRoles = (int) gameMap.get("numRoles");
            boolean fixedSum = (boolean) gameMap.get("fixedSum");
            Game game = Game.create(repository, name, numRoles, fixedSum);
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
    public NextMatchesResult getMatchesToRun(Seeding initialSeeding, Set<MatchResult> resultsSoFar) {
        Seeding seeding = initialSeeding;
        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            NextMatchesResult matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    seeding, resultsSoFar);
            if (!matchesForStage.getMatchesToRun().isEmpty()) {
                return matchesForStage;
            }
            Ranking standings = stage.getCurrentStandings(tournamentInternalName,
                    seeding, resultsSoFar);
            seeding = stage.getSeedingsFromFinalStandings(standings);
        }
        //No stages had matches left; the tournament is over
        return StandardNextMatchesResult.createEmpty();
    }

    @Override
    public Ranking getCurrentStandings(Seeding initialSeeding,
            Set<MatchResult> resultsSoFar) {
        Seeding seeding = initialSeeding;
        Ranking standings = null;
        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            NextMatchesResult matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    seeding, resultsSoFar);
            standings = mixInStandings(standings,
                    stage.getCurrentStandings(tournamentInternalName, seeding, resultsSoFar));
            if (!matchesForStage.getMatchesToRun().isEmpty()) {
                return standings;
            }
            seeding = stage.getSeedingsFromFinalStandings(standings);
        }
        //No stages had matches left; the tournament is over; use the last set of standings
        Preconditions.checkNotNull(standings);
        return standings;
    }

    private Ranking mixInStandings(Ranking oldStandings,
            Ranking newStandings) {
        if (oldStandings == null) {
            return newStandings;
        }
        //preserve old standings for players that didn't make the cut
        Set<PlayerScore> allPlayerScores = Sets.newHashSet();
        Set<Player> playersInNewerStandings = Sets.newHashSet();
        for (PlayerScore score : newStandings.getScores()) {
            allPlayerScores.add(PlayerScore.create(score.getPlayer(),
                    CutoffScore.madeCutoff(score),
                    score.getSeedFromRoundStart()));
            playersInNewerStandings.add(score.getPlayer());
        }
        for (PlayerScore score : oldStandings.getScores()) {
            if (!playersInNewerStandings.contains(score.getPlayer())) {
                allPlayerScores.add(PlayerScore.create(score.getPlayer(),
                        CutoffScore.failedCutoff(score),
                        score.getSeedFromRoundStart()));
            }
        }
        return StandardRanking.create(allPlayerScores);
    }

    private static class CutoffScore implements Score {
        private final boolean madeCutoff;
        private final Score score;

        private CutoffScore(boolean madeCutoff, Score score) {
            this.madeCutoff = madeCutoff;
            this.score = score;
        }

        public static CutoffScore failedCutoff(PlayerScore score) {
            return new CutoffScore(false, score.getScore());
        }

        public static CutoffScore madeCutoff(PlayerScore score) {
            return new CutoffScore(true, score.getScore());
        }

        @Override
        public int compareTo(Score other) {
            if (!(other instanceof CutoffScore)) {
                throw new IllegalArgumentException();
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
            if (!madeCutoff) {
                return "eliminated";
            } else {
                return score.toString();
            }
        }
    }

    @Override
    public List<Ranking> getStandingsHistory(Seeding initialSeeding, Set<MatchResult> resultsSoFar) {
        List<Ranking> result = Lists.newArrayList();
        result.add(StandardRanking.createForSeeding(initialSeeding));

        Seeding seeding = initialSeeding;
        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            NextMatchesResult matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    seeding, resultsSoFar);
            result.addAll(stage.getStandingsHistory(tournamentInternalName, seeding, resultsSoFar));
            if (!matchesForStage.getMatchesToRun().isEmpty()) {
                return result;
            }
            Ranking standings = stage.getCurrentStandings(tournamentInternalName,
                    seeding, resultsSoFar);
            seeding = stage.getSeedingsFromFinalStandings(standings);
        }
        return result;
    }

    @Override
    public Optional<ZonedDateTime> getInitialStartTime() {
        return stages.get(0).getRounds().get(0).getStartTime();
    }

    @Override
    public long getSecondsToWaitUntilInitialStartTime() {
        return TimeUtils.getSecondsToWaitUntilStartTime(getInitialStartTime());
    }
}
