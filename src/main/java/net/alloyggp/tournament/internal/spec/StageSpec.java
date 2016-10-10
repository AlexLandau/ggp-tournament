package net.alloyggp.tournament.internal.spec;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.InternalMatchResult;
import net.alloyggp.tournament.internal.Seedings;
import net.alloyggp.tournament.internal.StandardRanking;
import net.alloyggp.tournament.internal.YamlUtils;
import net.alloyggp.tournament.internal.admin.InternalAdminAction;
import net.alloyggp.tournament.internal.runner.FormatRunner;

@Immutable
public class StageSpec {
    private final int stageNum;
    private final StageFormat format;
    //Division into groups? Leave for later
    private final ImmutableList<RoundSpec> rounds;
    private final int playerLimit;
    private final ImmutableSet<TPlayer> excludedPlayers;

    private StageSpec(int stageNum, StageFormat format,
            ImmutableList<RoundSpec> rounds, int playerLimit,
            ImmutableSet<TPlayer> excludedPlayers) {
        Preconditions.checkArgument(stageNum >= 0);
        Preconditions.checkNotNull(format);
        Preconditions.checkArgument(!rounds.isEmpty());
        Preconditions.checkArgument(playerLimit > 0, "Player cutoff must be positive if present");
        format.validateRounds(rounds);
        this.stageNum = stageNum;
        this.format = format;
        this.rounds = rounds;
        this.playerLimit = playerLimit;
        this.excludedPlayers = excludedPlayers;
    }

    private static final ImmutableSet<String> ALLOWED_KEYS = ImmutableSet.of(
            "format",
            "rounds",
            "playerCutoff",
            "excludedPlayers"
            );

    @SuppressWarnings("unchecked")
    public static StageSpec parseYaml(Object yamlStage, int stageNum, Map<String, Game> games,
            AtomicInteger playerLimitSoFar) {
        Map<String, Object> stageMap = (Map<String, Object>) yamlStage;
        YamlUtils.validateKeys(stageMap, "stage", ALLOWED_KEYS);
        String formatName = (String) stageMap.get("format");
        List<RoundSpec> rounds = Lists.newArrayList();
        int roundNum = 0;
        for (Object yamlRound : (List<Object>) stageMap.get("rounds")) {
            rounds.add(RoundSpec.parseYaml(yamlRound, roundNum, games));
            roundNum++;
        }
        int playerLimit = playerLimitSoFar.get();
        if (stageMap.containsKey("playerLimit")) {
            playerLimit = Math.min(playerLimit, (int) stageMap.get("playerLimit"));
            playerLimitSoFar.set(playerLimit);
        }
        if (stageMap.containsKey("playerCutoff")) {
            //This affects the player limit for the next round, not the current round.
            playerLimitSoFar.set((int) stageMap.get("playerCutoff"));
        }
        Set<TPlayer> excludedPlayers = Sets.newHashSet();
        if (stageMap.containsKey("excludedPlayers")) {
            for (Object playerName : (List<Object>) stageMap.get("excludedPlayers")) {
                // Note that playerName could actually be e.g. an Integer instead
                // of a String, so we do need toString() instead of a cast.
                excludedPlayers.add(TPlayer.create(playerName.toString()));
            }
        }
        return new StageSpec(stageNum, StageFormat.parse(formatName),
                ImmutableList.copyOf(rounds), playerLimit,
                ImmutableSet.copyOf(excludedPlayers));
    }

    public TNextMatchesResult getMatchesToRun(String tournamentInternalName,
            TSeeding initialSeeding, List<InternalAdminAction> adminActions, Set<InternalMatchResult> resultsSoFar) {
        FormatRunner runner = format.getRunner();
        return runner.getMatchesToRun(tournamentInternalName, initialSeeding, adminActions, stageNum,
                rounds, resultsSoFar);
    }

    public TRanking getCurrentStandings(String tournamentInternalName,
            TSeeding initialSeeding, List<InternalAdminAction> adminActions, Set<InternalMatchResult> resultsSoFar) {
        List<TRanking> standingsHistory = getStandingsHistory(tournamentInternalName, initialSeeding, adminActions, resultsSoFar);
        if (standingsHistory.isEmpty()) {
            return StandardRanking.createForSeeding(initialSeeding);
        }
        return standingsHistory.get(standingsHistory.size() - 1);
    }

    public int getStageNum() {
        return stageNum;
    }

    public StageFormat getFormat() {
        return format;
    }

    public TSeeding getSeedingsFromPreviousStandings(TRanking standings) {
        Preconditions.checkNotNull(standings);
        return Seedings.getSeedingsFromFinalStandings(standings, playerLimit, excludedPlayers);
    }

    public ImmutableList<RoundSpec> getRounds() {
        return rounds;
    }

    public int getPlayerLimit() {
        return playerLimit;
    }

    public ImmutableSet<TPlayer> getExcludedPlayers() {
        return excludedPlayers;
    }

    public List<TRanking> getStandingsHistory(String tournamentInternalName,
            TSeeding initialSeeding, List<InternalAdminAction> adminActions, Set<InternalMatchResult> resultsSoFar) {
        return format.getRunner().getStandingsHistory(tournamentInternalName, initialSeeding,
                adminActions, stageNum, rounds, resultsSoFar);
    }

    public StageSpec apply(InternalAdminAction action) {
        StageFormat newFormat = action.editStageFormat(stageNum, format);
        int newPlayerLimit = action.editStagePlayerLimit(stageNum, playerLimit);
        Set<TPlayer> newExcludedPlayers = action.editStageExcludedPlayers(stageNum, excludedPlayers);
        List<RoundSpec> newRounds = Lists.newArrayList();
        for (RoundSpec round : rounds) {
            newRounds.add(round.apply(action, stageNum));
        }

        return new StageSpec(stageNum, newFormat, ImmutableList.copyOf(newRounds),
                newPlayerLimit, ImmutableSet.copyOf(newExcludedPlayers));
    }
}
