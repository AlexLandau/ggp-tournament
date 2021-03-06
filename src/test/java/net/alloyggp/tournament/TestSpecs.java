package net.alloyggp.tournament;

import java.io.File;

import net.alloyggp.tournament.api.TTournamentSpecParser;
import net.alloyggp.tournament.internal.spec.TournamentSpec;

public class TestSpecs {
    private TestSpecs() {
        //Not instantiable
    }

    public static TournamentSpec load(String name) {
        File file = new File("testSpecs", name + ".yaml");
        return (TournamentSpec) TTournamentSpecParser.parseYamlFile(file);
    }
}
