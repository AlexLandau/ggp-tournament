package net.alloyggp.tournament.internal.quasirandom;

import java.util.List;

import net.alloyggp.tournament.api.TPlayer;

public interface QuasiRandomMatchGenerator {
    List<List<List<TPlayer>>> generateMatchups(List<TPlayer> players, int playersPerMatch, int numMatches);
}
