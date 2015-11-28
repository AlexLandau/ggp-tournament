package net.alloyggp.tournament.quasirandom;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.internal.quasirandom.QuasiRandomMatchGenerator;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

public class FirstTryImpl implements QuasiRandomMatchGenerator {

    @Override
    public List<List<List<TPlayer>>> generateMatchups(List<TPlayer> players,
            int numRoles, int numMatches) {
        List<List<List<TPlayer>>> results = Lists.newArrayList();
        Multiset<Set<TPlayer>> matchupsSoFar = HashMultiset.create();
        for (int round = 0; round < numMatches; round++) {
            results.add(getNonFixedSumPlayerGroups(players, numRoles, matchupsSoFar));
        }
        return results;
    }

    private List<List<TPlayer>> getNonFixedSumPlayerGroups(final List<TPlayer> players,
            int numRoles, final Multiset<Set<TPlayer>> matchupsSoFar) {

        //Two-player: there's an elegant round-robin algorithm we
        //could use here, "rotate" all but one player in two rows

        //Do something naive for now
//        final Multiset<Set<TPlayer>> matchupsSoFar = nonFixedSumMatchupsSoFarByNumPlayers.get(numRoles);
        Preconditions.checkNotNull(matchupsSoFar);
//        Map<Player, Multiset<Integer>> roleAssignmentsSoFar = nonFixedSumRoleAssignmentsSoFarByNumPlayers.get(numRoles);
        List<TPlayer> playersToAssign = Lists.newArrayList(players);
        List<List<TPlayer>> results = Lists.newArrayList();
        while (playersToAssign.size() >= numRoles) {
            TPlayer player = playersToAssign.get(0);

            //Grab the first available players with the fewest previous matchups against us and each other
            final List<TPlayer> playersInGroup = Lists.newArrayList(player);
            playersToAssign.remove(player);
            while (playersInGroup.size() < numRoles) {
                Ordering<TPlayer> playerOrder = Ordering.from(new Comparator<TPlayer>() {
                            @Override
                            public int compare(TPlayer p1, TPlayer p2) {
                                int sum1 = getSum(p1);
                                int sum2 = getSum(p2);
                                return Integer.compare(sum1, sum2);
                            }

                            //Sum of matchups against players already in group
                            private int getSum(TPlayer p) {
                                int sum = 0;
                                for (TPlayer playerInGroup : playersInGroup) {
                                    sum += matchupsSoFar.count(ImmutableSet.of(p, playerInGroup));
                                }
                                return sum;
                            }
                        })
                        .compound(new Comparator<TPlayer>() {
                            @Override
                            public int compare(TPlayer o1, TPlayer o2) {
                                int seed1 = players.indexOf(o1);
                                int seed2 = players.indexOf(o2);
                                return Integer.compare(seed1, seed2);
                            }
                        });
                TPlayer playerToAdd = playerOrder.min(playersToAssign);
                playersInGroup.add(playerToAdd);
                playersToAssign.remove(playerToAdd);
            }
            //TODO: Shuffle the roles intelligently, somehow
            //Should role shuffling be per-game? Across the tournament?

            results.add(playersInGroup);
        }

        for (List<TPlayer> playerGroup : results) {
                //TODO: Fix this
                for (int p1 = 0; p1 < playerGroup.size(); p1++) {
                    for (int p2 = p1 + 1; p2 < playerGroup.size(); p2++) {
                        matchupsSoFar
                            .add(ImmutableSet.of(playerGroup.get(p1), playerGroup.get(p2)));
                    }
                }
            }

        return results;
    }


}
