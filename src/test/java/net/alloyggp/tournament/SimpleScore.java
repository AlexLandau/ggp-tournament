package net.alloyggp.tournament;

import net.alloyggp.tournament.api.TScore;

public class SimpleScore implements TScore {
    private final int score;

    public SimpleScore(int score) {
        this.score = score;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + score;
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
        SimpleScore other = (SimpleScore) obj;
        if (score != other.score) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(TScore other) {
        if (!(other instanceof SimpleScore)) {
            throw new IllegalArgumentException();
        }
        return Integer.compare(score, ((SimpleScore)other).score);
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public String getDescription() {
        return Integer.toString(score);
    }
}
