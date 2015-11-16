package net.alloyggp.tournament.api;

import javax.annotation.concurrent.Immutable;

/**
 * Note: This class's natural ordering will put the best players at
 * the beginning of the list and the worse players at the end. It uses
 * the seeding from the beginning of the round as a starting point,
 * so it should never return compareTo() == 0 for two different players.
 */
@Immutable
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
    public int compareTo(PlayerScore other) {
        int scoreComparison = score.compareTo(other.score);
        if (scoreComparison != 0) {
            //lower PlayerScore is better; higher Score is better
            return -scoreComparison;
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
        PlayerScore other = (PlayerScore) obj;
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
