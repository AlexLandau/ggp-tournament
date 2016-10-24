package net.alloyggp.tournament.internal.rope;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import net.alloyggp.escaperope.rope.Rope;
import net.alloyggp.escaperope.rope.StringRope;
import net.alloyggp.escaperope.rope.ropify.CoreWeavers;
import net.alloyggp.escaperope.rope.ropify.ListWeaver;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.escaperope.rope.ropify.RopeList;
import net.alloyggp.escaperope.rope.ropify.Weaver;
import net.alloyggp.tournament.api.TGame;
import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchResult.Outcome;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TPlayer;
import net.alloyggp.tournament.api.TPlayerScore;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TScore;
import net.alloyggp.tournament.internal.Game;
import net.alloyggp.tournament.internal.SimpleScore;
import net.alloyggp.tournament.internal.StandardRanking;
import net.alloyggp.tournament.internal.StandardRanking.EmptyScore;
import net.alloyggp.tournament.internal.runner.SingleEliminationFormat1Runner;
import net.alloyggp.tournament.internal.runner.SwissFormat1Runner;
import net.alloyggp.tournament.internal.runner.SwissFormat2Runner;
import net.alloyggp.tournament.internal.spec.TournamentSpec;

public class Weavers {

    public static final Weaver<TMatchSetup> MATCH_SETUP = new ListWeaver<TMatchSetup>() {
        @Override
        protected void addToList(TMatchSetup object, RopeBuilder list) {
            list.add(object.getMatchId());
            list.add(object.getGame(), GAME);
            list.add(object.getPlayers(), CoreWeavers.listOf(PLAYER));
            list.add(object.getStartClock());
            list.add(object.getPlayClock());
        }

        @Override
        public TMatchSetup fromRope(RopeList list) {
            String matchId = list.getString(0);
            TGame game = list.get(1, GAME);
            List<TPlayer> players = list.get(2, CoreWeavers.listOf(PLAYER));
            int startClock = list.getInt(3);
            int playClock = list.getInt(4);

            return TMatchSetup.create(matchId, game, players, startClock, playClock);
        }

    };

    public static final Weaver<TGame> GAME = new ListWeaver<TGame>() {
        @Override
        public void addToList(TGame object, RopeBuilder list) {
            list.add(object.getId());
            list.add(object.getUrl());
            list.add(object.getNumRoles());
            list.add(object.isFixedSum());
        }

        @Override
        protected TGame fromRope(RopeList list) {
            String id = list.getString(0);
            String url = list.getString(1);
            int numRoles = list.getInt(2);
            boolean fixedSum = list.getBoolean(3);

            return Game.create(id, url, numRoles, fixedSum);
        }
    };

    public static final Weaver<TPlayer> PLAYER = new Weaver<TPlayer>() {
        @Override
        public Rope toRope(TPlayer object) {
            return StringRope.create(object.getId());
        }

        @Override
        public TPlayer fromRope(Rope rope) {
            return TPlayer.create(rope.asString());
        }
    };

    //TODO: Make this kind of subclass-delegating thing a core weaver?
    private static final ImmutableMap<String, Weaver<TScore>> SCORE_SUBCLASS_WEAVERS =
            ImmutableMap.<String, Weaver<TScore>>builder()
            .put("CutoffScore", TournamentSpec.CUTOFF_SCORE_WEAVER)
            .put("EliminationScore", SingleEliminationFormat1Runner.SCORE_WEAVER)
            .put("EmptyScore", EmptyScore.WEAVER)
            .put("SwissScore", SwissFormat1Runner.SCORE_WEAVER)
            .put("Swiss2Score", SwissFormat2Runner.SCORE_WEAVER)
            .put("SimpleScore", SimpleScore.WEAVER)
            .build();

    public static final Weaver<TScore> SCORE = new ListWeaver<TScore>() {
        @Override
        protected void addToList(TScore score, RopeBuilder list) {
            Weaver<TScore> weaver = SCORE_SUBCLASS_WEAVERS.get(score.getClass().getSimpleName());
            list.add(score.getClass().getSimpleName());
            list.add(weaver.toRope(score));
        }

        @Override
        protected TScore fromRope(RopeList list) {
            String className = list.getString(0);
            Weaver<TScore> weaver = SCORE_SUBCLASS_WEAVERS.get(className);
            return list.get(1, weaver);
        }
    };

    public static final Weaver<TPlayerScore> PLAYER_SCORE = new ListWeaver<TPlayerScore>() {
        @Override
        protected void addToList(TPlayerScore object, RopeBuilder list) {
            list.add(object.getPlayer(), PLAYER);
            list.add(object.getScore(), SCORE);
            list.add(object.getSeedFromRoundStart());
        }

        @Override
        protected TPlayerScore fromRope(RopeList list) {
            TPlayer player = list.get(0, PLAYER);
            TScore score = list.get(1, SCORE);
            int seedFromRoundStart = list.getInt(2);
            return TPlayerScore.create(player, score, seedFromRoundStart);
        }
    };

    public static final Weaver<TRanking> RANKING = new ListWeaver<TRanking>() {
        @Override
        protected void addToList(TRanking object, RopeBuilder list) {
            list.add(object.getScores(), CoreWeavers.setOf(PLAYER_SCORE));
        }

        @Override
        protected TRanking fromRope(RopeList list) {
            Set<TPlayerScore> playerScores = list.get(0, CoreWeavers.setOf(PLAYER_SCORE));
            return StandardRanking.create(playerScores);
        }
    };

    public static final Weaver<TMatchResult> MATCH_RESULT = new ListWeaver<TMatchResult>() {
        @Override
        protected void addToList(TMatchResult object, RopeBuilder list) {
            list.add(object.getMatchId());
            list.add(object.getOutcome(), CoreWeavers.enumOf(Outcome.class));
            if (!object.wasAborted()) {
                list.add(object.getGoals(), CoreWeavers.listOfIntegers());
            }
        }

        @Override
        protected TMatchResult fromRope(RopeList list) {
            Outcome outcome = list.get(1, CoreWeavers.enumOf(Outcome.class));
            String matchId = list.getString(0);
            if (outcome == Outcome.COMPLETED) {
                List<Integer> goals = list.get(2, CoreWeavers.listOfIntegers());
                return TMatchResult.getSuccessfulMatchResult(matchId, goals);
            } else {
                return TMatchResult.getAbortedMatchResult(matchId);
            }
        }
    };

}
