package net.alloyggp.swiss.api;

import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
