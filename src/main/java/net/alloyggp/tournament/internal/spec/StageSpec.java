package net.alloyggp.tournament.internal.spec;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import net.alloyggp.tournament.api.TNextMatchesResult;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.InternalMatchResult;
import net.alloyggp.tournament.internal.Seedings;
import net.alloyggp.tournament.internal.StandardRanking;
import net.alloyggp.tournament.internal.YamlUtils;
import net.alloyggp.tournament.internal.runner.FormatRunner;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

@Immutable
public class StageSpec {
    private final int stageNum;
    private final StageFormat format;
    //Division into groups? Leave for later
    private final ImmutableList<RoundSpec> rounds;
    private final int playerCutoff;

    private StageSpec(int stageNum, StageFormat format, ImmutableList<RoundSpec> rounds, int playerCutoff) {
        Preconditions.checkArgument(stageNum >= 0);
        Preconditions.checkNotNull(format);
        Preconditions.checkArgument(!rounds.isEmpty());
        Preconditions.checkArgument(playerCutoff > 0, "Player cutoff must be positive if present");
        format.validateRounds(rounds);
        this.stageNum = stageNum;
        this.format = format;
        this.rounds = rounds;
        this.playerCutoff = playerCutoff;
    }

    private static final ImmutableSet<String> ALLOWED_KEYS = ImmutableSet.of(
            "format",
            "rounds",
            "playerCutoff"
            );

    @SuppressWarnings("unchecked")
    public static StageSpec parseYaml(Object yamlStage, int stageNum, Map<String, Game> games) {
        Map<String, Object> stageMap = (Map<String, Object>) yamlStage;
        YamlUtils.validateKeys(stageMap, "stage", ALLOWED_KEYS);
        String formatName = (String) stageMap.get("format");
        List<RoundSpec> rounds = Lists.newArrayList();
        for (Object yamlRound : (List<Object>) stageMap.get("rounds")) {
            rounds.add(RoundSpec.parseYaml(yamlRound, games));
        }
        int playerCutoff = Integer.MAX_VALUE;
        if (stageMap.containsKey("playerCutoff")) {
            playerCutoff = (int) stageMap.get("playerCutoff");
        }
        return new StageSpec(stageNum, StageFormat.parse(formatName),
                ImmutableList.copyOf(rounds), playerCutoff);
    }

    public TNextMatchesResult getMatchesToRun(String tournamentInternalName,
            TSeeding initialSeeding, Set<InternalMatchResult> resultsSoFar) {
        FormatRunner runner = format.getRunner();
        return runner.getMatchesToRun(tournamentInternalName, initialSeeding, stageNum,
                rounds, resultsSoFar);
    }

    public TRanking getCurrentStandings(String tournamentInternalName,
            TSeeding initialSeeding, Set<InternalMatchResult> resultsSoFar) {
        List<TRanking> standingsHistory = getStandingsHistory(tournamentInternalName, initialSeeding, resultsSoFar);
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

    public TSeeding getSeedingsFromFinalStandings(TRanking standings) {
        return Seedings.getSeedingsFromFinalStandings(standings, playerCutoff);
    }

    public ImmutableList<RoundSpec> getRounds() {
        return rounds;
    }

    public int getPlayerCutoff() {
        return playerCutoff;
    }

    public List<TRanking> getStandingsHistory(String tournamentInternalName,
            TSeeding initialSeeding, Set<InternalMatchResult> resultsSoFar) {
        return format.getRunner().getStandingsHistory(tournamentInternalName, initialSeeding,
                stageNum, rounds, resultsSoFar);
    }
}
