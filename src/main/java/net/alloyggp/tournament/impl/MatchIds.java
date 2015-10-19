package net.alloyggp.tournament.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

public class MatchIds {

    /**
     * Creates a match ID string that encodes the given information. This should be used
     * to generate all match IDs for matches used in tournaments; many format runners rely
     * on the information encoded this way.
     */
    public static String create(String tournamentInternalName,
            int stage,
            int round,
            int playerMatching,
            int match,
            int attempt) {
        Preconditions.checkArgument(stage >= 0);
        Preconditions.checkArgument(round >= 0);
        Preconditions.checkArgument(playerMatching >= 0);
        Preconditions.checkArgument(match >= 0);
        Preconditions.checkArgument(attempt >= 0);
        return tournamentInternalName + "-" + stage + "-" + round + "-" + playerMatching + "-" + match + "-" + attempt;
    }

    private static final Pattern MATCH_ID_PATTERN = Pattern.compile("^(.*)-(\\d+)-(\\d+)-(\\d+)-(\\d+)-(\\d+)$");

    public static String getTournamentInternalName(String matchId) {
        return getNthGroup(matchId, 1);
    }

    public static int parseStageNumber(String matchId) {
        return parseNthGroupAsNumber(matchId, 2);
    }

    public static int parseRoundNumber(String matchId) {
        return parseNthGroupAsNumber(matchId, 3);
    }

    public static int parsePlayerMatchingNumber(String matchId) {
        return parseNthGroupAsNumber(matchId, 4);
    }

    public static int parseMatchNumber(String matchId) {
        return parseNthGroupAsNumber(matchId, 5);
    }

    public static int parseAttemptNumber(String matchId) {
        return parseNthGroupAsNumber(matchId, 6);
    }

    private static int parseNthGroupAsNumber(String matchId, int groupNumber) {
        return Integer.parseInt(getNthGroup(matchId, groupNumber));
    }

    private static String getNthGroup(String matchId, int groupNumber) {
        Matcher matcher = MATCH_ID_PATTERN.matcher(matchId);
        boolean matches = matcher.matches();
        if (!matches) {
            throw new IllegalArgumentException("Could not parse " + matchId
                    + " as a tournament match ID");
        }
        return matcher.group(groupNumber);
    }

}
