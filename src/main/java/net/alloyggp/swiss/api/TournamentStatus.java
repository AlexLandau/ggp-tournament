package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

@Immutable
public class TournamentStatus {
	private final TournamentSpec spec;
	private final Seeding initialSeeding;
	private final ImmutableList<MatchResult> resultsSoFar;

	private TournamentStatus(TournamentSpec spec, Seeding initialSeeding, ImmutableList<MatchResult> resultsSoFar) {
		this.spec = spec;
		this.initialSeeding = initialSeeding;
		this.resultsSoFar = resultsSoFar;
	}

	public static TournamentStatus getInitialStatus(TournamentSpec spec, Seeding initialSeeding) {
		return new TournamentStatus(spec, initialSeeding, ImmutableList.of());
	}

	public TournamentStatus withNewResult(MatchResult newResult) {
		return withNewResults(ImmutableList.of(newResult));
	}

	public TournamentStatus withNewResults(List<MatchResult> newResults) {
		ImmutableList<MatchResult> allMatchResults = ImmutableList.<MatchResult>builder()
				.addAll(resultsSoFar)
				.addAll(newResults)
				.build();
		return new TournamentStatus(spec, initialSeeding, allMatchResults);
	}

	public TournamentSpec getSpec() {
		return spec;
	}

	public ImmutableList<MatchResult> getResultsSoFar() {
		return resultsSoFar;
	}

	public boolean isComplete() {
		return getNextMatchesToRun().isEmpty();
	}

	public Set<MatchSetup> getNextMatchesToRun() {
		return spec.getMatchesToRun(initialSeeding, resultsSoFar);
	}
}
