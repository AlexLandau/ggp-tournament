package net.alloyggp.tournament.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

@Immutable
public class Seeding {
    private final ImmutableList<Player> playersBestFirst;

    private Seeding(ImmutableList<Player> playersBestFirst) {
        this.playersBestFirst = playersBestFirst;
    }

    /**
     * Creates a random seeding of the given players.
     *
     * <p>To support fuzz testing (and paranoid clients who want to use
     * secure RNGs), the source of randomness is explicitly provided.
     */
    public static Seeding createRandomSeeding(Random random, Collection<Player> players) {
        //Uniquify the players, then put in a mutable list
        List<Player> playersList = Lists.newArrayList(ImmutableSet.copyOf(players));
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

    /**
     * Turns the seeding into a single string so that clients may easily
     * store it durably (e.g. in a database or on the file system). This
     * allows the Seeding to be recovered by calling fromPersistedString
     * with this as the argument.
     *
     * <p>It is recommended that clients store this value durably (along
     * with the tournament specification and match results) instead of in
     * memory so that if the server crashes, the tournament can be
     * continued.
     */
    //TODO: Escape this string properly to prevent commas in player IDs
    // along with newlines and other problems (instead of requiring them
    // to not be in the player ID)
    public String toPersistedString() {
        return playersBestFirst.stream()
            .map(Player::getId)
            .collect(Collectors.joining(","));
    }

    /**
     * Creates the seeding from a string previously created by
     * {@link #toPersistedString()}.
     */
    public static Seeding fromPersistedString(String persistedString) {
        List<Player> players = Lists.newArrayList();
        for (String playerId : Splitter.on(",").split(persistedString)) {
            players.add(Player.create(playerId));
        }
        return create(players);
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
