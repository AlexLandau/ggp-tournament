package net.alloyggp.swiss.api;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

@Immutable
public class MatchSpec {
	private final Game game;
	private final int startClock;
	private final int playClock;
	private final ImmutableList<Integer> playerSeedOrder;

	private MatchSpec(Game game, int startClock, int playClock, ImmutableList<Integer> playerSeedOrder) {
		this.game = game;
		this.startClock = startClock;
		this.playClock = playClock;
		this.playerSeedOrder = playerSeedOrder;
	}

	@SuppressWarnings("unchecked")
	public static MatchSpec parseYaml(Object yamlMatch) {
		Map<String, Object> matchMap = (Map<String, Object>) yamlMatch;
		matchMap.get("game");
		//TODO: Actually deal with games correctly
		Game game = Game.create("foo", "bar");
		int startClock = (int) matchMap.get("startClock");
		int playClock = (int) matchMap.get("playClock");
		//TODO: Actual player seed order from YAML
		ImmutableList<Integer> playerSeedOrder = ImmutableList.of(0, 1);
		return new MatchSpec(game, startClock, playClock, playerSeedOrder);
	}

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
		ImmutableList.Builder<Player> players = ImmutableList.builder();
		for (int seed : playerSeedOrder) {
			players.add(playersHighestSeedFirst.get(seed));
		}
		return players.build();
	}

}
