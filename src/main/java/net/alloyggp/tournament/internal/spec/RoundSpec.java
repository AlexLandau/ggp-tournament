package net.alloyggp.tournament.internal.spec;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.alloyggp.tournament.api.TGame;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.TimeUtils;
import net.alloyggp.tournament.internal.YamlUtils;
import net.alloyggp.tournament.internal.admin.InternalAdminAction;

@Immutable
public class RoundSpec {
    private final Optional<DateTime> startTime;
    private final ImmutableList<MatchSpec> matches;
    private final int roundNum;

    private RoundSpec(Optional<DateTime> startTime,
            ImmutableList<MatchSpec> matches, int roundNum) {
        this.startTime = startTime;
        this.matches = matches;
        this.roundNum = roundNum;
    }

    private static final ImmutableSet<String> ALLOWED_KEYS = ImmutableSet.of(
            "startTime",
            "matches"
            );



    @SuppressWarnings("unchecked")
    public static RoundSpec parseYaml(Object yamlRound, int roundNum, Map<String, Game> games) {
        Map<String, Object> roundMap = (Map<String, Object>) yamlRound;
        YamlUtils.validateKeys(roundMap, "round", ALLOWED_KEYS);

        final Optional<DateTime> startTime;
        if (roundMap.containsKey("startTime")) {
            String timeString = (String) roundMap.get("startTime");
            startTime = Optional.of(DateTime.parse(timeString, TimeUtils.RFC1123_DATE_TIME_FORMATTER));
//            java.util.Date date = (Date) roundMap.get("startTime");
//            startTime = date.
        } else {
            startTime = Optional.absent();
        }

        List<MatchSpec> matches = Lists.newArrayList();
        int matchNum = 0;
        for (Object yamlMatch : (List<Object>) roundMap.get("matches")) {
            matches.add(MatchSpec.parseYaml(yamlMatch, matchNum, games));
            matchNum++;
        }
        return new RoundSpec(startTime, ImmutableList.copyOf(matches), roundNum);
    }

    public Optional<DateTime> getStartTime() {
        return startTime;
    }

    public ImmutableList<MatchSpec> getMatches() {
        return matches;
    }

    public static Set<TGame> getAllGames(ImmutableList<RoundSpec> rounds) {
        Set<TGame> results = Sets.newHashSet();
        for (RoundSpec round : rounds) {
            for (MatchSpec match : round.getMatches()) {
                results.add(match.getGame());
            }
        }
        return results;
    }

    public RoundSpec apply(InternalAdminAction action, int stageNum) {
        Optional<DateTime> newStartTime = action.editRoundStartTime(startTime, stageNum, roundNum);
        List<MatchSpec> newMatches = Lists.newArrayList();
        for (MatchSpec match : matches) {
            newMatches.add(match.apply(action, stageNum, roundNum));
        }

        return new RoundSpec(newStartTime, ImmutableList.copyOf(newMatches), roundNum);
    }
}
