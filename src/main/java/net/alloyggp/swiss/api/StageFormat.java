package net.alloyggp.swiss.api;

import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;

import net.alloyggp.swiss.FormatRunner;
import net.alloyggp.swiss.SingleEliminationFormatRunner;

public enum StageFormat {
	SINGLE_ELIMINATION("singleElimination",
			(name, stageNum) -> SingleEliminationFormatRunner.create(name, stageNum)),
	;
	private final String yamlName;
	private final FormatRunnerSupplier runnerSupplier;

	private StageFormat(String yamlName, FormatRunnerSupplier runnerSupplier) {
		this.yamlName = yamlName;
		this.runnerSupplier = runnerSupplier;
	}

	/*package-private*/ FormatRunner getRunner(String tournamentInternalName, int stageNum) {
		return runnerSupplier.getRunner(tournamentInternalName, stageNum);
	}

	private static interface FormatRunnerSupplier {
		FormatRunner getRunner(String tournamentInternalName, int stageNum);
	}

	private static final Supplier<Map<String, StageFormat>> NAME_LOOKUP = Suppliers.memoize(() -> {
		ImmutableMap.Builder<String, StageFormat> formats = ImmutableMap.builder();
		for (StageFormat format : values()) {
			formats.put(format.yamlName, format);
		}
		return formats.build();
	});

	public static StageFormat parse(String formatName) {
		StageFormat format = NAME_LOOKUP.get().get(formatName);
		if (format == null) {
			throw new IllegalArgumentException("No format name found for name " + formatName);
		}
		return format;
	}

	@Override
	public String toString() {
		return yamlName;
	}
}
