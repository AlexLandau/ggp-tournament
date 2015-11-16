package net.alloyggp.tournament.api;

import javax.annotation.concurrent.Immutable;

/**
 * Note: This class's natural ordering will put the best players at
 * the beginning of the list and the worse players at the end. It uses
 * the seeding from the beginning of the round as a starting point,
 * so it should never return compareTo() == 0 for two different players.
 */
@Immutable
public class TPlayerScore implements Comparable<TPlayerScore> {
    private final TPlayer player;
    private final TScore score;
    private final int seedFromRoundStart;

    private TPlayerScore(TPlayer player, TScore score, int seedFromRoundStart) {
        this.player = player;
        this.score = score;
        this.seedFromRoundStart = seedFromRoundStart;
    }

    public static TPlayerScore create(TPlayer player, TScore score, int seedFromRoundStart) {
        return new TPlayerScore(player, score, seedFromRoundStart);
    }

    public TPlayer getPlayer() {
        return player;
    }

    public TScore getScore() {
        return score;
    }

    public int getSeedFromRoundStart() {
        return seedFromRoundStart;
    }

    @Override
    public int compareTo(TPlayerScore other) {
        //lower PlayerScore is better; higher Score is better
        int scoreComparison = other.score.compareTo(score);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        return Integer.compare(seedFromRoundStart, other.seedFromRoundStart);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((player == null) ? 0 : player.hashCode());
        result = prime * result + ((score == null) ? 0 : score.hashCode());
        result = prime * result + seedFromRoundStart;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TPlayerScore other = (TPlayerScore) obj;
        if (player == null) {
            if (other.player != null) {
                return false;
            }
        } else if (!player.equals(other.player)) {
            return false;
        }
        if (score == null) {
            if (other.score != null) {
                return false;
            }
        } else if (!score.equals(other.score)) {
            return false;
        }
        if (seedFromRoundStart != other.seedFromRoundStart) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PlayerScore [player=" + player + ", score=" + score + ", seedFromRoundStart=" + seedFromRoundStart
                + "]";
    }

}
