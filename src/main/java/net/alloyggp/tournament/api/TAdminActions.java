package net.alloyggp.tournament.api;

import net.alloyggp.tournament.internal.admin.InternalAdminActions;
import net.alloyggp.tournament.internal.admin.ReplaceGameAction;

public class TAdminActions {
    private TAdminActions() {
        // Not instantiable
    }

    public static TAdminAction replaceGame(int stage, int round, int match, TGame newGame) {
        return ReplaceGameAction.create(stage, round, match, newGame);
    }

    public static TAdminAction fromPersistedString(String persistedString) {
        return InternalAdminActions.fromPersistedString(persistedString);
    }
}
