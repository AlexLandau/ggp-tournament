package net.alloyggp.tournament.internal;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;

public class Seedings {
    private Seedings() {
        //Not instantiable
    }

    public static TSeeding getSeedingsFromFinalStandings(TRanking standings, int playerCutoff,
            Set<TPlayer> playersIneligibleForNextStage) {
        List<TPlayer> eligiblePlayers = Lists.newArrayList(standings.getPlayersBestFirst());
        eligiblePlayers.removeAll(playersIneligibleForNextStage);
        List<TPlayer> chosenPlayers = ImmutableList.copyOf(
                Iterables.limit(eligiblePlayers, playerCutoff));
        return TSeeding.create(chosenPlayers);
    }
}
