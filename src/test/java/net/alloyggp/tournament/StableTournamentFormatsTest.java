package net.alloyggp.tournament;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import net.alloyggp.escaperope.Delimiters;
import net.alloyggp.escaperope.RopeDelimiter;
import net.alloyggp.escaperope.rope.Rope;
import net.alloyggp.escaperope.rope.ropify.CoreWeavers;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.tournament.api.TMatchResult;
import net.alloyggp.tournament.api.TMatchSetup;
import net.alloyggp.tournament.api.TRanking;
import net.alloyggp.tournament.api.TSeeding;
import net.alloyggp.tournament.api.TTournamentStatus;
import net.alloyggp.tournament.internal.rope.Weavers;
import net.alloyggp.tournament.internal.spec.StageSpec;
import net.alloyggp.tournament.internal.spec.TournamentSpec;

/**
 * This test ensures that the results from tournament formats that
 * have been deemed "stable" do not change. As a result, users that
 * have run tournaments with these formats can upgrade their version
 * of the library safely, even if they still rely on the library to
 * interpret past tournament results.
 */
@RunWith(Parameterized.class)
public class StableTournamentFormatsTest {
    @Parameters(name = "{index}: {0} players, {1}")
    public static Iterable<Object[]> data() {
        return FuzzTests.getParameters();
    }

    private final int numPlayers;
    private final String testSpec;

    public StableTournamentFormatsTest(int numPlayers, String testSpec) {
        this.numPlayers = numPlayers;
        this.testSpec = testSpec;
    }

    @Test
    public void testStandingsHistoryNotRewritten() throws IOException {
        TournamentSpec spec = TestSpecs.load(testSpec);
        assumeStable(spec);

        for (long seed = 0L; seed < 100L; seed++) {
            TournamentReport storedReport = loadStoredReport(numPlayers, testSpec, seed);
            TournamentReport currentReport = createReport(numPlayers, spec, seed);
            if (!storedReport.equals(currentReport)) {
                fail("The generated and current reports did not match!");
            }
        }
    }

    private static void assumeStable(TournamentSpec spec) {
        for (StageSpec stage : spec.getStages()) {
            Assume.assumeTrue(stage.getFormat().isStable());
        }
    }

    private static TournamentReport createReport(int numPlayers, TournamentSpec spec, long seed) {
        Random random = new Random(seed);
        TSeeding initialSeeding = FuzzTests.createRandomSeeding(random, numPlayers);
        TTournamentStatus status = TTournamentStatus.getInitialStatus(spec, initialSeeding);
//        List<TRanking> standingsSoFar = Lists.newArrayList();
//        standingsSoFar.add(StandardRanking.createForSeeding(initialSeeding));
        Set<TMatchSetup> matchSetups = Sets.newHashSet();

        while (true) {
            Set<TMatchSetup> nextMatches = status.getNextMatchesToRun().getMatchesToRun();
            if (nextMatches.isEmpty()) {
                break;
            }
            //Pick one and choose a result for it
            TMatchSetup matchToResolve = FuzzTests.pickMatchAtRandom(random, nextMatches);
            matchSetups.add(matchToResolve);
            TMatchResult result = FuzzTests.getResult(random, matchToResolve);
            status = status.withNewResult(result);
        }
        return TournamentReport.create(
                matchSetups,
                status.getResultsSoFar(),
                status.getStandingsHistory());
    }

    private static class TournamentReport {
        private final ImmutableSet<TMatchSetup> matchSetups;
        private final ImmutableSet<TMatchResult> resultsSoFar;
        private final ImmutableList<TRanking> finalStandingsHistory;

        private TournamentReport(ImmutableSet<TMatchSetup> matchSetups, ImmutableSet<TMatchResult> resultsSoFar,
                ImmutableList<TRanking> finalStandingsHistory) {
            this.matchSetups = matchSetups;
            this.resultsSoFar = resultsSoFar;
            this.finalStandingsHistory = finalStandingsHistory;
        }

