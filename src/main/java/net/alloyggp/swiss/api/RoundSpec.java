package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Immutable
public class RoundSpec {
    private final ImmutableList<MatchSpec> matches;

    private RoundSpec(ImmutableList<MatchSpec> matches) {
        this.matches = matches;
    }

    @SuppressWarnings("unchecked")
    public static RoundSpec parseYaml(Object yamlRound, Map<String, Game> games) {
        Map<String, Object> roundMap = (Map<String, Object>) yamlRound;
        List<MatchSpec> matches = Lists.newArrayList();
        for (Object yamlMatch : (List<Object>) roundMap.get("matches")) {
            matches.add(MatchSpec.parseYaml(yamlMatch, games));
        }
        return new RoundSpec(ImmutableList.copyOf(matches));
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
