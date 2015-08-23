package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.alloyggp.swiss.FormatRunner;
import net.alloyggp.swiss.Seedings;

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

	@SuppressWarnings("unchecked")
	public static StageSpec parseYaml(Object yamlStage, int stageNum, Map<String, Game> games) {
		Map<String, Object> stageMap = (Map<String, Object>) yamlStage;
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

	public Set<MatchSetup> getMatchesToRun(String tournamentInternalName, Seeding initialSeeding, List<MatchResult> resultsSoFar) {
		FormatRunner runner = format.getRunner(tournamentInternalName, stageNum);
		return runner.getMatchesToRun(initialSeeding, rounds, resultsSoFar);
	}

	public TournamentStandings getStandingsSoFar(String tournamentInternalName, Seeding initialSeeding, List<MatchResult> resultsSoFar) {
		FormatRunner runner = format.getRunner(tournamentInternalName, stageNum);
		return runner.getStandingsSoFar(initialSeeding, rounds, resultsSoFar);
	}

	public Seeding getSeedingsFromFinalStandings(TournamentStandings standings) {
		return Seedings.getSeedingsFromFinalStandings(standings, playerCutoff);
	}
}
