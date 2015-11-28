package net.alloyggp.tournament.quasirandom;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.internal.quasirandom.QuasiRandomMatchGenerator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RolesFirstImpl2p1 implements QuasiRandomMatchGenerator {

    @Override
    public List<List<List<TPlayer>>> generateMatchups(List<TPlayer> players,
            int playersPerMatch, int numMatches) {
        Preconditions.checkArgument(players.size() >= playersPerMatch);
        //Assume players with highest priority for byes are at the end
        List<List<TPlayer>> firstRoundWithBye = getFirstRound(players, playersPerMatch);
        Map<TPlayer, List<Integer>> roleAssignmentsByRound = getRoleAssignmentsByRound(firstRoundWithBye, playersPerMatch);

        //Now turn this into assignments, somehow...
        //In each round, group the players into their assigned roles
        List<List<List<TPlayer>>> rounds = Lists.newArrayList();
        int offset = 0;
        for (int roundNum = 0; roundNum < numMatches; roundNum++) {
            int roleAssnIndex = roundNum % playersPerMatch;
            ListMultimap<Integer, TPlayer> playersByRole = ArrayListMultimap.create();
            for (TPlayer player : players) {
                int roleAssn = roleAssignmentsByRound.get(player).get(roleAssnIndex);
                playersByRole.put(roleAssn, player);
            }

            //Pick one player by role
            //TODO: Spread these out intelligently
            int matchesPerRound = players.size() / playersPerMatch;
            List<List<TPlayer>> round = Lists.newArrayList();
            for (int matchNum = 0; matchNum < matchesPerRound; matchNum++) {
                List<TPlayer> match = Lists.newArrayList();
                //TODO: Better than this...
                for (int role = 0; role < playersPerMatch; role++) {
                    List<TPlayer> playersForRole = playersByRole.get(role);
                    int chosenIndex = (roundNum+matchNum+(offset*role)) % playersForRole.size();
                    TPlayer player = playersForRole.get(chosenIndex);
                    match.add(player);
//                    playersByRole.get(role).remove(player);
                }
                round.add(match);
            }
            rounds.add(round);
            offset++;
        }
        return rounds;
    }

    private Map<TPlayer, List<Integer>> getRoleAssignmentsByRound(
            List<List<TPlayer>> firstRoundWithBye, int playersPerMatch) {
        Map<TPlayer, List<Integer>> results = Maps.newHashMap();
        List<List<Integer>> permutationsForPlayersPerMatch = getPermutationsFor(playersPerMatch);
        //Go through...
        for (int i = 0; i < firstRoundWithBye.size(); i++) {
            List<TPlayer> match = firstRoundWithBye.get(i);
            int permIndex = i % permutationsForPlayersPerMatch.size();
            List<Integer> permutationForMatch = permutationsForPlayersPerMatch.get(permIndex);
            //TODO: Account for bye case
            for (int playerIdx = 0; playerIdx < match.size(); playerIdx++) {
                //TODO: Consider filling out to case where # matches exceeds # players per match
                //i.e. have additional permutations used in those cases
                results.put(match.get(playerIdx), permuteToPutNFirst(permutationForMatch, playerIdx));
            }
        }
        return results;
    }

    private List<Integer> permuteToPutNFirst(List<Integer> permutation,
            int n) {
        List<Integer> result = Lists.newArrayList(permutation);
        int index = result.indexOf(n);
        Preconditions.checkState(index != -1);
        Collections.rotate(result, -index);
        Preconditions.checkState(result.get(0) == n);
        return result;
    }

    private List<List<Integer>> getPermutationsFor(int playersPerMatch) {
        //TODO: Generalize
        List<List<Integer>> permutations = Lists.newArrayList();
        addPermutationsWithPrefix(permutations, ImmutableList.of(0), playersPerMatch);
        if (playersPerMatch == 4) {
            System.out.println(permutations);
            Preconditions.checkState(permutations.equals(ImmutableList.<List<Integer>>of(
                    ImmutableList.of(0, 1, 2, 3),
                    ImmutableList.of(0, 2, 3, 1),
                    ImmutableList.of(0, 3, 1, 2),
                    ImmutableList.of(0, 1, 3, 2),
                    ImmutableList.of(0, 2, 1, 3),
                    ImmutableList.of(0, 3, 2, 1))));
        }
        return permutations;
//        throw new IllegalArgumentException("implement this...");
    }

    private void addPermutationsWithPrefix(List<List<Integer>> permutations,
            List<Integer> prefix, int numRoles) {
        if (prefix.size() == numRoles) {
            permutations.add(prefix);
            return;
        }
        int lastValue = prefix.get(prefix.size() - 1);
        int curValue = (lastValue + 1) % numRoles;
        List<List<List<Integer>>> sublists = Lists.newArrayList();
        while (curValue != lastValue) {
            if (!prefix.contains(curValue)) {
                List<Integer> newPrefix = Lists.newArrayList(prefix);
                newPrefix.add(curValue);
                List<List<Integer>> sublist = Lists.newArrayList();
                addPermutationsWithPrefix(sublist, newPrefix, numRoles);
                sublists.add(sublist);
            }
            curValue = (curValue + 1) % numRoles;
        }
        permutations.addAll(interleave(sublists));
    }

    private static <T> List<T> interleave(
            List<List<T>> sublists) {
        List<T> result = Lists.newArrayList();
        for (int i = 1; i < sublists.size(); i++) {
            Preconditions.checkArgument(sublists.get(0).size() == sublists.get(i).size());
        }
        for (int i = 0; i < sublists.get(0).size(); i++) {
            for (List<T> sublist : sublists) {
                result.add(sublist.get(i));
            }
        }
        return result;
    }

    //TODO: Deal with bye players
    private List<List<TPlayer>> getFirstRound(List<TPlayer> players, int playersPerMatch) {
        List<List<TPlayer>> matches = Lists.newArrayList();
        List<TPlayer> playersRemaining = Lists.newArrayList(players);
        while (playersRemaining.size() > 0) {
            List<TPlayer> match = ImmutableList.copyOf(Iterables.limit(playersRemaining, playersPerMatch));
            playersRemaining.removeAll(match);
            matches.add(match);
        }
        return matches;
    }

}
