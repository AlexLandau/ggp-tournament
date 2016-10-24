package net.alloyggp.tournament.internal.spec;

import java.util.Comparator;
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

import net.alloyggp.escaperope.rope.ropify.ListWeaver;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.escaperope.rope.ropify.RopeList;
import net.alloyggp.escaperope.rope.ropify.Weaver;
import net.alloyggp.tournament.api.TAdminAction;
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
import net.alloyggp.tournament.internal.admin.InternalAdminAction;
import net.alloyggp.tournament.internal.rope.Weavers;

@Immutable
public class TournamentSpec implements TTournament {
    private final String tournamentInternalName;
    private final String tournamentDisplayName;
    private final ImmutableList<StageSpec> stages;
    private final ImmutableList<InternalAdminAction> revisionsApplied; //Tracks application of admin actions.
//    private final MatchFilter filter;

    private TournamentSpec(String tournamentInternalName, String tournamentDisplayName,
            ImmutableList<StageSpec> stages, ImmutableList<InternalAdminAction> revisionsApplied) {
        Preconditions.checkNotNull(tournamentInternalName);
        Preconditions.checkNotNull(tournamentDisplayName);
        Preconditions.checkArgument(!stages.isEmpty());
        Preconditions.checkArgument(tournamentInternalName.matches("[a-zA-Z0-9_]+"),
                "Tournament internal name should consist of alphanumerics and underscores, but was %s", tournamentInternalName);
        this.tournamentInternalName = tournamentInternalName;
        this.tournamentDisplayName = tournamentDisplayName;
        this.stages = stages;
        this.revisionsApplied = revisionsApplied;
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
                ImmutableList.copyOf(stages), ImmutableList.<InternalAdminAction>of());
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

    public ImmutableList<? extends TAdminAction> getRevisionsApplied() {
        return revisionsApplied;
    }

    @Override
    public TNextMatchesResult getMatchesToRun(TSeeding initialSeeding, Set<TMatchResult> clientResults,
            List<TAdminAction> adminActions) {
        return applyInternal(adminActions).getMatchesToRun(initialSeeding, clientResults);
    }

