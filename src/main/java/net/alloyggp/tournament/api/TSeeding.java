package net.alloyggp.tournament.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import net.alloyggp.escaperope.Delimiter;
import net.alloyggp.escaperope.Delimiters;

@Immutable
public class TSeeding {
    private final ImmutableList<TPlayer> playersBestFirst;

    private TSeeding(ImmutableList<TPlayer> playersBestFirst) {
        Preconditions.checkArgument(!playersBestFirst.isEmpty(), "There must be at least one player in a seeding");
        this.playersBestFirst = playersBestFirst;
    }

    /**
     * Creates a random seeding of the given players.
     *
     * <p>To support fuzz testing (and paranoid clients who want to use
     * secure RNGs), the source of randomness is explicitly provided.
     */
    public static TSeeding createRandomSeeding(Random random, Collection<TPlayer> players) {
        //Uniquify the players, then put in a mutable list
        List<TPlayer> playersList = Lists.newArrayList(ImmutableSet.copyOf(players));
        Collections.shuffle(playersList, random);
        return create(playersList);
    }

    /**
     * Creates a non-random seeding of the given players. The players that come earlier
     * in the list are considered better and are given an advantage in tie-breakers.
     */
    public static TSeeding create(List<TPlayer> playersBestFirst) {
        return new TSeeding(ImmutableList.copyOf(playersBestFirst));
    }

    public ImmutableList<TPlayer> getPlayersBestFirst() {
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
    public String toPersistedString() {
        Delimiter delimiter = Delimiters.getEscapeCharDelimiterConvertingNulls(',', '\\');
        String delimited = delimiter.delimit(Lists.transform(playersBestFirst, new Function<TPlayer, String>() {
            @Override
            public String apply(TPlayer player) {
                return player.getId();
            }
        }));
        //Remove the last comma for backwards-compatibility reasons
        Preconditions.checkState(!delimited.isEmpty());
        return delimited.substring(0, delimited.length() - 1);
    }

    /**
     * Creates the seeding from a string previously created by
     * {@link #toPersistedString()}.
     */
    public static TSeeding fromPersistedString(String persistedString) {
        Delimiter delimiter = Delimiters.getEscapeCharDelimiterConvertingNulls(',', '\\');
        List<String> playerIds = delimiter.undelimit(persistedString + ",");

        List<TPlayer> players = Lists.newArrayList();
        for (String playerId : playerIds) {
            players.add(TPlayer.create(playerId));
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
        TSeeding other = (TSeeding) obj;
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
