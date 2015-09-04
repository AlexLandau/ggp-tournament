package net.alloyggp.swiss.spec;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.alloyggp.swiss.YamlUtils;
import net.alloyggp.swiss.api.Game;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.Player;

@Immutable
public class MatchSpec {
    private final Game game;
    private final int startClock;
    private final int playClock;
    private final ImmutableList<Integer> playerSeedOrder;

    private MatchSpec(Game game, int startClock, int playClock,
            ImmutableList<Integer> playerSeedOrder) {
        this.game = game;
        this.startClock = startClock;
        this.playClock = playClock;
        this.playerSeedOrder = playerSeedOrder;
    }

    private static final ImmutableSet<String> ALLOWED_KEYS = ImmutableSet.of(
            "game",
            "startClock",
            "playClock",
            "seedRoles"
            );

    @SuppressWarnings("unchecked")
    public static MatchSpec parseYaml(Object yamlMatch, Map<String, Game> games) {
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
            playerSeedOrder = ImmutableList.copyOf(
                    IntStream.range(0, game.getNumRoles())
                    .boxed().collect(Collectors.toList()));
        }

        return new MatchSpec(game, startClock, playClock, playerSeedOrder);
    }

    public Game getGame() {
        return game;
    }

    public int getStartClock() {
        return startClock;
    }

    public int getPlayClock() {
        return playClock;
    }

    //From seed order (best first), to the order of their roles
    public ImmutableList<Player> putInOrder(List<Player> playersBestSeedFirst) {
        ImmutableList.Builder<Player> players = ImmutableList.builder();
        for (int seed : playerSeedOrder) {
            players.add(playersBestSeedFirst.get(seed));
        }
        return players.build();
    }

    public MatchSetup createMatchSetup(String matchId, List<Player> playersHighestSeedFirst) {
        return MatchSetup.create(matchId, game, putInOrder(playersHighestSeedFirst),
                startClock, playClock);
    }
}
