package net.alloyggp.swiss.api;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

@Immutable
public class MatchSpec {
	private final Game game;
	private final int startClock;
	private final int playClock;

	public Game getGame() {
		return game;
	}

	public int getStartClock() {
		return startClock;
	}

	public int getPlayClock() {
		return playClock;
	}

	//From seed order (highest first), to the order of their roles
	public ImmutableList<Player> putInOrder(ImmutableList<Player> playersHighestSeedFirst) {
	}
}
