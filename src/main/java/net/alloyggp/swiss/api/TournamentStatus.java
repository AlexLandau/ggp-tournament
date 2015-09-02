package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

/**
 * An immutable object representing a particular state of a tournament. This includes
 * the specification of the tournament, the initial seeding of players, and any match
 * results obtained so far.
 */
@Immutable
public class TournamentStatus {
    private final Tournament spec;
    private final Seeding initialSeeding;
    private final ImmutableList<MatchResult> resultsSoFar;

    private TournamentStatus(Tournament spec, Seeding initialSeeding,
            ImmutableList<MatchResult> resultsSoFar) {
        this.spec = spec;
        this.initialSeeding = initialSeeding;
        this.resultsSoFar = resultsSoFar;
    }

    /**
     * Returns the initial status of the tournament in which no matches have been played.
     *
     * <p>In addition to the tournament specification, this requires an initial seeding of
     * the players that will be participating. This seeding is used as a tie-breaker in
     * the first stage of the tournament. It also affects match assignments. For some
     * tournaments, the client may want to use a pre-existing set of player rankings; in
     * other cases, a random seeding is sufficient.
     */
    public static TournamentStatus getInitialStatus(Tournament spec, Seeding initialSeeding) {
        return new TournamentStatus(spec, initialSeeding, ImmutableList.of());
    }

    /**
     * Returns a new TournamentStatus object that includes the given result in addition
     * to the results already known.
     */
    public TournamentStatus withNewResult(MatchResult newResult) {
        return withNewResults(ImmutableList.of(newResult));
    }

    /**
     * Returns a new TournamentStatus object that includes the given results in addition
     * to the results already known.
     */
    public TournamentStatus withNewResults(List<MatchResult> newResults) {
        ImmutableList<MatchResult> allMatchResults = ImmutableList.<MatchResult>builder()
                .addAll(resultsSoFar)
                .addAll(newResults)
                .build();
        return new TournamentStatus(spec, initialSeeding, allMatchResults);
    }

    public Tournament getSpec() {
        return spec;
    }

    public ImmutableList<MatchResult> getResultsSoFar() {
        return resultsSoFar;
    }

    /**
     * Returns true iff the tournament is over and there are no more matches to
     * run.
     */
    public boolean isComplete() {
        return getNextMatchesToRun().isEmpty();
    }

    /**
     * Returns the next set of matches to run.
     */
    public Set<MatchSetup> getNextMatchesToRun() {
        return spec.getMatchesToRun(initialSeeding, resultsSoFar);
    }

    public TournamentStandings getStandings() {
        return spec.getCurrentStandings(initialSeeding, resultsSoFar);
    }
}
