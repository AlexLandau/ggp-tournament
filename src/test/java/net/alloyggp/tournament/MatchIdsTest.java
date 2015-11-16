package net.alloyggp.tournament;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.alloyggp.tournament.internal.MatchIds;

public class MatchIdsTest {
    @Test
    public void testNormalParsing() {
        testParsing(0, 1, 2, 3, 4);
    }

    @Test
    public void testLongerNumbersParsing() {
        testParsing(1111, 2222, 3333, 4444, 1234);
    }

    @Test
    public void testHyphenInTournamentName() {
        testParsing(5, 4, 3, 2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeNumbersRejected() {
        MatchIds.create(0, -1, 0, 1, 0);
    }

    private void testParsing(int stage,
            int round, int playerMatching, int match, int attempt) {
        String matchId = MatchIds.create(stage,
                round, playerMatching, match, attempt);
        assertEquals(stage, MatchIds.parseStageNumber(matchId));
        assertEquals(round, MatchIds.parseRoundNumber(matchId));
        assertEquals(playerMatching, MatchIds.parsePlayerMatchingNumber(matchId));
        assertEquals(match, MatchIds.parseMatchNumber(matchId));
        assertEquals(attempt, MatchIds.parseAttemptNumber(matchId));
    }
}
