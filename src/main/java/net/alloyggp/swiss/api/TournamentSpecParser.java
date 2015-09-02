package net.alloyggp.swiss.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

import net.alloyggp.swiss.spec.TournamentSpec;

public class TournamentSpecParser {
    private TournamentSpecParser() {
        //not instantiable
    }

    /**
     * Loads and parses a tournament specification in YAML format and returns the
     * specification.
     */
    public static Tournament parseYamlFile(File file) {
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
    public static Tournament parseYamlString(String yamlString) {
        return TournamentSpec.parseYamlRootObject(new Yaml().load(yamlString));
    }
}
