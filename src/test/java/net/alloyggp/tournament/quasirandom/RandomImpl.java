package net.alloyggp.tournament.quasirandom;

import java.util.List;
import java.util.Random;

import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.internal.quasirandom.QuasiRandomMatchGenerator;

import com.google.common.collect.Lists;

public class RandomImpl implements QuasiRandomMatchGenerator {

    @Override
    public List<List<List<TPlayer>>> generateMatchups(List<TPlayer> players,
            int gameSize, int numMatches) {
        //Pure "random" impl with a fixed seed
        List<List<List<TPlayer>>> result = Lists.newArrayList();
        for (int round = 0; round < numMatches; round++) {
            result.add(newRound(players, gameSize, round));
        }
        return result;
    }

    private List<List<TPlayer>> newRound(List<TPlayer> players, int gameSize, int seed) {
        Random random = new Random(seed);
        List<TPlayer> playersToPick = Lists.newArrayList(players);
        List<List<TPlayer>> results = Lists.newArrayList();
        while (playersToPick.size() >= gameSize) {
            List<TPlayer> curMatch = Lists.newArrayList();
            for (int r = 0; r < gameSize; r++) {
                int chosenIndex = random.nextInt(playersToPick.size());
                curMatch.add(playersToPick.get(chosenIndex));
                playersToPick.remove(chosenIndex);
            }
            results.add(curMatch);
        }
        return results;
    }

}
