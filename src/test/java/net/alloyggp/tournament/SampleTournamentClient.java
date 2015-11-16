package net.alloyggp.tournament;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.api.Tournament;
import net.alloyggp.tournament.api.TournamentStatus;

/*
 * As much as anything else, this is to help me get a better understanding of what
 * the API should look like from a client's perspective...
 */
public class SampleTournamentClient {
    @Test
    public void testSingleElimination() {
        Tournament spec = TestSpecs.load("swiss1test7");
        Seeding initialSeeding = toSeeding("1", "2", "3", "4", "5", "6", "7");
        TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
        //Run matches until exhaustion...
        while (!status.isComplete()) {
            Set<MatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
            List<MatchResult> results = getRandomOutcomes(nextMatches);
            System.out.println("Match results: " + results);
            status = status.withNewResults(results);
            System.out.println("Standings are: " + status.getCurrentStandings());
        }
        Ranking standings = status.getCurrentStandings();
        System.out.println("Standings are: " + standings);
    }

    private Seeding toSeeding(String... playerNames) {
        List<Player> playersBestFirst = Lists.newArrayList();
        for (String playerName : playerNames) {
            playersBestFirst.add(Player.create(playerName));
        }
        return Seeding.create(playersBestFirst);
    }

    private List<MatchResult> getRandomOutcomes(Set<MatchSetup> nextMatches) {
        Random random = new Random();
        List<MatchResult> outcomes = Lists.newArrayList();
        for (MatchSetup setup : nextMatches) {
            outcomes.add(FuzzTests.getResult(random, setup));
        }
        return outcomes;
    }

}
