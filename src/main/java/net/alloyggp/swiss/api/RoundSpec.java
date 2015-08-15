package net.alloyggp.swiss.api;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

@Immutable
public class RoundSpec {
	//How many games to win? ...
	private final ImmutableList<MatchSpec> matches;

	public ImmutableList<MatchSpec> getMatches() {
		return matches;
	}
}
