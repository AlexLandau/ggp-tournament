package net.alloyggp.tournament.impl;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Seeding;

public class Seedings {
    private Seedings() {
        //Not instantiable
    }

    public static Seeding getSeedingsFromFinalStandings(Ranking standings, int playerCutoff) {
        List<Player> eligiblePlayers = ImmutableList.copyOf(
                Iterables.limit(standings.getPlayersBestFirst(), playerCutoff));
        return Seeding.create(eligiblePlayers);
    }
}
