package org.jex.cli;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading YAML configuration files from plugin resources.
 * Provides simple access to embedded static configuration in plugin JARs.
 */
public class ConfigParser {

    /**
     * Load a YAML configuration file from plugin resources.
     *
     * @param resourcePath Path to the YAML file (e.g., "/config.yaml")
     * @param contextClass Class to use for loading resources (use this.getClass())
     * @return Map containing the parsed YAML configuration, or empty map if file not found
     */
    public static Map<String, Object> load(String resourcePath, Class<?> contextClass) {
        try (InputStream inputStream = contextClass.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.err.println("Warning: Could not find " + resourcePath + " in plugin resources");
                return new HashMap<>();
            }

            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = yaml.load(inputStream);

            return config != null ? config : new HashMap<>();

        } catch (Exception e) {
            System.err.println("Error loading config from resource: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get a string value from the config map with a default fallback.
     *
     * @param config Configuration map
     * @param key Key to look up
     * @param defaultValue Default value if key not found
     * @return String value or default
     */
    public static String getString(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Get an integer value from the config map with a default fallback.
     *
     * @param config Configuration map
     * @param key Key to look up
     * @param defaultValue Default value if key not found
     * @return Integer value or default
     */
    public static int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a boolean value from the config map with a default fallback.
     *
     * @param config Configuration map
     * @param key Key to look up
     * @param defaultValue Default value if key not found
     * @return Boolean value or default
     */
    public static boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}