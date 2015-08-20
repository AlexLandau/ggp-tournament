package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Immutable
public class RoundSpec {
	//How many games to win? ...
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
}
