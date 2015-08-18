package net.alloyggp.swiss.api;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import net.alloyggp.swiss.FormatRunner;

@Immutable
public class StageSpec {
	private final int stageNum;
	private final StageFormat format;
	//Division into groups? Leave for later
	private final ImmutableList<RoundSpec> rounds;
	private final int playerCutoff;

	private StageSpec(int stageNum, StageFormat format, ImmutableList<RoundSpec> rounds, int playerCutoff) {
		this.stageNum = stageNum;
		this.format = format;
		this.rounds = rounds;
		this.playerCutoff = playerCutoff;
	}

	//TODO: Include some information on how to turn standings at the end of this round
	//into the seeding for the next round

	//After a stage is done (all the matches in it have been played), we want to
	//pass along a set of standings to the next stage (or the end of the tournament).

	//So: Standings from the end of one tournament can turn into a seeding for the next.
	//Actually, it sounds like it might make more sense to have a stage handle turning its
	//own end standings into a seeding for the next round (or ranking for the final outcome),
	//rather than deal with its inputs; and we can have the outmost level of the tournament
	//(or even the client?) handle seeding rather than the stage.

	public Set<MatchSetup> getMatchesToRun(String tournamentInternalName, Seeding initialSeeding, List<MatchResult> resultsSoFar) {
		FormatRunner runner = format.getRunner(tournamentInternalName, stageNum);
		return runner.getMatchesToRun(initialSeeding, rounds, resultsSoFar);
	}

	public TournamentStandings getStandingsSoFar(String tournamentInternalName, Seeding initialSeeding, List<MatchResult> resultsSoFar) {
		FormatRunner runner = format.getRunner(tournamentInternalName, stageNum);
		return runner.getStandingsSoFar(initialSeeding, rounds, resultsSoFar);
	}

	public Seeding getSeedingsFromFinalStandings(TournamentStandings standings) {
		List<PlayerScore> playersBestFirst = ImmutableList.copyOf(standings.getScores()).reverse();
		playersBestFirst = ImmutableList.copyOf(Iterables.limit(playersBestFirst, playerCutoff));
		List<Player> players = playersBestFirst.stream()
									.map(PlayerScore::getPlayer)
									.collect(Collectors.toList());
		return Seeding.create(players);
	}
}