    private TNextMatchesResult getMatchesToRun(TSeeding initialSeeding, Set<TMatchResult> clientResults) {
        Set<InternalMatchResult> resultsSoFar = handleInputResults(clientResults);

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
                    seeding, revisionsApplied, resultsSoFar);
            if (!matchesForStage.getMatchesToRun().isEmpty()) {
                return matchesForStage;
            }
            standings = stage.getCurrentStandings(tournamentInternalName,
                    seeding, revisionsApplied, resultsSoFar);
        }
        //No stages had matches left; the tournament is over
        return StandardNextMatchesResult.createEmpty();
    }

    //TODO: In addition to filtering results coming in, we'll want to make sure results
    //going back out have the right numbers. If we're modifying round 3, we don't want to
    //suddenly force a rerun of round 2 as a result, so round 2 should have the older
    //numActionsApplied numbers, but round 3 should have newer numActionsApplied numbers.
    //TODO: Settle on actions vs. revisions for naming
    private Set<InternalMatchResult> handleInputResults(Set<TMatchResult> clientResults) {
        Set<InternalMatchResult> filteredResults = Sets.newHashSet();
        for (TMatchResult unformattedResult : clientResults) {
            InternalMatchResult result = InternalMatchResult.create(unformattedResult);
            int stageNumber = result.getMatchId().getStageNumber();
            Comparator<Integer> roundComparator = stages.get(stageNumber).getFormat().getRoundComparator();
            boolean invalidated = false;
            for (int i = result.getMatchId().getNumActionsApplied(); i < revisionsApplied.size(); i++) {
                InternalAdminAction adminAction = revisionsApplied.get(i);
                if (adminAction.invalidates(result.getMatchId(), roundComparator)) {
                    invalidated = true;
                    break;
                }
            }
            if (!invalidated) {
                filteredResults.add(result);
            }
        }
        return filteredResults;
    }

    @Override
    public TRanking getCurrentStandings(TSeeding initialSeeding,
            Set<TMatchResult> clientResults, List<TAdminAction> adminActions) {
        return applyInternal(adminActions).getCurrentStandings(initialSeeding, clientResults);
    }

    private TRanking getCurrentStandings(TSeeding initialSeeding,
            Set<TMatchResult> clientResults) {
        Set<InternalMatchResult> resultsSoFar = handleInputResults(clientResults);

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
                    seeding, revisionsApplied, resultsSoFar);
            standings = mixInStandings(standings,
                    stage.getCurrentStandings(tournamentInternalName, seeding, revisionsApplied, resultsSoFar));
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

    public static final Weaver<TScore> CUTOFF_SCORE_WEAVER = new ListWeaver<TScore>() {
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
    public List<TRanking> getStandingsHistory(TSeeding initialSeeding, Set<TMatchResult> clientResults,
            List<TAdminAction> adminActions) {
        return applyInternal(adminActions).getStandingsHistory(initialSeeding, clientResults);
    }

    private List<TRanking> getStandingsHistory(TSeeding initialSeeding, Set<TMatchResult> clientResults) {
        List<TRanking> result = Lists.newArrayList();
        result.add(StandardRanking.createForSeeding(initialSeeding));
        TRanking lastStageFinalRanking = null;

        Set<InternalMatchResult> resultsSoFar = handleInputResults(clientResults);

        for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
            StageSpec stage = stages.get(stageNum);
            TSeeding seeding;
            if (stageNum == 0) {
                seeding = initialSeeding;
            } else {
                seeding = stage.getSeedingsFromPreviousStandings(lastStageFinalRanking);
            }
            TNextMatchesResult matchesForStage = stage.getMatchesToRun(tournamentInternalName,
                    seeding, revisionsApplied, resultsSoFar);
            for (TRanking ranking : stage.getStandingsHistory(tournamentInternalName, seeding, revisionsApplied, resultsSoFar)) {
                result.add(mixInStandings(lastStageFinalRanking, ranking));
            }
            if (!matchesForStage.getMatchesToRun().isEmpty()) {
                return result;
            }
            lastStageFinalRanking = mixInStandings(lastStageFinalRanking,
                    stage.getCurrentStandings(tournamentInternalName,
                    seeding, revisionsApplied, resultsSoFar));
        }
        return result;
    }

    @Override
    public Optional<DateTime> getInitialStartTime(List<TAdminAction> adminActions) {
        return applyInternal(adminActions).getInitialStartTime();
    }

    private Optional<DateTime> getInitialStartTime() {
        return stages.get(0).getRounds().get(0).getStartTime();
    }

    @Override
    public long getSecondsToWaitUntilInitialStartTime(List<TAdminAction> adminActions) {
        return TimeUtils.getSecondsToWaitUntilStartTime(getInitialStartTime(adminActions));
    }

    /**
     * Note: This is internal code not covered by the API guarantees. Clients should not be
     * using this directly.
     */
    public TTournament apply(TAdminAction action) {
        if (!(action instanceof InternalAdminAction)) {
            throw new IllegalArgumentException("Custom implementations of TAdminAction are not supported");
        }

        return applyInternal((InternalAdminAction) action);
    }

    private TournamentSpec applyInternal(InternalAdminAction action) {
        ImmutableList.Builder<StageSpec> newStages = ImmutableList.builder();
        for (StageSpec stage : stages) {
            newStages.add(stage.apply(action));
        }
        List<InternalAdminAction> newRevisions = Lists.newArrayList(revisionsApplied);
        newRevisions.add(action);
        return new TournamentSpec(tournamentInternalName, tournamentDisplayName,
                newStages.build(), ImmutableList.copyOf(newRevisions));
    }

    /**
     * Note: This is internal code not covered by the API guarantees. Clients should not be
     * using this directly.
     */
    public TTournament apply(List<TAdminAction> adminActions) {
        return applyInternal(adminActions);
    }

    private TournamentSpec applyInternal(List<TAdminAction> adminActions) {
        List<InternalAdminAction> internalActions = Lists.newArrayList();
        for (TAdminAction action : adminActions) {
            if (!(action instanceof InternalAdminAction)) {
                throw new IllegalArgumentException("Custom implementations of TAdminAction are not supported");
            }
            internalActions.add((InternalAdminAction) action);
        }
        //TODO: This may not be the most efficient way to apply this
        TournamentSpec spec = this;
        for (InternalAdminAction action : internalActions) {
            spec = spec.applyInternal(action);
        }

        return spec;
    }
}
