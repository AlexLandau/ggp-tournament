package net.alloyggp.swiss;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.alloyggp.swiss.api.MatchResult;
import net.alloyggp.swiss.api.MatchResult.Outcome;
import net.alloyggp.swiss.api.MatchSetup;
import net.alloyggp.swiss.api.MatchSpec;
import net.alloyggp.swiss.api.Player;
import net.alloyggp.swiss.api.PlayerScore;
import net.alloyggp.swiss.api.RoundSpec;
import net.alloyggp.swiss.api.Score;
import net.alloyggp.swiss.api.Seeding;
import net.alloyggp.swiss.api.TournamentStandings;

/*
 * Non-obvious aspects of this format implementation:
 * - In general, we use the system of creating a fixed bracket that pairs
 *   opponents such that the highest-ranked players would play the lowest-ranked
 *   (remaining) opponents in each round if the higher-seeded player always wins.
 * - If the number of players is not a power of 2, we cut to a power of two in a
 *   non-standard first round. Byes are awarded to the highest-ranked players first.
 * - This could be used in tournaments where the number of players is unknown when
 *   the tournament is planned, and so the number of rounds defined is unsuitable.
 *   Thus, we use the rounds in a special way: We always play such that the last
 *   round defined is the final round, the second-to-last is the semifinals, and
 *   so on. If the number of rounds needed exceeds the number of rounds defined,
 *   the first round defined is reused for the additional rounds.
 */
public class SingleEliminationFormatRunner implements FormatRunner {
	private final String tournamentInternalName;
	private final int stageNum;

	private SingleEliminationFormatRunner(String tournamentInternalName, int stageNum) {
		this.tournamentInternalName = tournamentInternalName;
		this.stageNum = stageNum;
	}

	public static SingleEliminationFormatRunner create(String tournamentInternalName, int stageNum) {
		return new SingleEliminationFormatRunner(tournamentInternalName, stageNum);
	}

	@Override
	public Set<MatchSetup> getMatchesToRun(Seeding initialSeeding, ImmutableList<RoundSpec> rounds,
			List<MatchResult> resultsSoFar) {
		Set<MatchSetup> matchesToReturn = Sets.newHashSet();
//		Set<Player> eliminatedPlayers = getEliminatedPlayers(rounds, resultsSoFar);
		List<Player> playersByPosition = Lists.newArrayList(initialSeeding.getPlayersBestFirst());
		//If we're not a power of two, handle that
		int numPlayers = playersByPosition.size();
		int numRoundsLeft = getNumRounds(numPlayers);
		if (numPlayers > (1 << numRoundsLeft)) {
			RoundSpec round = getRoundForNumRoundsLeft(rounds, numRoundsLeft);
			int numNormalRounds = numRoundsLeft - 1;
			int numPlayinMatches = numPlayers - (1 << numNormalRounds);
			for (int i = 0; i < numPlayinMatches; i++) {
				int position1 = (1 << numNormalRounds) - i - 1;
				int position2 = (1 << numNormalRounds) + i;
				Player player1 = playersByPosition.get(position1);
				Player player2 = playersByPosition.get(position2);
				if (wonInRound(player1, numRoundsLeft, round, resultsSoFar)) {
					//... do nothing?
				} else if (wonInRound(player2, numRoundsLeft, round, resultsSoFar)) {
					playersByPosition.set(position1, player2);
					playersByPosition.set(position2, player1);
				} else {
					//TODO: Do we want to define roles according to seeding rather than bracket position?
					//If so, pass in seeding here
					matchesToReturn.add(getNextMatchForPairing(player1, player2, numRoundsLeft, round, resultsSoFar));
				}
			}
			numRoundsLeft--;
			if (!matchesToReturn.isEmpty()) {
				return matchesToReturn;
			}
		}
		//Now we handle the normal rounds
		while (numRoundsLeft > 0) {
			RoundSpec round = getRoundForNumRoundsLeft(rounds, numRoundsLeft);
			int numPlayersLeft = (1 << numRoundsLeft);
			for (int i = 0; i < numPlayersLeft/2; i++) {
				int position1 = i;
				int position2 = numPlayersLeft - i - 1;
				Player player1 = playersByPosition.get(position1);
				Player player2 = playersByPosition.get(position2);
				if (wonInRound(player1, numRoundsLeft, round, resultsSoFar)) {
					//... do nothing?
				} else if (wonInRound(player2, numRoundsLeft, round, resultsSoFar)) {
					playersByPosition.set(position1, player2);
					playersByPosition.set(position2, player1);
				} else {
					//TODO: Do we want to define roles according to seeding rather than bracket position?
					//If so, pass in seeding here
					matchesToReturn.add(getNextMatchForPairing(player1, player2, numRoundsLeft, round, resultsSoFar));
				}
			}
			numRoundsLeft--;
			if (!matchesToReturn.isEmpty()) {
				return matchesToReturn;
			}
		}
		//We're at the end of the tournament
		return matchesToReturn;
	}

	private RoundSpec getRoundForNumRoundsLeft(ImmutableList<RoundSpec> rounds, int numRoundsLeft) {
		//Count from the end, not the beginning (so the last round is always the finals)
		int index = rounds.size() - numRoundsLeft;
		if (index < 0) {
			//Repeat the first round until we hit the schedule
			index = 0;
		}
		return rounds.get(index);
	}

