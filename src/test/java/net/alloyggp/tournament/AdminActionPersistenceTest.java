package net.alloyggp.tournament;

import org.junit.Assert;
import org.junit.Test;

import net.alloyggp.tournament.api.TAdminAction;
import net.alloyggp.tournament.api.TAdminActions;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.admin.ReplaceGameAction;

public class AdminActionPersistenceTest {

    @Test
    public void testReplaceGameAction() {
        Game game1 = Game.create("game1", "http://games.ggp.org/something/or/other", 2, true);
        Game game2 = Game.create("simpleGameId2", "http://games.ggp.org/something/or/other.foo", 4, false);
        test(ReplaceGameAction.create(0, 0, 0, game1));
        test(ReplaceGameAction.create(0, 3, 5, game1));
        test(ReplaceGameAction.create(1, 2, 3, game2));
    }

    private void test(TAdminAction action) {
        Assert.assertEquals(action, TAdminActions.fromPersistedString(action.toPersistedString()));
    }
}
