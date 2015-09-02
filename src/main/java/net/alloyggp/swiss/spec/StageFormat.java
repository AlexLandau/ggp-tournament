package net.alloyggp.swiss.spec;

import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.alloyggp.swiss.FormatRunner;
import net.alloyggp.swiss.SingleEliminationFormatRunner;
import net.alloyggp.swiss.SwissFormat1Runner;

public enum StageFormat {
    SINGLE_ELIMINATION("singleElimination",
            () -> SingleEliminationFormatRunner.create()),
    SWISS1("swiss1",
            () -> SwissFormat1Runner.create()),
    ;
    private final String yamlName;
    private final Supplier<FormatRunner> runnerSupplier;

    private StageFormat(String yamlName, Supplier<FormatRunner> runnerSupplier) {
        this.yamlName = yamlName;
        this.runnerSupplier = runnerSupplier;
    }

    /*package-private*/ FormatRunner getRunner() {
        return runnerSupplier.get();
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

    public void validateRounds(ImmutableList<RoundSpec> rounds) {
        getRunner().validateRounds(rounds);
    }
}
