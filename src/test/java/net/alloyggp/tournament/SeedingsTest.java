package net.alloyggp.tournament;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TPlayerScore;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.internal.Seedings;
import net.alloyggp.tournament.internal.SimpleScore;
import net.alloyggp.tournament.internal.StandardRanking;

public class SeedingsTest {
    @Test
    public void testGetSeedingsFromStandings() {
        List<TPlayerScore> scores = Lists.newArrayList();
        List<TPlayer> players = Lists.newArrayList();
        for (int i = 0; i < 5; i++) {
            TPlayer player = TPlayer.create("p" + i);
            scores.add(TPlayerScore.create(player, new SimpleScore(i), i));
            players.add(player);
        }
        TRanking standings = StandardRanking.create(scores);
        ArrayList<TPlayerScore> scoresSorted = Lists.newArrayList(standings.getScores());
        assertEquals(Lists.reverse(scores), scoresSorted);

        TSeeding seedingsCutTo6 = Seedings.getSeedingsFromFinalStandings(standings, 6);
        TSeeding seedingsCutTo5 = Seedings.getSeedingsFromFinalStandings(standings, 5);
        TSeeding seedingsCutTo4 = Seedings.getSeedingsFromFinalStandings(standings, 4);
        TSeeding seedingsCutTo3 = Seedings.getSeedingsFromFinalStandings(standings, 3);
        assertEquals(5, seedingsCutTo6.getPlayersBestFirst().size());
        assertEquals(5, seedingsCutTo5.getPlayersBestFirst().size());
        assertEquals(4, seedingsCutTo4.getPlayersBestFirst().size());
        assertEquals(3, seedingsCutTo3.getPlayersBestFirst().size());
        assertEquals(Lists.reverse(players), seedingsCutTo6.getPlayersBestFirst());
        assertEquals(Lists.reverse(players), seedingsCutTo5.getPlayersBestFirst());
        assertEquals(Lists.reverse(players).subList(0, 4), seedingsCutTo4.getPlayersBestFirst());
        assertEquals(Lists.reverse(players).subList(0, 3), seedingsCutTo3.getPlayersBestFirst());
    }
}
