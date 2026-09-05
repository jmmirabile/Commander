package org.jex.cli;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Install {

    private static final String REQUIRED_MAVEN_VERSION = "3.6";

    private static String getJarPath() {
        try {
            return new File(Install.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (URISyntaxException e) {
            System.err.println("Error locating JAR file: " + e.getMessage());
            return null;
        }
    }

    public static void run() {
        // Maven is required to build and install plugins (new-plugin, --install-plugin workflow)
        String mavenVersion = JexUtil.getMavenVersionOrNull();

        if (mavenVersion == null) {
            System.err.println("Error: Maven is required to install Jex.");
            System.err.println("Please install Maven: https://maven.apache.org/install.html");
            System.err.println("\nVerify installation with: mvn --version");
            System.exit(1);
            return;
        }

        if (!JexUtil.isVersionAtLeast(mavenVersion, REQUIRED_MAVEN_VERSION)) {
            System.err.println("Error: Jex requires Maven " + REQUIRED_MAVEN_VERSION + "+ (detected " + mavenVersion + ").");
            System.err.println("Please upgrade Maven: https://maven.apache.org/install.html");
            System.exit(1);
            return;
        }

        Path configPath = Paths.get(PathConfig.getConfigDirectory());
        Path pluginsPath = Paths.get(PathConfig.getPluginsDirectory());
        Path pluginYamlPath = Paths.get(PathConfig.getPluginYamlPath());
        Path argumentsYamlPath = Paths.get(PathConfig.getArgumentsYamlPath());

        try {
            // Create configuration directory
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                System.out.println("Created configuration directory: " + configPath);
            } else {
                System.out.println("Configuration directory already exists: " + configPath);
            }

            // Create plugins subdirectory
            if (!Files.exists(pluginsPath)) {
                Files.createDirectories(pluginsPath);
                System.out.println("Created plugins directory: " + pluginsPath);
            } else {
                System.out.println("Plugins directory already exists: " + pluginsPath);
            }

            // Create default plugin.yaml
            if (!Files.exists(pluginYamlPath)) {
                createDefaultPluginYaml(pluginYamlPath.toString());
                System.out.println("Created default plugin.yaml: " + pluginYamlPath);
            } else {
                System.out.println("plugin.yaml already exists: " + pluginYamlPath);
            }

            // Create default arguments.yaml for Jex
            if (!Files.exists(argumentsYamlPath)) {
                createDefaultArgumentsYaml(argumentsYamlPath.toString());
                System.out.println("Created default arguments.yaml: " + argumentsYamlPath);
            } else {
                System.out.println("arguments.yaml already exists: " + argumentsYamlPath);
            }

            // Install Jex JAR
            installJexJar();

            // Refresh Jex in the local Maven repository so plugin projects (new-plugin)
            // always compile against the version that was just installed
            installJexToMavenRepo();

            // Install wrapper script
            installWrapperScript();

            // Install bundled plugins
            //installBundledPlugins(pluginsPath);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("Jex installation completed successfully!");
            System.out.println("=".repeat(60));
            System.out.println("\nConfiguration directory: " + configPath);
            System.out.println("Installation directory: " + PathConfig.getLibDirectory());
            System.out.println("\nYou can now install plugins by:");
            System.out.println("1. Copying plugin JAR files to: " + pluginsPath);
            System.out.println("2. Registering them in: " + pluginYamlPath);

        } catch (IOException e) {
            System.err.println("Error during installation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void installJexJar() throws IOException {
        String jarPath = getJarPath();
        if (jarPath == null) {
            System.err.println("Warning: Could not locate JAR file. Skipping JAR installation.");
            return;
        }

        String libDir = PathConfig.getLibDirectory();
        Path libPath = Paths.get(libDir);
        Path targetJar = Paths.get(libDir, "jex.jar");

        // Create lib directory if it doesn't exist
        if (!Files.exists(libPath)) {
            Files.createDirectories(libPath);
            System.out.println("Created library directory: " + libPath);
        }

        // Copy JAR to lib directory
        Files.copy(Paths.get(jarPath), targetJar, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Installed Jex JAR to: " + targetJar);
    }

    private static void installJexToMavenRepo() {
        String jexJar = Paths.get(PathConfig.getLibDirectory(), "jex.jar").toString();
        String jexVersion = JexMavenUtil.getVersion();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mvn", "install:install-file",
                    "-Dfile=" + jexJar,
                    "-DgroupId=org.jex.cli",
                    "-DartifactId=Jex",
                    "-Dversion=" + jexVersion,
                    "-Dpackaging=jar",
                    "-q"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Consume output to prevent blocking
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // discard
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Installed Jex to Maven local repository (org.jex.cli:Jex:" + jexVersion + ")");
            } else {
                System.err.println("Warning: Failed to install Jex to Maven local repository");
                System.err.println("  Plugin development (new-plugin) may not build correctly until this is resolved");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not install Jex to Maven local repository: " + e.getMessage());
        }
    }

    private static void installWrapperScript() throws IOException {
        Path binPath = Paths.get(PathConfig.getBinDirectory());

        // Create bin directory if it doesn't exist
        if (!Files.exists(binPath)) {
            Files.createDirectories(binPath);
            System.out.println("Created bin directory: " + binPath);
        }

        if (PathConfig.isWindows()) {
            installWindowsScript(binPath);
        } else {
            installUnixScript(binPath);
        }
    }

    private static void installUnixScript(Path binPath) throws IOException {
        Path scriptPath = binPath.resolve("jex");

        // Read the script template from resources
        try (InputStream is = Install.class.getResourceAsStream("/jex.sh");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             FileWriter writer = new FileWriter(scriptPath.toFile())) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line + "\n");
            }
        }

        // Make executable
        scriptPath.toFile().setExecutable(true, false);
        System.out.println("Installed wrapper script to: " + scriptPath);

        // Check if bin directory is in PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || !pathEnv.contains(binPath.toString())) {
            System.out.println("\n" + "!".repeat(60));
            System.out.println("IMPORTANT: Add the following to your PATH:");
            System.out.println("export PATH=\"$PATH:" + binPath + "\"");
            System.out.println("\nAdd this line to your ~/.bashrc, ~/.zshrc, or ~/.profile");
            System.out.println("!".repeat(60));
        } else {
            System.out.println("✓ Bin directory is already in PATH");
            System.out.println("\nYou can now run Jex using: jex");
        }
    }

    private static void installBundledPlugins(Path pluginsPath) throws IOException {
        // Install new-plugin from resources
        InputStream pluginStream = Install.class.getResourceAsStream("/plugins/new-plugin.jar");
        if (pluginStream != null) {
            Path targetPlugin = pluginsPath.resolve("new-plugin.jar");
            Files.copy(pluginStream, targetPlugin, StandardCopyOption.REPLACE_EXISTING);
            pluginStream.close();
            System.out.println("Installed bundled plugin: new-plugin.jar");
        } else {
            System.out.println("Note: new-plugin.jar not found in resources (will be available after full rebuild)");
        }
    }

    private static void installWindowsScript(Path binPath) throws IOException {
        Path scriptPath = binPath.resolve("jex.bat");

        // Read the script template from resources
        try (InputStream is = Install.class.getResourceAsStream("/jex.bat");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             FileWriter writer = new FileWriter(scriptPath.toFile())) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line + "\r\n"); // Windows line endings
            }
        }

        System.out.println("Installed wrapper script to: " + scriptPath);

        // Check if bin directory is in PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || !pathEnv.contains(binPath.toString())) {
            System.out.println("\n" + "!".repeat(60));
            System.out.println("IMPORTANT: Add the following to your PATH:");
            System.out.println(binPath);
            System.out.println("\nInstructions:");
            System.out.println("1. Open System Properties > Environment Variables");
            System.out.println("2. Edit the PATH variable for your user");
            System.out.println("3. Add: " + binPath);
            System.out.println("!".repeat(60));
        } else {
            System.out.println("✓ Bin directory is already in PATH");
            System.out.println("\nYou can now run Jex using: jex");
        }
    }

    private static void createDefaultPluginYaml(String path) throws IOException {
        String defaultContent = "# Jex JexPlugin Registry\n" +
                "# Bundled plugins (installed with Jex):\n" +
                "\n" +
                "# Add your custom plugins below in the following format:\n" +
                "#\n" +
                "# plugin-name:\n" +
                "#   jar: plugin-file.jar\n" +
                "#   class: com.example.PluginClassName\n" +
                "#   version: 1.0.0\n" +
                "#   description: \"JexPlugin description\"\n";

        try (FileWriter writer = new FileWriter(path)) {
            writer.write(defaultContent);
        }
    }

    private static void createDefaultArgumentsYaml(String path) throws IOException {
        String defaultContent = "# Jex CLI Arguments\n" +
                "# This file defines Jex's built-in command-line arguments\n" +
                "\n" +
                "options:\n" +
                "  - name: install\n" +
                "    long: install\n" +
                "    description: \"Install Jex (create directories, install JAR, wrapper scripts)\"\n" +
                "    required: false\n" +
                "    hasArg: false\n" +
                "\n" +
                "  - name: list\n" +
                "    short: l\n" +
                "    long: list\n" +
                "    description: \"List all installed plugins\"\n" +
                "    required: false\n" +
                "    hasArg: false\n" +
                "\n" +
                "  - name: help\n" +
                "    short: h\n" +
                "    long: help\n" +
                "    description: \"Display help information\"\n" +
                "    required: false\n" +
                "    hasArg: false\n" +
                "\n" +
                "  - name: version\n" +
                "    short: v\n" +
                "    long: version\n" +
                "    description: \"Display version\"\n" +
                "    required: false\n" +
                "    hasArg: false\n"
                ;

        try (FileWriter writer = new FileWriter(path)) {
            writer.write(defaultContent);
        }
    }
}