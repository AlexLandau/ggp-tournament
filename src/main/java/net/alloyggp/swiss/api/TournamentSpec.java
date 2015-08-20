package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Immutable
public class TournamentSpec {
	private final String tournamentInternalName;
	private final String tournamentDisplayName;
	//This could involve multiple formats...
	//How should this interact with TournamentRunner?
	//Typically you'd start with an initial seeding...
	private final ImmutableList<StageSpec> stages;
	//TODO: Also define transitions between stages
	//e.g. turning standings into new seeds, or cutting the number of players

	private TournamentSpec(String tournamentInternalName, String tournamentDisplayName,
			ImmutableList<StageSpec> stages) {
		this.tournamentInternalName = tournamentInternalName;
		this.tournamentDisplayName = tournamentDisplayName;
		this.stages = stages;
	}

	@SuppressWarnings("unchecked")
	public static TournamentSpec parseYamlRootObject(Object yamlRoot) {
		Map<String, Object> rootMap = (Map<String, Object>) yamlRoot;
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
			boolean zeroSum = (boolean) gameMap.get("zeroSum");
			Game game = Game.create(repository, name, numRoles, zeroSum);
			if (results.containsKey(name)) {
				throw new IllegalArgumentException("Can't have two games with the same name defined");
			}
			results.put(name, game);
		}
		return results;
	}

	public String getTournamentInternalName() {
		return tournamentInternalName;
	}

	public String getTournamentDisplayName() {
		return tournamentDisplayName;
	}

	public ImmutableList<StageSpec> getStages() {
		return stages;
	}

	public Set<MatchSetup> getMatchesToRun(Seeding initialSeeding, ImmutableList<MatchResult> resultsSoFar) {
		Seeding seeding = initialSeeding;
		for (StageSpec stage : stages) {
			//TODO: Fix seeding input
			Set<MatchSetup> matchesForStage = stage.getMatchesToRun(tournamentInternalName, initialSeeding, resultsSoFar);
			if (!matchesForStage.isEmpty()) {
				return matchesForStage;
			}
			TournamentStandings standings = stage.getStandingsSoFar(tournamentInternalName, seeding, resultsSoFar);
			seeding = stage.getSeedingsFromFinalStandings(standings);
		}
		//No stages had matches left; the tournament is over
		return ImmutableSet.of();
	}

	public TournamentStandings getCurrentStandings(Seeding initialSeeding, ImmutableList<MatchResult> resultsSoFar) {
		Seeding seeding = initialSeeding;
		TournamentStandings standings = null;
		for (StageSpec stage : stages) {
			//TODO: Fix seeding input
			Set<MatchSetup> matchesForStage = stage.getMatchesToRun(tournamentInternalName, initialSeeding, resultsSoFar);
			standings = stage.getStandingsSoFar(tournamentInternalName, seeding, resultsSoFar);
			if (!matchesForStage.isEmpty()) {
				return standings;
			}
			seeding = stage.getSeedingsFromFinalStandings(standings);
		}
		//No stages had matches left; the tournament is over; use the last set of standings
		Preconditions.checkNotNull(standings);
		return standings;
	}

}
