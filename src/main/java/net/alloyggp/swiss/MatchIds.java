package net.alloyggp.swiss;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchIds {

	public static String create(String tournamentInternalName,
			int stage,
			int round,
			int match,
			int attempt) {
		return tournamentInternalName + "-" + stage + "-" + round + "-" + match + "-" + attempt;
	}

	private static final Pattern MATCH_ID_PATTERN = Pattern.compile("^(.*)-(\\d+)-(\\d+)-(\\d+)-(\\d+)$");

	public static String getTournamentInternalName(String matchId) {
		return getNthGroup(matchId, 1);
	}

	public static int parseStageNumber(String matchId) {
		return parseNthGroupAsNumber(matchId, 2);
	}

	public static int parseRoundNumber(String matchId) {
		return parseNthGroupAsNumber(matchId, 3);
	}

	public static int parseMatchNumber(String matchId) {
		return parseNthGroupAsNumber(matchId, 4);
	}

	public static int parseAttemptNumber(String matchId) {
		return parseNthGroupAsNumber(matchId, 5);
	}

	private static int parseNthGroupAsNumber(String matchId, int groupNumber) {
		return Integer.parseInt(getNthGroup(matchId, groupNumber));
	}

	public static String getNthGroup(String matchId, int groupNumber) {
		Matcher matcher = MATCH_ID_PATTERN.matcher(matchId);
		boolean matches = matcher.matches();
		if (!matches) {
			throw new IllegalArgumentException("Could not parse " + matchId + " as a tournament match ID");
		}
		return matcher.group(groupNumber);
	}

}
