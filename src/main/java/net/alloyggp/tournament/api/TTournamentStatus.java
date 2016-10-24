package net.alloyggp.tournament.api;

import java.util.Collection;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.alloyggp.tournament.internal.spec.TournamentSpec;

/**
 * An immutable object representing a particular state of a tournament. This includes
 * the specification of the tournament, the initial seeding of players, and any match
 * results obtained so far.
 */
@Immutable
public class TTournamentStatus {
    private static final List<TAdminAction> NO_ACTIONS = ImmutableList.of();

    private final TTournament spec;
    private final TSeeding initialSeeding;
    private final ImmutableSet<TMatchResult> resultsSoFar;

    private TTournamentStatus(TTournament spec, TSeeding initialSeeding,
            ImmutableSet<TMatchResult> resultsSoFar) {
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
    public static TTournamentStatus getInitialStatus(TTournament spec, TSeeding initialSeeding) {
        return new TTournamentStatus(spec, initialSeeding, ImmutableSet.<TMatchResult>of());
    }

    /**
     * Returns a new TournamentStatus object that includes the given result in addition
     * to the results already known.
     */
    public TTournamentStatus withNewResult(TMatchResult newResult) {
        return withNewResults(ImmutableList.of(newResult));
    }

    /**
     * Returns a new TournamentStatus object that includes the given results in addition
     * to the results already known.
     */
    public TTournamentStatus withNewResults(Collection<TMatchResult> newResults) {
        ImmutableSet<TMatchResult> allMatchResults = ImmutableSet.<TMatchResult>builder()
                .addAll(resultsSoFar)
                .addAll(newResults)
                .build();
        return new TTournamentStatus(spec, initialSeeding, allMatchResults);
    }

    public TTournament getSpec() {
        return spec;
    }

    public ImmutableSet<TMatchResult> getResultsSoFar() {
        return resultsSoFar;
    }

    /**
     * Returns true iff the tournament is over and there are no more matches to
     * run.
     */
    public boolean isComplete() {
        return getNextMatchesToRun().getMatchesToRun().isEmpty();
    }

    /**
     * Returns the next set of matches to run.
     */
    public TNextMatchesResult getNextMatchesToRun() {
        return spec.getMatchesToRun(initialSeeding, resultsSoFar, NO_ACTIONS);
    }

    public TRanking getCurrentStandings() {
        return spec.getCurrentStandings(initialSeeding, resultsSoFar, NO_ACTIONS);
    }

    public List<TRanking> getStandingsHistory() {
        return spec.getStandingsHistory(initialSeeding, resultsSoFar, NO_ACTIONS);
    }

    public TTournamentStatus apply(TAdminAction adminAction) {
        return new TTournamentStatus(((TournamentSpec) spec).apply(adminAction), initialSeeding, resultsSoFar);
    }
}
