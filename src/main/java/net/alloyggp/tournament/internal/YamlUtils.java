package net.alloyggp.tournament.internal;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class YamlUtils {
    private YamlUtils() {
        //Not instantiable
    }

    public static void validateKeys(Map<String, ?> map, String specType, Set<String> allowedKeys) {
        SetView<String> unrecognizedKeys = Sets.difference(map.keySet(), allowedKeys);
        if (!unrecognizedKeys.isEmpty()) {
            throw new RuntimeException("Did not recognize keys in the " + specType + " configuration: "
                    + unrecognizedKeys);
        }
    }
}
