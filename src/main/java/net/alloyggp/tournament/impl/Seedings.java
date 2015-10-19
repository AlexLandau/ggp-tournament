package net.alloyggp.tournament.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.PlayerScore;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Seeding;

public class Seedings {
    private Seedings() {
        //Not instantiable
    }

    public static Seeding getSeedingsFromFinalStandings(Ranking standings, int playerCutoff) {
        List<PlayerScore> playersBestFirst = ImmutableList.copyOf(
                Iterables.limit(standings.getScores(), playerCutoff));
        List<Player> players = playersBestFirst.stream()
                                    .map(PlayerScore::getPlayer)
                                    .collect(Collectors.toList());
        return Seeding.create(players);
    }
}
