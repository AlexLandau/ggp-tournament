package net.alloyggp.tournament;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournament;
import net.alloyggp.tournament.api.TTournamentStatus;

/*
 * As much as anything else, this is to help me get a better understanding of what
 * the API should look like from a client's perspective...
 */
public class SampleTournamentClient {
    @Test
    public void testSingleElimination() {
        TTournament spec = TestSpecs.load("swiss1test7");
        TSeeding initialSeeding = toSeeding("1", "2", "3", "4", "5", "6", "7");
        TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
        //Run matches until exhaustion...
        while (!status.isComplete()) {
            Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
            List<TMatchResult> results = getRandomOutcomes(nextMatches);
            System.out.println("Match results: " + results);
            status = status.withNewResults(results);
            System.out.println("Standings are: " + status.getCurrentStandings());
        }
        TRanking standings = status.getCurrentStandings();
        System.out.println("Standings are: " + standings);
    }

    private TSeeding toSeeding(String... playerNames) {
        List<TPlayer> playersBestFirst = Lists.newArrayList();
        for (String playerName : playerNames) {
            playersBestFirst.add(TPlayer.create(playerName));
        }
        return TSeeding.create(playersBestFirst);
    }

    private List<TMatchResult> getRandomOutcomes(Set<TMatchSetup> nextMatches) {
        Random random = new Random();
        List<TMatchResult> outcomes = Lists.newArrayList();
        for (TMatchSetup setup : nextMatches) {
            outcomes.add(FuzzTests.getResult(random, setup));
        }
        return outcomes;
    }

}
