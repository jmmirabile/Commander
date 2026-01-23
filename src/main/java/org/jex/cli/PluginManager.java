package org.jex.cli;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages plugin lifecycle: install, update, uninstall.
 */
public class PluginManager {
    private final Path pluginDir;
    private final Path registryFile;

    public PluginManager() {
        this.pluginDir = Paths.get(PathConfig.getPluginsDirectory());
        this.registryFile = Paths.get(PathConfig.getConfigDirectory(), "plugin.yaml");
    }

    /**
     * Install a new plugin.
     */
    public void installPlugin(String name, String jarPath) throws IOException {
        installOrUpdatePlugin(name, jarPath, false);
    }

    /**
     * Update an existing plugin.
     */
    public void updatePlugin(String name, String jarPath) throws IOException {
        installOrUpdatePlugin(name, jarPath, true);
    }

    /**
     * Uninstall a plugin.
     */
    public void uninstallPlugin(String name) throws IOException {
        // Load registry
        Map<String, Map<String, String>> registry = loadRegistry();
        Map<String, String> pluginInfo = registry.get(name);

        if (pluginInfo == null) {
            throw new IllegalStateException("Plugin not found: " + name);
        }

        // Delete JAR
        String jarFile = pluginInfo.get("jar");
        Path jarPath = pluginDir.resolve(jarFile);
        Files.deleteIfExists(jarPath);

        // Remove from registry
        removeFromRegistry(name);

        System.out.println("✓ Uninstalled plugin: " + name);
    }

    // PRIVATE HELPERS

    /**
     * Shared logic for install and update.
     */
    private void installOrUpdatePlugin(String name, String jarPath, boolean mustExist) throws IOException {
        // Validate JAR exists
        Path sourceJar = Paths.get(jarPath);
        if (!Files.exists(sourceJar)) {
            throw new FileNotFoundException("JAR file not found: " + jarPath);
        }

        // Check existence
        Map<String, Map<String, String>> registry = loadRegistry();
        boolean exists = registry.containsKey(name);

        if (mustExist && !exists) {
            throw new IllegalStateException("Plugin not found: " + name);
        }
        if (!mustExist && exists) {
            throw new IllegalStateException("Plugin already installed: " + name);
        }

        // Extract metadata
        PluginMetadata metadata = extractMetadata(name, sourceJar);

        // Copy JAR
        Path destJar = pluginDir.resolve(metadata.getJarFile());
        Files.copy(sourceJar, destJar, StandardCopyOption.REPLACE_EXISTING);

        // Update registry
        addToRegistry(metadata);

        System.out.println("✓ " + (mustExist ? "Updated" : "Installed") + " plugin: " + name);
    }

    /**
     * Extract plugin metadata from JAR by loading it.
     */
    private PluginMetadata extractMetadata(String name, Path jarPath) throws IOException {
        try {
            // Find and instantiate plugin from JAR
            JexPlugin plugin = JexUtil.findPluginInJar(jarPath);

            if (plugin == null) {
                throw new IOException("No JexPlugin implementation found in JAR");
            }

            String jarFile = jarPath.getFileName().toString();
            String className = plugin.getClass().getName();
            String version = "1.0.0"; // Default, could extract from manifest
            String description = "A Jex plugin";

            return new PluginMetadata(name, jarFile, className, version, description);

        } catch (Exception e) {
            throw new IOException("Invalid plugin JAR: " + e.getMessage(), e);
        }
    }

    /**
     * Load plugin registry from YAML file.
     */
    private Map<String, Map<String, String>> loadRegistry() throws IOException {
        if (!Files.exists(registryFile)) {
            return new LinkedHashMap<>();
        }

        Yaml yaml = new Yaml();
        try (InputStream input = Files.newInputStream(registryFile)) {
            Map<String, Map<String, String>> registry = yaml.load(input);
            return registry != null ? registry : new LinkedHashMap<>();
        }
    }

    /**
     * Save plugin registry to YAML file.
     */
    private void saveRegistry(Map<String, Map<String, String>> registry) throws IOException {
        org.yaml.snakeyaml.DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
        options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        try (Writer writer = Files.newBufferedWriter(registryFile)) {
            yaml.dump(registry, writer);
        }
    }

    /**
     * Add or update plugin entry in registry.
     */
    private void addToRegistry(PluginMetadata metadata) throws IOException {
        Map<String, Map<String, String>> registry = loadRegistry();

        Map<String, String> pluginEntry = new LinkedHashMap<>();
        pluginEntry.put("jar", metadata.getJarFile());
        pluginEntry.put("class", metadata.getClassName());
        pluginEntry.put("version", metadata.getVersion());
        pluginEntry.put("description", metadata.getDescription());

        registry.put(metadata.getName(), pluginEntry);
        saveRegistry(registry);
    }

    /**
     * Remove plugin entry from registry.
     */
    private void removeFromRegistry(String name) throws IOException {
        Map<String, Map<String, String>> registry = loadRegistry();
        registry.remove(name);
        saveRegistry(registry);
    }
}