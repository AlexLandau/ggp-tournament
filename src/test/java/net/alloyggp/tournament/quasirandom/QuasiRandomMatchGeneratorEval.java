package net.alloyggp.tournament.quasirandom;

import java.util.List;
import java.util.Set;

import net.alloyggp.tournament.FuzzTests;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.internal.quasirandom.QuasiRandomMatchGenerator;
import net.alloyggp.tournament.internal.quasirandom.RolesFirstImpl3p2;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

public class QuasiRandomMatchGeneratorEval {

    public static void main(String[] args) {
        List<List<Integer>> sampleCases = ImmutableList.<List<Integer>>of(
                Lists.newArrayList(13, 2, 2),
                Lists.newArrayList(14, 2, 2),
                Lists.newArrayList(13, 3, 3),
                Lists.newArrayList(14, 3, 3),
                Lists.newArrayList(15, 3, 3),
                Lists.newArrayList(13, 4, 4),
                Lists.newArrayList(14, 4, 4),
                Lists.newArrayList(15, 4, 4),
                Lists.newArrayList(16, 4, 4));
        List<QuasiRandomMatchGenerator> generators = ImmutableList.of(
//                new RandomImpl(),
//                new FirstTryImpl(),
//                new RolesFirstImpl2(),
//                new RolesFirstImpl2p1(),
                new RolesFirstImpl3(),
                new RolesFirstImpl3p1(),
                new RolesFirstImpl3p2());

        for (List<Integer> sampleCase : sampleCases) {
            System.out.println("Testing case " + sampleCase);
            int numPlayers = sampleCase.get(0);
            int playersPerMatch = sampleCase.get(1);
            int numMatches = sampleCase.get(2);

            List<TPlayer> players = FuzzTests.createPlayers(numPlayers);
            for (QuasiRandomMatchGenerator gen : generators) {
                System.out.println("  Testing generator " + gen.getClass().getName());

                List<List<List<TPlayer>>> matchups = gen.generateMatchups(players, playersPerMatch, numMatches);
                System.out.println("    Matchups: " + matchups);

                //Variance in # times playing each opponent minimized, for each player
                Multiset<Set<TPlayer>> matchupPairingCounts = HashMultiset.create();
                for (List<List<TPlayer>> round : matchups) {
                    for (List<TPlayer> match : round) {
                        for (Set<TPlayer> pairing : getPairings(match)) {
                            matchupPairingCounts.add(pairing);
                        }
                    }
                }

                for (TPlayer player : players) {
                    int minMaxPairingsDiff = getMinMaxPairingsDiff(player, players, matchupPairingCounts);
                    System.out.println("    Min/max pairings diff for " + player + ": " + minMaxPairingsDiff);
                }

                //Variance in # times unassigned minimized, across players
                int minUnassigned = Integer.MAX_VALUE;
                int maxUnassigned = Integer.MIN_VALUE;
                for (TPlayer player : players) {
                    int unassigned = getNumRoundsUnassigned(player, matchups);
                    System.out.println("    Byes for " + player + ": " + unassigned);
                    if (unassigned < minUnassigned) {
                        minUnassigned = unassigned;
                    }
                    if (unassigned > maxUnassigned) {
                        maxUnassigned = unassigned;
                    }
                }
                System.out.println("    Min/max diff for number of byes: " + (maxUnassigned - minUnassigned));

                //Variance in # times in each role minimized, for each player
                for (TPlayer player : players) {
                    int minMaxTimesInRole = getMinMaxTimesInRole(player, playersPerMatch, matchups);
                    System.out.println("    Min/max times in roles for " + player + ": " + minMaxTimesInRole);
                }
            }
        }
    }

    private static int getMinMaxPairingsDiff(TPlayer player,
            List<TPlayer> players, Multiset<Set<TPlayer>> matchupPairingCounts) {
        int minPairings = Integer.MAX_VALUE;
        int maxPairings = Integer.MIN_VALUE;
        for (TPlayer opponent : players) {
            if (!player.equals(opponent)) {
                int pairings = matchupPairingCounts.count(ImmutableSet.of(player, opponent));
                if (pairings < minPairings) {
                    minPairings = pairings;
                }
                if (pairings > maxPairings) {
                    maxPairings = pairings;
                }
            }
        }
        return maxPairings - minPairings;
    }

    private static int getMinMaxTimesInRole(TPlayer player,
            int playersPerMatch, List<List<List<TPlayer>>> matchups) {
        List<Integer> timesInEachRole = Lists.newArrayList();
        for (int i = 0; i < playersPerMatch; i++) {
            timesInEachRole.add(0);
        }
        for (List<List<TPlayer>> round : matchups) {
            for (List<TPlayer> match : round) {
                int index = match.indexOf(player);
                if (index != -1) {
                    timesInEachRole.set(index, 1 + timesInEachRole.get(index));
                }
            }
        }
        int min = Ordering.natural().min(timesInEachRole);
        int max = Ordering.natural().max(timesInEachRole);
        return max - min;
    }

    private static int getNumRoundsUnassigned(TPlayer player,
            List<List<List<TPlayer>>> matchups) {
        int count = 0;
        for (List<List<TPlayer>> round : matchups) {
            if (!playerInRound(player, round)) {
                count++;
            }
        }
        return count;
    }

    private static boolean playerInRound(TPlayer player,
            List<List<TPlayer>> round) {
        for (List<TPlayer> match : round) {
            if (match.contains(player)) {
                return true;
            }
        }
        return false;
    }

    private static List<Set<TPlayer>> getPairings(List<TPlayer> match) {
        List<Set<TPlayer>> results = Lists.newArrayList();
        for (int i = 0; i < match.size(); i++) {
            for (int j = i + 1; j < match.size(); j++) {
                results.add(ImmutableSet.of(match.get(i), match.get(j)));
            }
        }
        return results;
    }

}
