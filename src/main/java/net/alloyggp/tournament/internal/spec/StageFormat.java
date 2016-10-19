package net.alloyggp.tournament.internal.spec;

import java.util.Comparator;
import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

import net.alloyggp.tournament.internal.runner.FormatRunner;
import net.alloyggp.tournament.internal.runner.SingleEliminationFormat1Runner;
import net.alloyggp.tournament.internal.runner.SwissFormat1Runner;
import net.alloyggp.tournament.internal.runner.SwissFormat2Runner;

public enum StageFormat {
    SINGLE_ELIMINATION1("singleElimination1", true, new Supplier<FormatRunner>() {
        @Override
        public FormatRunner get() {
            return SingleEliminationFormat1Runner.create();
        }
    }, Ordering.<Integer>natural().reverse()),
    SWISS1("swiss1", true, new Supplier<FormatRunner>() {
        @Override
        public FormatRunner get() {
            return SwissFormat1Runner.create();
        }
    }, Ordering.<Integer>natural()),
    SWISS2("swiss2", false, new Supplier<FormatRunner>() {
        @Override
        public FormatRunner get() {
            return SwissFormat2Runner.create();
        }
    }, Ordering.<Integer>natural()),
    ;
    private final String yamlName;
    private final boolean stable;
    private final Supplier<FormatRunner> runnerSupplier;
    private final Comparator<Integer> roundComparator;

    private StageFormat(String yamlName, boolean stable, Supplier<FormatRunner> runnerSupplier,
            Comparator<Integer> roundComparator) {
        this.yamlName = yamlName;
        this.stable = stable;
        this.runnerSupplier = runnerSupplier;
        this.roundComparator = roundComparator;
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

    /**
     * Returns a comparator that sorts round numbers from those that should take place earlier in the tournament
     * to those that should take place later. This is used to determine, for example, if a round's
     * results need to be thrown out because of a modification made to an earlier round.
     */
    public Comparator<Integer> getRoundComparator() {
        return roundComparator;
    }
}
