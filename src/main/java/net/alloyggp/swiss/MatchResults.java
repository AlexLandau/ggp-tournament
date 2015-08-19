package net.alloyggp.swiss;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import net.alloyggp.swiss.api.MatchResult;

public class MatchResults {
	private MatchResults() {
		//Not instantiable
	}

	public static Set<MatchResult> filterByStage(Collection<MatchResult> inputs, int stageNum) {
		return inputs.stream()
			.filter(result -> {
				String matchId = result.getSetup().getMatchId();
				int matchStage = MatchIds.parseStageNumber(matchId);
				return matchStage == stageNum;
			})
			.collect(Collectors.toSet());
	}

	public static SetMultimap<Integer, MatchResult> mapByRound(List<MatchResult> resultsSoFar, int stageNum) {
		SetMultimap<Integer, MatchResult> mapped = HashMultimap.create();
		for (MatchResult result : filterByStage(resultsSoFar, stageNum)) {
			String matchId = result.getSetup().getMatchId();
			int matchRound = MatchIds.parseRoundNumber(matchId);
			mapped.put(matchRound, result);
		}
		return mapped;
	}
}
