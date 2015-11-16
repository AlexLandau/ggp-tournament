package net.alloyggp.tournament.spec;

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

import net.alloyggp.tournament.api.Game;
import net.alloyggp.tournament.impl.TimeUtils;
import net.alloyggp.tournament.impl.YamlUtils;

@Immutable
public class RoundSpec {
    private final Optional<DateTime> startTime;
    private final ImmutableList<MatchSpec> matches;

    private RoundSpec(Optional<DateTime> startTime, ImmutableList<MatchSpec> matches) {
        this.startTime = startTime;
        this.matches = matches;
    }

    private static final ImmutableSet<String> ALLOWED_KEYS = ImmutableSet.of(
            "startTime",
            "matches"
            );



    @SuppressWarnings("unchecked")
    public static RoundSpec parseYaml(Object yamlRound, Map<String, Game> games) {
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
        for (Object yamlMatch : (List<Object>) roundMap.get("matches")) {
            matches.add(MatchSpec.parseYaml(yamlMatch, games));
        }
        return new RoundSpec(startTime, ImmutableList.copyOf(matches));
    }

    public Optional<DateTime> getStartTime() {
        return startTime;
    }

    public ImmutableList<MatchSpec> getMatches() {
        return matches;
    }

    public static Set<Game> getAllGames(ImmutableList<RoundSpec> rounds) {
        Set<Game> results = Sets.newHashSet();
        for (RoundSpec round : rounds) {
            for (MatchSpec match : round.getMatches()) {
                results.add(match.getGame());
            }
        }
        return results;
    }
}
