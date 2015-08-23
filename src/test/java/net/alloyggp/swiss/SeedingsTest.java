package net.alloyggp.swiss;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import net.alloyggp.swiss.api.Player;
import net.alloyggp.swiss.api.PlayerScore;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.TournamentStandings;

public class SeedingsTest {
    @Test
    public void testGetSeedingsFromStandings() {
        List<PlayerScore> scores = Lists.newArrayList();
        List<Player> players = Lists.newArrayList();
        for (int i = 0; i < 5; i++) {
            Player player = Player.create("p" + i);
            scores.add(PlayerScore.create(player, new SimpleScore(i), i));
            players.add(player);
        }
        TournamentStandings standings = TournamentStandings.create(scores);
        ArrayList<PlayerScore> scoresSorted = Lists.newArrayList(standings.getScores());
        assertEquals(Lists.reverse(scores), scoresSorted);

        Seeding seedingsCutTo6 = Seedings.getSeedingsFromFinalStandings(standings, 6);
        Seeding seedingsCutTo5 = Seedings.getSeedingsFromFinalStandings(standings, 5);
        Seeding seedingsCutTo4 = Seedings.getSeedingsFromFinalStandings(standings, 4);
        Seeding seedingsCutTo3 = Seedings.getSeedingsFromFinalStandings(standings, 3);
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
