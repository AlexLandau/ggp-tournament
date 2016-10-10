package net.alloyggp.tournament.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.ImmutableList;

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
        return parseYamlFile(file, ImmutableList.<TAdminAction>of());
    }

    /**
     * Loads and parses a tournament specification in YAML format and returns the
     * specification. Also applies administrative actions to the tournament, which
     * can be used to change the tournament structure on-the-fly.
     */
    public static TTournament parseYamlFile(File file, List<TAdminAction> adminActions) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            return TournamentSpec.parseYamlRootObject(new Yaml().load(in))
                    .apply(adminActions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads and parses a tournament specification in YAML format and returns the
     * specification.
     */
    public static TTournament parseYamlString(String yamlString) {
        return parseYamlString(yamlString, ImmutableList.<TAdminAction>of());
    }

    /**
     * Loads and parses a tournament specification in YAML format and returns the
     * specification. Also applies administrative actions to the tournament, which
     * can be used to change the tournament structure on-the-fly.
     */
    public static TTournament parseYamlString(String yamlString, List<TAdminAction> adminActions) {
        return TournamentSpec.parseYamlRootObject(new Yaml().load(yamlString))
                .apply(adminActions);
    }
}
