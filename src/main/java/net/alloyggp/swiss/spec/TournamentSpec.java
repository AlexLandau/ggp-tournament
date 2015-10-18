package net.alloyggp.swiss.spec;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.alloyggp.swiss.MatchResults;
import net.alloyggp.swiss.YamlUtils;
import net.alloyggp.swiss.api.Game;
import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.Player;
import net.alloyggp.swiss.api.PlayerScore;
import net.alloyggp.swiss.api.Score;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.Tournament;
import net.alloyggp.swiss.api.TournamentSpecParser;
import net.alloyggp.swiss.api.TournamentStandings;

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

    /* (non-Javadoc)
     * @see net.alloyggp.swiss.api.Tournament#getTournamentInternalName()
     */
    @Override
    public String getTournamentInternalName() {
        return tournamentInternalName;
    }

    /* (non-Javadoc)
     * @see net.alloyggp.swiss.api.Tournament#getTournamentDisplayName()
     */
    @Override
    public String getTournamentDisplayName() {
        return tournamentDisplayName;
    }

    public ImmutableList<StageSpec> getStages() {
        return stages;
    }

    /* (non-Javadoc)
     * @see net.alloyggp.swiss.api.Tournament#getMatchesToRun(net.alloyggp.swiss.api.Seeding, com.google.common.collect.ImmutableList)
     */
    @Override
    public Set<MatchSetup> getMatchesToRun(Seeding initialSeeding, List<MatchResult> resultsSoFar) {
        Seeding seeding = initialSeeding;
        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            Set<MatchResult> resultsInStage = MatchResults.filterByStage(resultsSoFar, stageNum);
            Set<MatchSetup> matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    initialSeeding, resultsInStage);
            if (!matchesForStage.isEmpty()) {
                return matchesForStage;
            }
            TournamentStandings standings = stage.getCurrentStandings(tournamentInternalName,
                    seeding, resultsInStage);
            seeding = stage.getSeedingsFromFinalStandings(standings);
        }
        //No stages had matches left; the tournament is over
        return ImmutableSet.of();
    }

    /* (non-Javadoc)
     * @see net.alloyggp.swiss.api.Tournament#getCurrentStandings(net.alloyggp.swiss.api.Seeding, com.google.common.collect.ImmutableList)
     */
    @Override
    public TournamentStandings getCurrentStandings(Seeding initialSeeding,
            List<MatchResult> resultsSoFar) {
        Seeding seeding = initialSeeding;
        TournamentStandings standings = null;
        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            Set<MatchResult> resultsInStage = MatchResults.filterByStage(resultsSoFar, stageNum);
            Set<MatchSetup> matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    initialSeeding, resultsInStage);
            standings = mixInStandings(standings,
                    stage.getCurrentStandings(tournamentInternalName, seeding, resultsInStage));
            if (!matchesForStage.isEmpty()) {
                return standings;
            }
            seeding = stage.getSeedingsFromFinalStandings(standings);
        }
        //No stages had matches left; the tournament is over; use the last set of standings
        Preconditions.checkNotNull(standings);
        return standings;
    }

    private TournamentStandings mixInStandings(TournamentStandings oldStandings,
            TournamentStandings newStandings) {
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
        return TournamentStandings.create(allPlayerScores);
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
    public List<TournamentStandings> getStandingsHistory(Seeding initialSeeding, List<MatchResult> resultsSoFar) {
        List<TournamentStandings> result = Lists.newArrayList();
        result.add(TournamentStandings.createForSeeding(initialSeeding));

        Seeding seeding = initialSeeding;
        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            Set<MatchResult> resultsInStage = MatchResults.filterByStage(resultsSoFar, stageNum);
            Set<MatchSetup> matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    initialSeeding, resultsInStage);
            result.addAll(stage.getStandingsHistory(tournamentInternalName, initialSeeding, resultsInStage));
            if (!matchesForStage.isEmpty()) {
                return result;
            }
            TournamentStandings standings = stage.getCurrentStandings(tournamentInternalName,
                    seeding, resultsInStage);
            seeding = stage.getSeedingsFromFinalStandings(standings);
        }
        return result;
    }

    public static class EmptyScore implements Score {
        private EmptyScore() {
            // Use create()
        }

        @Override
        public int compareTo(Score o) {
            if (!(o instanceof EmptyScore)) {
                throw new IllegalArgumentException("Incomparable scores being compared");
            }
            return 0;
        }

        public static Score create() {
            return new EmptyScore();
        }

        @Override
        public int hashCode() {
            return 1;
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
            return true;
        }

        @Override
        public String toString() {
            return "initial seeding for stage";
        }
    }

}
