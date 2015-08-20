package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.alloyggp.swiss.FormatRunner;

@Immutable
public class StageSpec {
	private final int stageNum;
	private final StageFormat format;
	//Division into groups? Leave for later
	private final ImmutableList<RoundSpec> rounds;
	private final int playerCutoff;

	private StageSpec(int stageNum, StageFormat format, ImmutableList<RoundSpec> rounds, int playerCutoff) {
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
			playerCutoff = Integer.parseInt((String) stageMap.get("playerCutoff"));
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
		List<PlayerScore> playersBestFirst = ImmutableList.copyOf(standings.getScores()).reverse();
		playersBestFirst = ImmutableList.copyOf(Iterables.limit(playersBestFirst, playerCutoff));
		List<Player> players = playersBestFirst.stream()
									.map(PlayerScore::getPlayer)
									.collect(Collectors.toList());
		return Seeding.create(players);
	}

}
