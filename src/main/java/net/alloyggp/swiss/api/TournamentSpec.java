package net.alloyggp.swiss.api;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

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

	public ImmutableList<StageSpec> getStages() {
		return stages;
	}


}
