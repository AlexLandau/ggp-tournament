package net.alloyggp.tournament.internal.admin;

import java.util.Comparator;

import net.alloyggp.escaperope.rope.ropify.ListWeaver;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.escaperope.rope.ropify.RopeList;
import net.alloyggp.escaperope.rope.ropify.Weaver;
import net.alloyggp.tournament.api.TGame;
import net.alloyggp.tournament.internal.MatchId;
import net.alloyggp.tournament.internal.rope.Weavers;

public class ReplaceGameAction extends InternalAdminAction {
    private final int stageNum;
    private final int roundNum;
    private final int matchNum;
    private final TGame newGame;

    private ReplaceGameAction(int stageNum, int roundNum, int matchNum,
            TGame newGame) {
        this.stageNum = stageNum;
        this.roundNum = roundNum;
        this.matchNum = matchNum;
        this.newGame = newGame;
    }

    public static ReplaceGameAction create(int stage, int round, int match,
            TGame newGame) {
        return new ReplaceGameAction(stage, round, match, newGame);
    }

    @Override
    public TGame editMatchGame(TGame game, int stageNum, int roundNum,
            int matchNum) {
        if (this.stageNum == stageNum
                && this.roundNum == roundNum
                && this.matchNum == matchNum) {
            return newGame;
        }
        return game;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + matchNum;
        result = prime * result + ((newGame == null) ? 0 : newGame.hashCode());
        result = prime * result + roundNum;
        result = prime * result + stageNum;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReplaceGameAction other = (ReplaceGameAction) obj;
        if (matchNum != other.matchNum)
            return false;
        if (newGame == null) {
            if (other.newGame != null)
                return false;
        } else if (!newGame.equals(other.newGame))
            return false;
        if (roundNum != other.roundNum)
            return false;
        if (stageNum != other.stageNum)
            return false;
        return true;
    }

    @Override
    public boolean invalidates(MatchId matchId, Comparator<Integer> roundComparator) {
        if (matchId.getStageNumber() < stageNum) {
            return false;
        }
        if (matchId.getStageNumber() > stageNum) {
            return true;
        }
        if (roundComparator.compare(matchId.getRoundNumber(), roundNum) < 0) {
            return false;
        }
        if (roundComparator.compare(matchId.getRoundNumber(), roundNum) > 0) {
            return true;
        }
        if (matchId.getMatchNumber() < matchNum) {
            return false;
        }
        return true;
    }

    public static final Weaver<ReplaceGameAction> WEAVER = new ListWeaver<ReplaceGameAction>() {
        @Override
        protected void addToList(ReplaceGameAction object, RopeBuilder list) {
            list.add(object.stageNum);
            list.add(object.roundNum);
            list.add(object.matchNum);
            list.add(object.newGame, Weavers.GAME);
        }

        @Override
        protected ReplaceGameAction fromRope(RopeList list) {
            int stageNum = list.getInt(0);
            int roundNum = list.getInt(1);
            int matchNum = list.getInt(2);
            TGame game = list.get(3, Weavers.GAME);
            return create(stageNum, roundNum, matchNum, game);
        }
    };

    @Override
    public String toPersistedString() {
        return InternalAdminActions.toPersistedString(this);
    }
}
