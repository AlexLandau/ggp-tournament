package net.alloyggp.tournament.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

import net.alloyggp.tournament.internal.spec.TournamentSpec;

public class TTournamentSpecParser {
    private TTournamentSpecParser() {
        //not instantiable
    }

    /**
     * Loads and parses a tournament specification in YAML format and returns the
     * specification.
     */
    public static TTournament parseYamlFile(File file) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            return TournamentSpec.parseYamlRootObject(new Yaml().load(in));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads and parses a tournament specification in YAML format and returns the
     * specification.
     */
    public static TTournament parseYamlString(String yamlString) {
        return TournamentSpec.parseYamlRootObject(new Yaml().load(yamlString));
    }
}
