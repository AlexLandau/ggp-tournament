package net.alloyggp.swiss.api;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

@Immutable
public class TournamentStatus {
	private final TournamentSpec spec;
	private final TournamentStandings initialSeeding;
	private final ImmutableList<MatchResult> resultsSoFar;

	private TournamentStatus(TournamentSpec spec, ImmutableList<MatchResult> resultsSoFar) {
		this.spec = spec;
		this.resultsSoFar = resultsSoFar;
	}

	public static TournamentStatus getInitialStatus(TournamentSpec spec) {
		return new TournamentStatus(spec, ImmutableList.of());
	}

	public TournamentStatus withResult(MatchResult result) {
		ImmutableList<MatchResult> newMatchResults = ImmutableList.<MatchResult>builder()
				.addAll(resultsSoFar)
				.add(result)
				.build();
		return new TournamentStatus(spec, newMatchResults);
	}

	public TournamentSpec getSpec() {
		return spec;
	}

	public ImmutableList<MatchResult> getResultsSoFar() {
		return resultsSoFar;
	}

	public boolean isComplete() {
		// TODO Implement
	}

	public TournamentStatus withNewResults(List<MatchResult> results) {
		// TODO Implement
	}

	public List<MatchSetup> getNextMatchesToRun() {
		for (StageSpec stage : spec.getStages()) {
			//Get matches for this stage

		}
	}
}
