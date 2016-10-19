package net.alloyggp.tournament.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

import net.alloyggp.tournament.internal.admin.InternalAdminAction;
import net.alloyggp.tournament.internal.spec.StageFormat;

public class MatchIds {

    /**
     * Creates a match ID string that encodes the given information. This should be used
     * to generate a substring of all match IDs for matches used in tournaments; many format runners rely
     * on the information encoded this way.
     */
    public static String create(ImmutableList<InternalAdminAction> adminActions, StageFormat format, int stage, int round,
            int playerMatching, int match, int attempt) {
        //Compute number of admin actions that should be applied
        //TODO: Explain why we do this
        MatchId currentMatchId = MatchId.create(0, stage, round, playerMatching, match, attempt);
        for (int i = 0; i < adminActions.size(); i++) {
            if (adminActions.get(i).invalidates(currentMatchId, format.getRoundComparator())) {
                currentMatchId = MatchId.create(i + 1, stage, round, playerMatching, match, attempt);
            }
        }
        return currentMatchId.toString();
    }

    private static final Pattern MATCH_ID_PATTERN = Pattern.compile("^ggpt-(\\d+)-(\\d+)-(\\d+)-(\\d+)-(\\d+)$");
    private static final Pattern MATCH_ID_WITH_ACTIONS_PATTERN = Pattern.compile("^ggpta-(\\d+)-(\\d+)-(\\d+)-(\\d+)-(\\d+)-(\\d+)$");

    public static int parseNumActionsApplied(String matchId) {
        return parseNthGroupAsNumber(matchId, 1);
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
        { // Original format
            Matcher matcher = MATCH_ID_PATTERN.matcher(matchId);
            boolean matches = matcher.matches();
            if (matches) {
                //Old-style ID, shift the group number
                return matcher.group(groupNumber - 1);
            }
        }

        { // Format with admin actions
            Matcher matcher = MATCH_ID_WITH_ACTIONS_PATTERN.matcher(matchId);
            boolean matches = matcher.matches();
            if (matches) {
                return matcher.group(groupNumber);
            }
        }

        throw new IllegalArgumentException("Could not parse " + matchId
                + " as a tournament match ID");
    }
}
