package net.alloyggp.tournament;

import java.util.Comparator;

import net.alloyggp.tournament.api.TMatchSetup;

public class MatchSetups {
    private MatchSetups() {
        //Not instantiable
    }

    public static final Comparator<TMatchSetup> COMPARATOR = new Comparator<TMatchSetup>() {
        @Override
        public int compare(TMatchSetup o1, TMatchSetup o2) {
            return o1.getMatchId().compareTo(o2.getMatchId());
        }
    };
}
