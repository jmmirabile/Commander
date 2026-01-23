package org.jex.plugins.newplugin;

import org.jex.cli.JexPlugin;
import org.jex.cli.PathConfig;
import org.jex.cli.JexMavenUtil;
import org.jex.cli.ArgumentParser;
import org.apache.commons.cli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewPlugin implements JexPlugin {

    @Override
    public String getName() {
        return "new-plugin";
    }

    @Override
    public void execute(String[] args) {
        // Parse arguments - automatically handles help, empty args, and errors
        CommandLine cmd = ArgumentParser.parse(args, getName(), "/plugins/newplugin/arguments.yaml", this.getClass());
        if (cmd == null) return;  // Help was shown

        // Get plugin name from remaining arguments
        String[] remainingArgs = cmd.getArgs();
        if (remainingArgs.length == 0) {
            System.err.println("Error: Plugin name is required");
            System.err.println("Usage: jex new-plugin <plugin-name> [options]");
            System.exit(1);
        }

        String pluginName = remainingArgs[0];
        String javaPackage = cmd.getOptionValue("package");

        // Check Maven availability
        if (!isMavenAvailable()) {
            System.err.println("\nError: Maven is required for plugin development");
            System.err.println("Please install Maven: https://maven.apache.org/install.html");
            System.err.println("\nVerify installation with: mvn --version");
            System.exit(1);
        }

        // Install Jex to Maven local repo (if not already there)
        installJexToMavenRepo();

        // Generate the plugin
        generate(pluginName, javaPackage);
    }

    private void generate(String pluginName, String javaPackage) {
        if (pluginName == null || pluginName.trim().isEmpty()) {
            System.err.println("Error: JexPlugin name cannot be empty");
            return;
        }

        // Sanitize plugin name (lowercase, hyphens)
        String sanitizedName = pluginName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        String className = toCamelCase(sanitizedName);

        // Determine package name
        String packageName;
        if (javaPackage != null && !javaPackage.trim().isEmpty()) {
            packageName = javaPackage;
        } else {
            packageName = "com.example." + sanitizedName.replace("-", "");
        }

        // Display header
        System.out.println();
        System.out.println("Jex JexPlugin Generator");
        System.out.println("==========================");
        System.out.println();
        System.out.println("This will create a Java Maven project with:");
        System.out.println("  • JexPlugin class implementing the JexPlugin interface");
        System.out.println("  • arguments.yaml for CLI argument definitions");
        System.out.println("  • pom.xml configured for Jex plugins");
        System.out.println("  • README.md with usage instructions");
        System.out.println("  • Complete project structure ready to open in your IDE");
        System.out.println();

        // Prompt for directory
        String targetDir = promptForDirectory();
        if (targetDir == null) {
            System.out.println("JexPlugin generation cancelled.");
            return;
        }

        Path projectPath = Paths.get(targetDir, sanitizedName + "-plugin");

        // Check if directory already exists
        if (Files.exists(projectPath)) {
            System.err.println("Error: Directory already exists: " + projectPath);
            System.err.println("Please choose a different location or plugin name.");
            return;
        }

        try {
            System.out.println("\nCreating " + sanitizedName + "-plugin at: " + projectPath);
            System.out.println();

            // Create project structure
            createProjectStructure(projectPath, packageName);

            // Generate files
            generatePomXml(projectPath, sanitizedName);
            generatePluginClass(projectPath, packageName, className, sanitizedName);
            generateArgumentsYaml(projectPath, sanitizedName);
            generateReadme(projectPath, sanitizedName, className, packageName);
            generateGitignore(projectPath);

            System.out.println();
            System.out.println("Project created successfully!");
            System.out.println();
            System.out.println("Next steps:");
            System.out.println("1. cd " + projectPath);
            System.out.println("2. Open in your IDE (IntelliJ, Eclipse, VS Code)");
            if (javaPackage == null) {
                System.out.println("3. (Optional) Customize package name in pom.xml and " + className + ".java");
                System.out.println("   Current package: " + packageName);
            }
            System.out.println((javaPackage == null ? "4" : "3") + ". Implement your plugin logic in " + className + ".java");
            System.out.println((javaPackage == null ? "5" : "4") + ". Build: mvn clean package");
            System.out.println((javaPackage == null ? "6" : "5") + ". Install plugin (see README.md)");
            System.out.println();
            System.out.println("See README.md for detailed instructions.");

        } catch (IOException e) {
            System.err.println("Error generating plugin project: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String promptForDirectory() {
        String currentDir = System.getProperty("user.dir");

        System.out.println("Where would you like to create the project?");
        System.out.print("(Press Enter for current directory: " + currentDir + ")\n> ");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input = reader.readLine();

            if (input == null || input.trim().isEmpty()) {
                return currentDir;
            }

            // Expand ~ to home directory
            if (input.startsWith("~")) {
                input = System.getProperty("user.home") + input.substring(1);
            }

            Path path = Paths.get(input);
            if (!Files.exists(path)) {
                System.out.print("Directory does not exist. Create it? (y/N): ");
                String confirm = reader.readLine();
                if (confirm != null && confirm.trim().equalsIgnoreCase("y")) {
                    Files.createDirectories(path);
                    return path.toString();
                } else {
                    return null;
                }
            }

            if (!Files.isDirectory(path)) {
                System.err.println("Error: Path is not a directory: " + path);
                return null;
            }

            return path.toString();

        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
            return null;
        }
    }

    private void createProjectStructure(Path projectPath, String packageName) throws IOException {
        String packagePath = packageName.replace(".", "/");

        // Create directories
        Files.createDirectories(projectPath.resolve("src/main/java/" + packagePath));
        Files.createDirectories(projectPath.resolve("src/main/resources"));
        Files.createDirectories(projectPath.resolve("src/test/java/" + packagePath));

        System.out.println("✓ Created project structure");
    }

    private void generatePomXml(Path projectPath, String pluginName) throws IOException {
        String template = loadTemplate("/plugins/newplugin/templates/PomTemplate.xml");
        String artifactId = pluginName + "-plugin";
        String jexVersion = JexMavenUtil.getVersion();

        String content = template
                .replace("${ARTIFACT_ID}", artifactId)
                .replace("${PLUGIN_NAME_CAPITALIZED}", capitalize(pluginName))
                .replace("${JEX_VERSION}", jexVersion);

        writeFile(projectPath.resolve("pom.xml"), content);
        System.out.println("✓ Generated pom.xml");
    }

    private void generatePluginClass(Path projectPath, String packageName, String className, String pluginName) throws IOException {
        String template = loadTemplate("/plugins/newplugin/templates/PluginTemplate.java");
        String packagePath = packageName.replace(".", "/");

        String content = template
                .replace("${PACKAGE_NAME}", packageName)
                .replace("${CLASS_NAME}", className)
                .replace("${PLUGIN_NAME}", pluginName)
                .replace("${CLASS_NAME_CAPITALIZED}", capitalize(pluginName));

        Path javaFile = projectPath.resolve("src/main/java/" + packagePath + "/" + className + ".java");
        writeFile(javaFile, content);
        System.out.println("✓ Generated " + className + ".java");
    }

    private void generateArgumentsYaml(Path projectPath, String pluginName) throws IOException {
        String template = loadTemplate("/plugins/newplugin/templates/ArgumentsTemplate.yaml");

        String content = template
                .replace("${PLUGIN_NAME_CAPITALIZED}", capitalize(pluginName));

        writeFile(projectPath.resolve("src/main/resources/arguments.yaml"), content);
        System.out.println("✓ Generated arguments.yaml");
    }

    private void generateReadme(Path projectPath, String pluginName, String className, String packageName) throws IOException {
        String template = loadTemplate("/plugins/newplugin/templates/ReadmeTemplate.md");
        String artifactId = pluginName + "-plugin";

        String content = template
                .replace("${PLUGIN_NAME_CAPITALIZED}", capitalize(pluginName))
                .replace("${ARTIFACT_ID}", artifactId)
                .replace("${PLUGIN_NAME}", pluginName)
                .replace("${PACKAGE_NAME}", packageName)
                .replace("${CLASS_NAME}", className)
                .replace("${PACKAGE_PATH}", packageName.replace(".", "/"));

        writeFile(projectPath.resolve("README.md"), content);
        System.out.println("✓ Generated README.md");
    }

    private void generateGitignore(Path projectPath) throws IOException {
        String content = """
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
dependency-reduced-pom.xml

# IntelliJ IDEA
.idea/
*.iml
*.iws
*.ipr

# Eclipse
.classpath
.project
.settings/

# VS Code
.vscode/

# macOS
.DS_Store

# Build artifacts
*.jar
*.war
*.class
""";

        writeFile(projectPath.resolve(".gitignore"), content);
        System.out.println("✓ Generated .gitignore");
    }

    private void writeFile(Path filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(content);
        }
    }

    private String toCamelCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private boolean isMavenAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void installJexToMavenRepo() {
        System.out.println("\nChecking Maven local repository...");

        // Check if already installed (avoid re-installing every time)
        if (isJexInMavenRepo()) {
            System.out.println("✓ Jex already in Maven local repository");
            return;
        }

        System.out.println("Installing Jex to Maven local repository...");
        String jexJar = PathConfig.getLibDirectory() + File.separator + "jex.jar";
        String jexVersion = JexMavenUtil.getVersion();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mvn", "install:install-file",
                    "-Dfile=" + jexJar,
                    "-DgroupId=org.jex.cli",
                    "-DartifactId=Jex",
                    "-Dversion=" + jexVersion,
                    "-Dpackaging=jar",
                    "-q"  // Quiet mode
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Consume output to prevent blocking
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Optionally print output for debugging
                    // System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✓ Jex installed to Maven local repository");
            } else {
                System.err.println("⚠ Warning: Failed to install Jex to Maven repository");
                System.err.println("  JexPlugin development may require manual Maven setup");
            }
        } catch (Exception e) {
            System.err.println("⚠ Warning: Could not install to Maven repo: " + e.getMessage());
            System.err.println("  JexPlugin development may require manual Maven setup");
        }
    }

    private boolean isJexInMavenRepo() {
        String jexVersion = JexMavenUtil.getVersion();
        String mavenRepo = System.getProperty("user.home") +
                File.separator + ".m2" + File.separator + "repository" +
                File.separator + "org" + File.separator + "jex" +
                File.separator + "cli" + File.separator + "Jex" +
                File.separator + jexVersion + File.separator + "Jex-" + jexVersion + ".jar";
        return new File(mavenRepo).exists();
    }

    private String loadTemplate(String templatePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream(templatePath)))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
}

