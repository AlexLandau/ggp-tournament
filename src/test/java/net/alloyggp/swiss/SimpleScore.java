package net.alloyggp.swiss;

import net.alloyggp.swiss.api.Score;

public class SimpleScore implements Score {
	private final int score;

	public SimpleScore(int score) {
		this.score = score;
	}

	@Override
	public int compareTo(Score o) {
		if (!(o instanceof SimpleScore)) {
			throw new IllegalArgumentException();
		}
		return Integer.compare(score, ((SimpleScore)o).score);
	}
}
