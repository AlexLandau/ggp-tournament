package net.alloyggp.swiss.api;

import java.util.Comparator;

public class PlayerScore implements Comparable<PlayerScore> {
	private final Player player;
	private final Score score;
	private final int seedFromRoundStart;

	private PlayerScore(Player player, Score score, int seedFromRoundStart) {
		this.player = player;
		this.score = score;
		this.seedFromRoundStart = seedFromRoundStart;
	}

	public static PlayerScore create(Player player, Score score, int seedFromRoundStart) {
		return new PlayerScore(player, score, seedFromRoundStart);
	}

	public Player getPlayer() {
		return player;
	}

	public Score getScore() {
		return score;
	}

	public int getSeedFromRoundStart() {
		return seedFromRoundStart;
	}

	@Override
	public int compareTo(PlayerScore o) {
		return Comparator
				.comparing(PlayerScore::getScore)
				.thenComparing(PlayerScore::getSeedFromRoundStart).reversed() //preserve low seed -> better
				.compare(this, o);
	}

}
