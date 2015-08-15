package net.alloyggp.swiss.api;

import net.alloyggp.swiss.FormatRunner;
import net.alloyggp.swiss.SingleEliminationFormatRunner;

public enum StageFormat {
	SINGLE_ELIMINATION((name, stageNum) -> SingleEliminationFormatRunner.create(name, stageNum)),
	;
	private final FormatRunnerSupplier runnerSupplier;

	private StageFormat(FormatRunnerSupplier runnerSupplier) {
		this.runnerSupplier = runnerSupplier;
	}

	/*package-private*/ FormatRunner getRunner(String tournamentInternalName, int stageNum) {
		return runnerSupplier.getRunner(tournamentInternalName, stageNum);
	}

	private static interface FormatRunnerSupplier {
		FormatRunner getRunner(String tournamentInternalName, int stageNum);
	}
}
