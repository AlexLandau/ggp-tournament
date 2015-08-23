package net.alloyggp.swiss.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

public class TournamentSpecParser {

    /**
     * Loads and parses a tournament specification in YAML format and returns the
     * specification.
     */
    public static TournamentSpec parse(File file) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            return TournamentSpec.parseYamlRootObject(new Yaml().load(in));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
