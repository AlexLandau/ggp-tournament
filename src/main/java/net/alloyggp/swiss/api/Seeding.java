package net.alloyggp.swiss.api;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class Seeding {
    private final ImmutableList<Player> playersBestFirst;

    private Seeding(ImmutableList<Player> playersBestFirst) {
        this.playersBestFirst = playersBestFirst;
    }

    /**
     * Creates a random seeding of the given players.
     *
     * <p>To support fuzz testing, the source of randomness is explicitly provided.
     */
    public static Seeding createRandomSeeding(Random random, List<Player> players) {
        List<Player> playersList = Lists.newArrayList(players);
        Collections.shuffle(playersList, random);
        return create(playersList);
    }

    /**
     * Creates a non-random seeding of the given players. The players that come earlier
     * in the list are considered better and are given an advantage in tie-breakers.
     */
    public static Seeding create(List<Player> playersBestFirst) {
        return new Seeding(ImmutableList.copyOf(playersBestFirst));
    }

    public ImmutableList<Player> getPlayersBestFirst() {
        return playersBestFirst;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((playersBestFirst == null) ? 0 : playersBestFirst.hashCode());
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
        Seeding other = (Seeding) obj;
        if (playersBestFirst == null) {
            if (other.playersBestFirst != null) {
                return false;
            }
        } else if (!playersBestFirst.equals(other.playersBestFirst)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Seeding [playersBestFirst=" + playersBestFirst + "]";
    }
}
