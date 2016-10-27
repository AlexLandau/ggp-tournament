package net.alloyggp.tournament.internal.admin;

import net.alloyggp.escaperope.Delimiters;
import net.alloyggp.escaperope.RopeDelimiter;
import net.alloyggp.escaperope.rope.Rope;
import net.alloyggp.escaperope.rope.ropify.SubclassWeaver;
import net.alloyggp.escaperope.rope.ropify.Weaver;
import net.alloyggp.tournament.api.TAdminAction;

public class InternalAdminActions {
    private InternalAdminActions() {
        //Not instantiable
    }

    @SuppressWarnings("deprecation")
    public static final Weaver<TAdminAction> WEAVER = SubclassWeaver.builder(TAdminAction.class)
            .add(ReplaceGameAction.class, "ReplaceGame", ReplaceGameAction.WEAVER)
            .build();

    public static RopeDelimiter getStandardDelimiter() {
        return Delimiters.getJsonArrayRopeDelimiter();
    }

    public static TAdminAction fromPersistedString(String persistedString) {
        Rope rope = getStandardDelimiter().undelimit(persistedString);
        return WEAVER.fromRope(rope);
    }

    public static String toPersistedString(TAdminAction adminAction) {
        Rope rope = WEAVER.toRope(adminAction);
        return getStandardDelimiter().delimit(rope);
    }
}
