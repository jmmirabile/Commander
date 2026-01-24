package org.jex.cli;

import org.apache.commons.cli.*;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ArgumentParser {

    /**
     * Parse command-line arguments for a plugin.
     * Uses the standard /arguments.yaml resource path.
     * This method handles the complete argument parsing flow:
     * 1. Loads options from arguments.yaml (including config section)
     * 2. Checks if args is empty -> shows help
     * 3. Checks for -h (short help) or --help (long help) flag -> shows appropriate help
     * 4. Parses arguments and returns CommandLine
     * 5. On parse errors -> shows short help and exits
     *
     * @param args Command-line arguments passed to the plugin
     * @param pluginName Name of the plugin (for help display)
     * @param contextClass Class context for loading resources (use this.getClass())
     * @return CommandLine object with parsed arguments, or null if help was shown
     */
    public static CommandLine parse(String[] args, String pluginName, Class<?> contextClass) {
        return parse(args, pluginName, "/arguments.yaml", contextClass);
    }

    /**
     * Parse command-line arguments for a plugin with custom resource path.
     * This method handles the complete argument parsing flow:
     * 1. Loads options from specified YAML file (including config section)
     * 2. Checks if args is empty -> shows help
     * 3. Checks for -h (short help) or --help (long help) flag -> shows appropriate help
     * 4. Parses arguments and returns CommandLine
     * 5. On parse errors -> shows short help and exits
     *
     * @param args Command-line arguments passed to the plugin
     * @param pluginName Name of the plugin (for help display)
     * @param resourcePath Path to the YAML file (e.g., "/plugins/newplugin/arguments.yaml")
     * @param contextClass Class context for loading resources (use this.getClass())
     * @return CommandLine object with parsed arguments, or null if help was shown
     */
    public static CommandLine parse(String[] args, String pluginName, String resourcePath, Class<?> contextClass) {
        // Load options from specified YAML resource
        OptionsAndConfig optionsAndConfig = loadOptionsAndConfigFromResource(resourcePath, contextClass);
        Options options = optionsAndConfig.options;
        String helpFile = optionsAndConfig.helpTextFile;

        // No arguments provided
        if (args.length == 0) {
            System.err.println("No arguments provided\n");
            showLongHelp(pluginName, options, helpFile, contextClass);
            return null;
        }

        // Check for help flags BEFORE parsing (to avoid required option errors)
        for (String arg : args) {
            if (arg.equals("-h")) {
                showShortHelp(pluginName, options);
                return null;
            }
            if (arg.equals("--help")) {
                showLongHelp(pluginName, options, helpFile, contextClass);
                return null;
            }
        }

        // Parse arguments
        try {
            CommandLineParser parser = new DefaultParser();
            return parser.parse(options, args);
        } catch (ParseException e) {
            // Handle parse errors (unknown options, missing required options, etc.)
            System.err.println("Error: " + e.getMessage() + "\n");
            showShortHelp(pluginName, options);
            System.exit(1);
            return null;
        }
    }

    /**
     * Show short help (quick reference) - always auto-generated from options.
     * Used for -h flag and error messages.
     *
     * @param pluginName Name of the plugin
     * @param options Parsed Options object
     */
    private static void showShortHelp(String pluginName, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jex " + pluginName, options);
    }

    /**
     * Show long help (detailed documentation) - uses custom help file if available.
     * Used for --help flag and when no arguments provided.
     *
     * @param pluginName Name of the plugin
     * @param options Parsed Options object
     * @param helpFile Path to custom help file (relative to plugin resources), or null
     * @param contextClass Class context for loading resources
     */
    private static void showLongHelp(String pluginName, Options options, String helpFile, Class<?> contextClass) {
        if (helpFile != null) {
            // Load and display custom help text file
            String helpContent = loadCustomHelp(helpFile, contextClass);
            if (helpContent != null) {
                System.out.println(helpContent);
                return;
            }
            // Fall through to short help if file couldn't be loaded
            System.err.println("Warning: Could not load help file: " + helpFile);
        }

        // Fallback to short help (auto-generated from options)
        showShortHelp(pluginName, options);
    }

    /**
     * Load custom help text from a resource file.
     *
     * @param helpFile Path to help file (relative to plugin resources, e.g., "/help.txt")
     * @param contextClass Class context for loading resources
     * @return Help text content, or null if file couldn't be loaded
     */
    private static String loadCustomHelp(String helpFile, Class<?> contextClass) {
        try (InputStream inputStream = contextClass.getResourceAsStream(helpFile)) {
            if (inputStream == null) {
                return null;
            }
            return new String(inputStream.readAllBytes());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Container class for options and config data.
     */
    private static class OptionsAndConfig {
        Options options;
        String helpTextFile;

        OptionsAndConfig(Options options, String helpTextFile) {
            this.options = options;
            this.helpTextFile = helpTextFile;
        }
    }

    /**
     * Load options and config from a resource file.
     * Reads both the config section (for help_text_file) and options section.
     *
     * @param resourcePath Path to the YAML file (e.g., "/arguments.yaml")
     * @param contextClass Class context for loading resources
     * @return OptionsAndConfig containing parsed options and help file path
     */
    private static OptionsAndConfig loadOptionsAndConfigFromResource(String resourcePath, Class<?> contextClass) {
        Options options = new Options();
        String helpTextFile = null;

        try (InputStream inputStream = contextClass.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.err.println("Warning: Could not find " + resourcePath + " in plugin resources");
                return new OptionsAndConfig(options, null);
            }

            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = yaml.load(inputStream);

            if (config == null) {
                return new OptionsAndConfig(options, null);
            }

            // Load config section
            if (config.containsKey("config")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configSection = (Map<String, Object>) config.get("config");
                if (configSection != null && configSection.containsKey("help_text_file")) {
                    helpTextFile = (String) configSection.get("help_text_file");
                }
            }

            // Load options section
            if (config.containsKey("options")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> optionsList = (List<Map<String, Object>>) config.get("options");
                options = buildOptionsFromList(optionsList);
            }

        } catch (Exception e) {
            System.err.println("Error loading arguments from resource: " + e.getMessage());
        }

        return new OptionsAndConfig(options, helpTextFile);
    }

    public static Options loadOptionsFromYaml(String yamlPath) {
        Options options = new Options();
        Yaml yaml = new Yaml();

        try (InputStream inputStream = Files.newInputStream(Paths.get(yamlPath))) {
            Map<String, Object> config = yaml.load(inputStream);

            if (config == null) {
                return options;
            }

            // Load options section
            if (!config.containsKey("options")) {
                return options;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> optionsList = (List<Map<String, Object>>) config.get("options");
            return buildOptionsFromList(optionsList);

        } catch (Exception e) {
            System.err.println("Error loading arguments from YAML: " + e.getMessage());
        }

        return options;
    }

    /**
     * Load CLI options from a bundled resource file (arguments.yaml).
     * Package-private convenience method for internal org.jex.cli classes only.
     * External plugins should use loadOptionsFromResource(String, Class) instead.
     */
    static Options loadOptionsFromResource(String resourcePath) {
        return loadOptionsFromResource(resourcePath, ArgumentParser.class);
    }

    /**
     * Load CLI options from a bundled resource file using a specific class context.
     * This is the public API for loading plugin resources.
     * Note: This method only loads options, not config. Use parse() for complete argument handling.
     *
     * @param resourcePath Path to the YAML file (e.g., "/arguments.yaml")
     * @param contextClass Class to use for loading resources (use this.getClass() or YourPlugin.class)
     * @return Parsed Options object
     * @deprecated Use parse(String[], String, Class) instead for complete argument handling
     */
    public static Options loadOptionsFromResource(String resourcePath, Class<?> contextClass) {
        return loadOptionsAndConfigFromResource(resourcePath, contextClass).options;
    }

    /**
     * Build Options from a list of option configurations.
     * Extracts common logic for parsing option configurations.
     *
     * @param optionsList List of option configuration maps from YAML
     * @return Options object with all parsed options
     */
    private static Options buildOptionsFromList(List<Map<String, Object>> optionsList) {
        Options options = new Options();

        for (Map<String, Object> optionConfig : optionsList) {
            Option.Builder builder = Option.builder();

            if (optionConfig.containsKey("short")) {
                builder.option((String) optionConfig.get("short"));
            }

            // Use 'name' as the long option (primary identifier)
            // Backward compatibility: fall back to 'long' if 'name' not present
            if (optionConfig.containsKey("name")) {
                builder.longOpt((String) optionConfig.get("name"));
            } else if (optionConfig.containsKey("long")) {
                // Legacy: still support old 'long' field for backward compatibility
                builder.longOpt((String) optionConfig.get("long"));
            }

            if (optionConfig.containsKey("description")) {
                builder.desc((String) optionConfig.get("description"));
            }

            if (optionConfig.containsKey("hasArg")) {
                boolean hasArg = (Boolean) optionConfig.get("hasArg");
                builder.hasArg(hasArg);
            }

            // Check for argHelpLabel (preferred) or argName (legacy) for help text display
            if (optionConfig.containsKey("argHelpLabel")) {
                builder.argName((String) optionConfig.get("argHelpLabel"));
            } else if (optionConfig.containsKey("argName")) {
                // Backward compatibility: still support old argName property
                builder.argName((String) optionConfig.get("argName"));
            }

            if (optionConfig.containsKey("required")) {
                builder.required((Boolean) optionConfig.get("required"));
            }

            options.addOption(builder.build());
        }

        return options;
    }
}