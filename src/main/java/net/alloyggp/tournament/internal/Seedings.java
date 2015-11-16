package net.alloyggp.tournament.internal;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;

public class Seedings {
    private Seedings() {
        //Not instantiable
    }

    public static TSeeding getSeedingsFromFinalStandings(TRanking standings, int playerCutoff) {
        List<TPlayer> eligiblePlayers = ImmutableList.copyOf(
                Iterables.limit(standings.getPlayersBestFirst(), playerCutoff));
        return TSeeding.create(eligiblePlayers);
    }
}
