package net.alloyggp.tournament;

import java.io.File;

import net.alloyggp.tournament.api.TournamentSpecParser;
import net.alloyggp.tournament.spec.TournamentSpec;

public class TestSpecs {
    private TestSpecs() {
        //Not instantiable
    }

    public static TournamentSpec load(String name) {
        File file = new File("testSpecs", name + ".yaml");
        return (TournamentSpec) TournamentSpecParser.parseYamlFile(file);
    }
}
