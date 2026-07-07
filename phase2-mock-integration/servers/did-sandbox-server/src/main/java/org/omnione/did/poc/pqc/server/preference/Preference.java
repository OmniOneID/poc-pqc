/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.preference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Preference {
    private static final String FILE_PATH = "prefs.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static Map<String, String> loadPreferences() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                return new HashMap<String, String>();
            }
            return mapper.readValue(file, new TypeReference<Map<String, String>>(){});
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to read preferences file", e);
        }
    }

    private static void savePreferences(Map<String, String> prefs) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), prefs);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to write preferences file", e);
        }
    }

    public static void put(String key, String value) {
        Map<String, String> prefs = Preference.loadPreferences();
        prefs.put(key, value);
        Preference.savePreferences(prefs);
    }

    public static String get(String key) {
        Map<String, String> prefs = Preference.loadPreferences();
        return prefs.get(key);
    }

    public static void remove(String key) {
        Map<String, String> prefs = Preference.loadPreferences();
        prefs.remove(key);
        Preference.savePreferences(prefs);
    }

    public static void clear() {
        Preference.savePreferences(new HashMap<String, String>());
    }
}

