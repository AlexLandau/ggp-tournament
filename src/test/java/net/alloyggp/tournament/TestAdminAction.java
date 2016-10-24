package net.alloyggp.tournament;

import java.util.Comparator;

import net.alloyggp.tournament.internal.MatchId;
import net.alloyggp.tournament.internal.admin.InternalAdminAction;

public class TestAdminAction extends InternalAdminAction {
    private final int stageNum;
    private final int roundNum;

    private TestAdminAction(int stageNum, int roundNum) {
        this.stageNum = stageNum;
        this.roundNum = roundNum;
    }

    public static TestAdminAction create(int stageNum, int roundNum) {
        return new TestAdminAction(stageNum, roundNum);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        TestAdminAction other = (TestAdminAction) obj;
        if (roundNum != other.roundNum)
            return false;
        if (stageNum != other.stageNum)
            return false;
        return true;
    }

    //TODO: Put this in some superclass.
    @Override
    public boolean invalidates(MatchId matchId, Comparator<Integer> roundComparator) {
        if (matchId.getStageNumber() < stageNum) {
            return false;
        }
        if (matchId.getStageNumber() > stageNum) {
            return true;
        }
        return roundComparator.compare(matchId.getRoundNumber(), roundNum) >= 0;
    }

    @Override
    public String toPersistedString() {
        throw new UnsupportedOperationException();
    }
}