        public static TournamentReport create(Set<TMatchSetup> matchSetups, Set<TMatchResult> resultsSoFar,
                List<TRanking> finalStandingsHistory) {
            return new TournamentReport(ImmutableSet.copyOf(matchSetups),
                    ImmutableSet.copyOf(resultsSoFar),
                    ImmutableList.copyOf(finalStandingsHistory));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((finalStandingsHistory == null) ? 0 : finalStandingsHistory.hashCode());
            result = prime * result + ((matchSetups == null) ? 0 : matchSetups.hashCode());
            result = prime * result + ((resultsSoFar == null) ? 0 : resultsSoFar.hashCode());
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
            TournamentReport other = (TournamentReport) obj;
            if (finalStandingsHistory == null) {
                if (other.finalStandingsHistory != null) {
                    return false;
                }
            } else if (!finalStandingsHistory.equals(other.finalStandingsHistory)) {
                return false;
            }
            if (matchSetups == null) {
                if (other.matchSetups != null) {
                    return false;
                }
            } else if (!matchSetups.equals(other.matchSetups)) {
                return false;
            }
            if (resultsSoFar == null) {
                if (other.resultsSoFar != null) {
                    return false;
                }
            } else if (!resultsSoFar.equals(other.resultsSoFar)) {
                return false;
            }
            return true;
        }

        public String toSerializedString() {
            RopeDelimiter ropeDelimiter = Delimiters.getPrototypeRopeDelimiter();
            return ropeDelimiter.delimit(toRope());
        }

        private Rope toRope() {
            RopeBuilder ropeList = RopeBuilder.create();
            ropeList.add(matchSetups, CoreWeavers.setOf(Weavers.MATCH_SETUP));
            ropeList.add(resultsSoFar, CoreWeavers.setOf(Weavers.MATCH_RESULT));
            ropeList.add(finalStandingsHistory, CoreWeavers.listOf(Weavers.RANKING));
            return ropeList.toRope();
        }

        public static TournamentReport fromSerializedString(String fileContents) {
            // TODO Implement
            RopeDelimiter ropeDelimiter = Delimiters.getPrototypeRopeDelimiter();
            List<Rope> list = ropeDelimiter.undelimit(fileContents).asList();
            Set<TMatchSetup> matchSetups = CoreWeavers.setOf(Weavers.MATCH_SETUP).fromRope(list.get(0));
            Set<TMatchResult> resultsSoFar = CoreWeavers.setOf(Weavers.MATCH_RESULT).fromRope(list.get(1));
            List<TRanking> finalStandingsHistory = CoreWeavers.listOf(Weavers.RANKING).fromRope(list.get(2));

            return create(matchSetups, resultsSoFar, finalStandingsHistory);
        }
    }

    /**
     * When run, generates and stores the reports for the tournaments that would
     * be tested this way.
     */
    public static void main(String[] args) throws IOException {
        //TODO: Store the results calculated this way in a readable format
        for (Object[] data : data()) {
            int numPlayers = (int) data[0];
            String testSpec = (String) data[1];
            TournamentSpec spec = TestSpecs.load(testSpec);
            assumeStable(spec);

            for (long seed = 0L; seed < 100L; seed++) {
                TournamentReport report = createReport(numPlayers, spec, seed);
                storeReport(numPlayers, testSpec, seed, report);
            }
        }
    }

    private static TournamentReport loadStoredReport(int numPlayers, String testSpec, long seed) throws IOException {
        File file = getStoredReportFile(numPlayers, testSpec, seed);
        String fileContents = Files.toString(file, Charsets.UTF_8);
        return TournamentReport.fromSerializedString(fileContents);
    }

    private static void storeReport(int numPlayers, String testSpec, long seed, TournamentReport report) throws IOException {
        File file = getStoredReportFile(numPlayers, testSpec, seed);
        Files.write(report.toSerializedString(), file, Charsets.UTF_8);
    }

    private static File getStoredReportFile(int numPlayers, String testSpec, long seed) {
        File baseFolder = new File("testFiles");
        File subfolder = new File(baseFolder, testSpec + "_" + numPlayers);
        if (!subfolder.isDirectory()) {
            boolean mkdirsSuccess = subfolder.mkdirs();
            if (!mkdirsSuccess) {
                throw new RuntimeException("The subfolder " + subfolder.getAbsolutePath() + " does not exist and could not be created");
            }
        }
        return new File(subfolder, seed + ".report");
    }

}
