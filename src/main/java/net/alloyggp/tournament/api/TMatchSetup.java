package net.alloyggp.tournament.api;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

@Immutable
public class TMatchSetup {
    private final String matchId;
    private final TGame game;
    private final ImmutableList<TPlayer> players;
    private final int startClock;
    private final int playClock;

    private TMatchSetup(String matchId, TGame game, ImmutableList<TPlayer> players, int startClock,
            int playClock) {
        this.matchId = matchId;
        this.game = game;
        this.players = players;
        this.startClock = startClock;
        this.playClock = playClock;
    }

    public static TMatchSetup create(String matchId, TGame game, List<TPlayer> players, int startClock,
            int playClock) {
        return new TMatchSetup(matchId, game, ImmutableList.copyOf(players),
                startClock, playClock);
    }

    public String getMatchId() {
        return matchId;
    }

    public TGame getGame() {
        return game;
    }

    public List<TPlayer> getPlayers() {
        return players;
    }

    public int getStartClock() {
        return startClock;
    }

    public int getPlayClock() {
        return playClock;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((game == null) ? 0 : game.hashCode());
        result = prime * result + ((matchId == null) ? 0 : matchId.hashCode());
        result = prime * result + playClock;
        result = prime * result + ((players == null) ? 0 : players.hashCode());
        result = prime * result + startClock;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TMatchSetup other = (TMatchSetup) obj;
        if (game == null) {
            if (other.game != null) {
                return false;
            }
        } else if (!game.equals(other.game)) {
            return false;
        }
        if (matchId == null) {
            if (other.matchId != null) {
                return false;
            }
        } else if (!matchId.equals(other.matchId)) {
            return false;
        }
        if (playClock != other.playClock) {
            return false;
        }
        if (players == null) {
            if (other.players != null) {
                return false;
            }
        } else if (!players.equals(other.players)) {
            return false;
        }
        if (startClock != other.startClock) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MatchSetup [matchId=" + matchId + ", game=" + game + ", players=" + players + ", startClock="
                + startClock + ", playClock=" + playClock + "]";
    }

}
