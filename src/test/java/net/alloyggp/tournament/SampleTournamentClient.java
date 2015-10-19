package net.alloyggp.tournament;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import net.alloyggp.tournament.api.MatchResult;
import net.alloyggp.tournament.api.MatchSetup;
import net.alloyggp.tournament.api.Player;
import net.alloyggp.tournament.api.Ranking;
import net.alloyggp.tournament.api.Seeding;
import net.alloyggp.tournament.api.Tournament;
import net.alloyggp.tournament.api.TournamentSpecParser;
import net.alloyggp.tournament.api.TournamentStatus;

/*
 * As much as anything else, this is to help me get a better understanding of what
 * the API should look like from a client's perspective...
 */
public class SampleTournamentClient {
    @Test
    public void testSingleElimination() {
        Tournament spec = TournamentSpecParser.parseYamlFile(new File("testSpecs/swiss1test7.yaml"));
        Seeding initialSeeding = toSeeding("1", "2", "3", "4", "5", "6", "7");
        TournamentStatus status = TournamentStatus.getInitialStatus(spec, initialSeeding);
        //Run matches until exhaustion...
        while (!status.isComplete()) {
            Set<MatchSetup> nextMatches = status.getNextMatchesToRun();
            List<MatchResult> results = getRandomOutcomes(nextMatches);
            System.out.println("Match results: " + results);
            status = status.withNewResults(results);
            System.out.println("Standings are: " + status.getCurrentStandings());
        }
        Ranking standings = status.getCurrentStandings();
        System.out.println("Standings are: " + standings);
    }

    private Seeding toSeeding(String... playerNames) {
        List<Player> playersBestFirst = Arrays.stream(playerNames)
                .map(Player::create)
                .collect(Collectors.toList());
        return Seeding.create(playersBestFirst);
    }

    private List<MatchResult> getRandomOutcomes(Set<MatchSetup> nextMatches) {
        Random random = new Random();
        return nextMatches.stream()
        .map(setup -> FuzzTests.getResult(random, setup))
        .collect(Collectors.toList());
    }

}