	private MatchSetup getNextMatchForPairing(Player player1, Player player2, int numRoundsLeft, RoundSpec round,
			List<MatchResult> resultsSoFar) {
		List<MatchResult> completedSoFar = Lists.newArrayList();
		List<MatchResult> abortedSoFar = Lists.newArrayList();
		//First, gather all the non-abandoned results so far
		for (MatchResult result : resultsSoFar) {
			String matchId = result.getSetup().getMatchId();
			int roundNumber = MatchIds.parseRoundNumber(matchId);
			if (roundNumber != numRoundsLeft) {
				continue;
			}
			if (!result.getSetup().getPlayers().contains(player1)) {
				continue;
			}
			if (result.getOutcome() == Outcome.ABORTED) {
				abortedSoFar.add(result);
			} else {
				completedSoFar.add(result);
			}
		}

		//...
		ImmutableList<MatchSpec> matches = round.getMatches();
		MatchSpec specToUse = null;
		for (int i = 0; i < matches.size(); i++) {
			specToUse = matches.get(i);
			if (haveCompleted(i, completedSoFar)) {
				continue;
			} else {
				break;
			}
		}
		int matchNum = 0;
		while (haveCompleted(matchNum, completedSoFar)) {
			matchNum++;
		}
		int priorMatchAttempts = 0;
		for (MatchResult result : abortedSoFar) {
			if (MatchIds.parseMatchNumber(result.getSetup().getMatchId()) == matchNum) {
				priorMatchAttempts++;
			}
		}

		Preconditions.checkNotNull(specToUse);
		//If we make it here, repeat the last match type
		String matchId = MatchIds.create(tournamentInternalName, stageNum, numRoundsLeft, matchNum, priorMatchAttempts);
		//TODO: Correctly define roles?
		//TODO: Accurate seeding
		return MatchSetup.create(matchId, specToUse, ImmutableList.of(player1, player2));

		//TODO: Also, if we've abandoned the current match we're trying before,
		//retry it before ___
//		for (MatchSpec match : round.getMatches()) {
//
//		}
	}

	private boolean haveCompleted(int matchNumber, List<MatchResult> completedSoFar) {
		for (MatchResult result : completedSoFar) {
			Preconditions.checkArgument(result.getOutcome() == Outcome.COMPLETED);
			String matchId = result.getSetup().getMatchId();
			int resultMatchNumber = MatchIds.parseMatchNumber(matchId);
			if (matchNumber == resultMatchNumber) {
				return true;
			}
		}
		return false;
	}

	private boolean wonInRound(Player player, int numRoundsLeft, RoundSpec round, List<MatchResult> resultsSoFar) {
		int gamesPlayed = 0;
		int pointsScored = 0;
		int pointsAboveOpponent = 0;
		for (MatchResult result : resultsSoFar) {
			String matchId = result.getSetup().getMatchId();
			int roundNumber = MatchIds.parseRoundNumber(matchId);
			if (roundNumber != numRoundsLeft) {
				continue;
			}
			if (!result.getSetup().getPlayers().contains(player)) {
				continue;
			}
			if (result.getOutcome() == Outcome.ABORTED) {
				continue;
			}
			gamesPlayed++;
			int playerIndex = result.getSetup().getPlayers().indexOf(player);
			int playerPoints = result.getGoals().get(playerIndex);
			Preconditions.checkState(playerIndex == 0 || playerIndex == 1);
			int oppIndex = 1 - playerIndex;
			int oppPoints = result.getGoals().get(oppIndex);
			pointsScored += playerPoints;
			pointsAboveOpponent += (playerPoints - oppPoints);
		}
		return wonInRound(round, gamesPlayed, pointsScored, pointsAboveOpponent);
	}

	private boolean wonInRound(RoundSpec round, int gamesPlayed, int pointsScored, int pointsAboveOpponent) {
		//We assume that any game can return 100/0...
		//We end the round once one player has enough points to guarantee they're ahead at or after the
		//end of the last scheduled match
		//TODO: Maybe assign a threshold so TOs can schedule follow-up games with more flexibility?
		if (gamesPlayed >= round.getMatches().size()) {
			if (pointsAboveOpponent > 0) {
				return true;
			}
		} else {
			//Can we end the round early?
			int gamesLeft = round.getMatches().size() - gamesPlayed;
			if (pointsAboveOpponent > 100*gamesLeft) {
				return true;
			}
		}
		return false;
	}

	private int getNumRounds(int numPlayers) {
		int numRounds = 0;
		int num = numPlayers;
		while (num > 1) {
			num = num >> 1;
			numRounds++;
		}
		if (numPlayers > (1 << numRounds)) {
			numRounds++;
		}
		return numRounds;
	}

//	private Set<Player> getEliminatedPlayers(ImmutableList<RoundSpec> rounds, List<MatchResult> resultsSoFar) {
//		// TODO Implement
//	}

	@Override
	public TournamentStandings getStandingsSoFar(Seeding initialSeeding, ImmutableList<RoundSpec> rounds,
			List<MatchResult> resultsSoFar) {
		// TODO Implement
		//For each player, add a PlayerScore...
		ImmutableSortedSet.Builder<PlayerScore> playerScores = ImmutableSortedSet.naturalOrder();
		Set<Player> eliminatedPlayers = getEliminatedPlayers(rounds, resultsSoFar);

		ImmutableList<Player> playersBestFirst = initialSeeding.getPlayersBestFirst();
		for (int i = 0; i < playersBestFirst.size(); i++) {
			Player player = playersBestFirst.get(i);
			Score score = new EliminationScore(!eliminatedPlayers.contains(player)); //In this case, just 1 if still alive, 0 otherwise
			playerScores.add(PlayerScore.create(player, score, i));
		}
	}

	private static class EliminationScore implements Score {
		private final boolean stillAlive;

		private EliminationScore(boolean stillAlive) {
			this.stillAlive = stillAlive;
		}

		@Override
		public int compareTo(Score o) {
			if (!(o instanceof EliminationScore)) {
				throw new RuntimeException("Incomparable scores being compared");
			}
			return Boolean.compare(stillAlive, ((EliminationScore)o).stillAlive);
		}
	}
}
