package net.alloyggp.tournament.internal.admin;

import java.util.Comparator;
import java.util.Set;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.alloyggp.tournament.api.TAdminAction;
import net.alloyggp.tournament.api.TGame;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.internal.MatchId;
import net.alloyggp.tournament.internal.spec.StageFormat;

@SuppressWarnings("unused")
public abstract class InternalAdminAction implements TAdminAction {
    public StageFormat editStageFormat(int stageNum, StageFormat format) {
        return format;
    }

    public int editStagePlayerLimit(int stageNum, int playerLimit) {
        return playerLimit;
    }

    public Set<TPlayer> editStageExcludedPlayers(int stageNum,
            ImmutableSet<TPlayer> excludedPlayers) {
        return excludedPlayers;
    }

    public Optional<DateTime> editRoundStartTime(Optional<DateTime> startTime,
            int stageNum, int roundNum) {
        return startTime;
    }

    public TGame editMatchGame(TGame game, int stageNum, int roundNum, int matchNum) {
        return game;
    }

    public int editMatchStartClock(int startClock, int stageNum, int roundNum,
            int matchNum) {
        return startClock;
    }

    public int editMatchPlayClock(int playClock, int stageNum, int roundNum,
            int matchNum) {
        return playClock;
    }

    public ImmutableList<Integer> editMatchPlayerSeedOrder(
            ImmutableList<Integer> playerSeedOrder, int stageNum, int roundNum,
            int matchNum) {
        return playerSeedOrder;
    }

    public double editMatchWeight(double weight, int stageNum, int roundNum,
            int matchNum) {
        return weight;
    }

    //Require implementations of these, since they're easy to forget
    @Override
    public abstract boolean equals(Object obj);
    @Override
    public abstract int hashCode();

    /**
     * Returns true iff the match result should be ignored because this action invalidates it.
     */
    //TODO: Put this in some superclass.
    public abstract boolean invalidates(MatchId matchId, Comparator<Integer> roundComparator);
}
