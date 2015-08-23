package net.alloyggp.swiss;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        AssignedMatchesConsistencyTest.class,
        MatchIdsTest.class,
        MatchIdUniquenessTest.class,
        MatchResultOrderingTest.class,
        SeedingsTest.class})
public class AllTests {

}
