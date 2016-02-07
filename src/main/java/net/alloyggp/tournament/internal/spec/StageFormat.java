package net.alloyggp.tournament.internal.spec;

import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.alloyggp.tournament.internal.runner.FormatRunner;
import net.alloyggp.tournament.internal.runner.SingleEliminationFormatRunner;
import net.alloyggp.tournament.internal.runner.SwissFormat1Runner;
import net.alloyggp.tournament.internal.runner.SwissFormat2Runner;

public enum StageFormat {
    SINGLE_ELIMINATION("singleElimination1", true, new Supplier<FormatRunner>() {
        @Override
        public FormatRunner get() {
            return SingleEliminationFormatRunner.create();
        }
    }),
    SWISS1("swiss1", true, new Supplier<FormatRunner>() {
        @Override
        public FormatRunner get() {
            return SwissFormat1Runner.create();
        }
    }),
    SWISS2("swiss2", false, new Supplier<FormatRunner>() {
        @Override
        public FormatRunner get() {
            return SwissFormat2Runner.create();
        }
    }),
    ;
    private final String yamlName;
    private final boolean stable;
    private final Supplier<FormatRunner> runnerSupplier;

    private StageFormat(String yamlName, boolean stable, Supplier<FormatRunner> runnerSupplier) {
        this.yamlName = yamlName;
        this.stable = stable;
        this.runnerSupplier = runnerSupplier;
    }

    /*package-private*/ FormatRunner getRunner() {
        return runnerSupplier.get();
    }

    private static final Supplier<Map<String, StageFormat>> NAME_LOOKUP = Suppliers.memoize(
            new Supplier<Map<String, StageFormat>>() {
                @Override
                public Map<String, StageFormat> get() {
                    ImmutableMap.Builder<String, StageFormat> formats = ImmutableMap.builder();
                    for (StageFormat format : values()) {
                        formats.put(format.yamlName, format);
                    }
                    return formats.build();
                }
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

    public boolean isStable() {
        return stable;
    }
}
