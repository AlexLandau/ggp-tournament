package net.alloyggp.tournament.internal.spec;

import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import net.alloyggp.tournament.api.TGame;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.YamlUtils;
import net.alloyggp.tournament.internal.admin.InternalAdminAction;

@Immutable
public class MatchSpec {
    private final TGame game;
    private final int startClock;
    private final int playClock;
    private final ImmutableList<Integer> playerSeedOrder;
    private final double weight;
    private final int matchNum;

    private MatchSpec(TGame game, int startClock, int playClock,
            ImmutableList<Integer> playerSeedOrder, double weight,
            int matchNum) {
        Preconditions.checkArgument(weight >= 0.0);
        this.game = game;
        this.startClock = startClock;
        this.playClock = playClock;
        this.playerSeedOrder = playerSeedOrder;
        this.weight = weight;
        this.matchNum = matchNum;
    }

    private static final ImmutableSet<String> ALLOWED_KEYS = ImmutableSet.of(
            "game",
            "startClock",
            "playClock",
            "seedRoles",
            "weight"
            );

    @SuppressWarnings("unchecked")
    public static MatchSpec parseYaml(Object yamlMatch, int matchNum, Map<String, Game> games) {
        Map<String, Object> matchMap = (Map<String, Object>) yamlMatch;
        YamlUtils.validateKeys(matchMap, "match", ALLOWED_KEYS);
        String gameName = (String) matchMap.get("game");
        Game game = games.get(gameName);
        if (game == null) {
            throw new IllegalArgumentException("Could not find game specification with name "
                    + gameName + " in the YAML file.");
        }
        int startClock = (int) matchMap.get("startClock");
        int playClock = (int) matchMap.get("playClock");
        ImmutableList<Integer> playerSeedOrder;
        if (matchMap.containsKey("seedRoles")) {
            playerSeedOrder = ImmutableList.copyOf(
                    (List<Integer>) matchMap.get("seedRoles"));
        } else {
            playerSeedOrder = getDefaultPlayerSeedOrder(game.getNumRoles());
        }
        double weight = 1.0;
        if (matchMap.containsKey("weight")) {
            weight = (double) matchMap.get("weight");
        }

        return new MatchSpec(game, startClock, playClock, playerSeedOrder, weight, matchNum);
    }

    private static ImmutableList<Integer> getDefaultPlayerSeedOrder(int numRoles) {
        List<Integer> seeds = Lists.newArrayList();
        for (int i = 0; i < numRoles; i++) {
            seeds.add(i);
        }
        return ImmutableList.copyOf(seeds);
    }

    public TGame getGame() {
        return game;
    }

    public int getStartClock() {
        return startClock;
    }

    public int getPlayClock() {
        return playClock;
    }

    public ImmutableList<Integer> getPlayerSeedOrder() {
        return playerSeedOrder;
    }

    //From seed order (best first), to the order of their roles
    public ImmutableList<TPlayer> putInOrder(List<TPlayer> playersBestSeedFirst) {
        ImmutableList.Builder<TPlayer> players = ImmutableList.builder();
        for (int seed : playerSeedOrder) {
            players.add(playersBestSeedFirst.get(seed));
        }
        return players.build();
    }

    public TMatchSetup createMatchSetup(String matchId, List<TPlayer> playersHighestSeedFirst) {
        return TMatchSetup.create(matchId, game, putInOrder(playersHighestSeedFirst),
                startClock, playClock);
    }

    public double getWeight() {
        return weight;
    }

    public MatchSpec apply(InternalAdminAction action, int stageNum,
            int roundNum) {
        TGame newGame = action.editMatchGame(game, stageNum, roundNum, matchNum);
        int newStartClock = action.editMatchStartClock(startClock, stageNum, roundNum, matchNum);
        int newPlayClock = action.editMatchPlayClock(playClock, stageNum, roundNum, matchNum);
        ImmutableList<Integer> newPlayerSeedOrder = action.editMatchPlayerSeedOrder(
                playerSeedOrder, stageNum, roundNum, matchNum);
        double newWeight = action.editMatchWeight(weight, stageNum, roundNum, matchNum);

        return new MatchSpec(newGame, newStartClock, newPlayClock, newPlayerSeedOrder, newWeight, matchNum);
    }
}
