package net.alloyggp.tournament.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

/**
 * An internal identifier of a match within the tournament structure.
 */
public class MatchId {
    private final int stage;
    private final int round;
    private final int playerMatching;
    private final int match;
    private final int attempt;

    private MatchId(int stage, int round, int playerMatching, int match,
            int attempt) {
        Preconditions.checkArgument(stage >= 0);
        Preconditions.checkArgument(round >= 0);
        Preconditions.checkArgument(playerMatching >= 0);
        Preconditions.checkArgument(match >= 0);
        Preconditions.checkArgument(attempt >= 0);
        this.stage = stage;
        this.round = round;
        this.playerMatching = playerMatching;
        this.match = match;
        this.attempt = attempt;
    }

    public static MatchId create(int stage, int round, int playerMatching,
            int match, int attempt) {
        return new MatchId(stage, round, playerMatching, match, attempt);
    }

    private static final Pattern MATCH_ID_PATTERN = Pattern.compile("^ggpt-(\\d+)-(\\d+)-(\\d+)-(\\d+)-(\\d+)$");
    public static MatchId create(String matchIdString) {
        Matcher matcher = MATCH_ID_PATTERN.matcher(matchIdString);
        boolean matches = matcher.matches();
        if (!matches) {
            throw new IllegalArgumentException("Could not parse " + matchIdString
                    + " as an intra-tournament match ID");
        }
        int stage = parseNthGroupAsNumber(matcher, 1);
        int round = parseNthGroupAsNumber(matcher, 2);
        int playerMatching = parseNthGroupAsNumber(matcher, 3);
        int match = parseNthGroupAsNumber(matcher, 4);
        int attempt = parseNthGroupAsNumber(matcher, 5);
        return new MatchId(stage, round, playerMatching, match, attempt);
    }

    private static int parseNthGroupAsNumber(Matcher matcher, int i) {
        return Integer.parseInt(matcher.group(i));
    }

    public int getStageNumber() {
        return stage;
    }

    public int getRoundNumber() {
        return round;
    }

    public int getPlayerMatchingNumber() {
        return playerMatching;
    }

    public int getMatchNumber() {
        return match;
    }

    public int getAttemptNumber() {
        return attempt;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + attempt;
        result = prime * result + match;
        result = prime * result + playerMatching;
        result = prime * result + round;
        result = prime * result + stage;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MatchId other = (MatchId) obj;
        if (attempt != other.attempt)
            return false;
        if (match != other.match)
            return false;
        if (playerMatching != other.playerMatching)
            return false;
        if (round != other.round)
            return false;
        if (stage != other.stage)
            return false;
        return true;
    }

    //TODO: Memoize this?
    @Override
    public String toString() {
        return "ggpt-" + stage + "-" + round + "-" + playerMatching + "-" + match + "-" + attempt;
    }

}
