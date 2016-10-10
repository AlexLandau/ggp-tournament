package net.alloyggp.tournament.api;

import net.alloyggp.tournament.internal.admin.ReplaceGameAction;

public class TAdminActions {
    public static TAdminAction replaceGame(int stage, int round, int match, TGame newGame) {
        return ReplaceGameAction.create(stage, round, match, newGame);
    }
}
