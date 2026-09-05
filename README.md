# Jex

A plugin-based CLI framework for Java that allows developers to create command-line applications as plugins. Uses Maven to
create each plugin project.

## Overview
Jex, short for Java Executor. It is a Java-based command line, plugin framework. Plugins are self-contained
JAR files that are added to the Jex plugins directory and registered in a plugin.yaml file.

Features:
 - Quickly create a Java maven based project that can be launched from the cmd line.
 - Quick project setup using maven. Just open as a maven project in a Java IDE.
 - Edit arguments.yaml file to add arguments, then add processing for each argument in the execute method.
 - Arguments.yaml and built-in parsing makes adding arguments easy.
 - Developers can create a new plugin by running `jex new-plugin [name]`

**Key Features:**
- Self-installing fat JAR with OS-specific wrapper scripts
- YAML-based configuration for arguments and plugins
- **Arguments** are defined in a yaml file, not code! (Still have to write code to process your arguments)
- Dynamic plugin loading from JAR files. Just type "jex --list" to see plugins or "jex <plugin-name>" to run a plugin.
- Built-in help and plugin management commands
- Minimal setup - download and run "java -jar <jex jar file> --install" (Maven 3.6+ is required and is checked automatically)
- Update Jex without overwriting the installed plugins.
- Runs on Linux, MacOS and Windows

## Quick Start

### Requirements

   * Java 21 or later
   * Maven 3.6+

### Installation

1. **Download** the `Jex-<version>.jar` file (fat JAR with all dependencies) from the Releases link.

2. **Run install** to set up Jex:
   ```bash
   java -jar Jex-<version>.jar --install
   ```
   After installation completes, the following has been created:
   * Configuration directories (OS-specific locations)
   * Wrapper script (`jex` on Unix/Linux/macOS, `jex.bat` on Windows)
   * Default `plugin.yaml` registry file (empty template for developer plugins)

3. **Add to PATH** (if needed):
   - **Linux/macOS**: The installer will tell you if `~/.local/bin` needs to be added to PATH
   - **Windows**: Add `%LOCALAPPDATA%\Programs\Jex` to your PATH environment variable

4. **Verify installation**:
   ```bash
   jex --help
   jex -v     
   jex new-plugin [name] # Create a new plugin project with Maven build system.
   ```

### Installation Locations

Jex installs to OS-specific locations:

**Linux:**
- JAR: `~/.local/lib/jex/jex.jar`
- Script: `~/.local/bin/jex`
- Config: `~/.config/Jex/`
- Plugins: `~/.config/Jex/plugins/`

**macOS:**
- JAR: `~/Library/Application Support/Jex/jex.jar`
- Script: `~/.local/bin/jex`
- Config: `~/Library/Application Support/Jex/`
- Plugins: `~/Library/Application Support/Jex/plugins/`

**Windows:**
- JAR: `%LOCALAPPDATA%\Programs\Jex\jex.jar`
- Script: `%LOCALAPPDATA%\Programs\Jex\jex.bat`
- Config: `%APPDATA%\Jex\`
- Plugins: `%APPDATA%\Jex\plugins\`

## Architecture

### Minimal Core Design

Jex follows a **minimal core + plugin architecture**. The core has only 3 built-in commands:
- `--install`: Bootstrap Jex installation (create directories, install JAR, wrapper scripts)
- `--list` / `-l`: List installed plugins
- `--help` / `-h`: Display help information

**Everything else is a plugin**, including the plugin generator. This design keeps the core small, fast, and focused while enabling unlimited extensibility through plugins.

### Execution Flow

1. User runs: `jex [options]` or `jex <plugin-name> [plugin-args...]`
2. If a built-in command is detected (e.g., `--install`, `--help`, `--list`), execute it and exit
3. Otherwise, treat the first argument as a plugin name
4. Look up the plugin in `plugin.yaml` registry
5. Load the plugin JAR dynamically using URLClassLoader
6. Instantiate the plugin class and execute it with remaining arguments
7. The plugin handles its own argument parsing using Apache Commons CLI 

### Configuration Directory Structure

Jex stores its configuration in OS-specific locations:

- **Windows**: `%APPDATA%/Jex`
- **Linux**: `~/.config/Jex`
- **macOS**: `~/Library/Application Support/Jex`

```
<platform config directory>/Jex/
├── plugin.yaml           # Registry of installed plugins
├── arguments.yaml        # Jex's own CLI arguments
└── plugins/
    ├── my-plugin.jar     # Self-contained plugin JAR
    ├── another-plugin.jar 
    └── third-plugin.jar
```

## Plugin System

You can create a plugin by running "jex new-plugin [name]" or "jex new-plugin [name] -p [java package]". This will ask to
create the plugin folder in the current directory or let you type a new path for the project. Once the project
directory is created, open that project in your JAVA IDE as a Maven project. Everything will be ready, including a
main class for your plugin with the name you provided the command.

If you've migrated a standalone test class with a static main method with arguments to Jex, do the following:
1. Open that file and copy the main method and other methods to this new class.
2. Modify or copy the package structure and add any utility classes to package.
3. Fix any imports that need fixing.
4. Add any libraries you need to the pom.xml file. Your IDE may have a Maven library/repo explorer.
5. Add the arguments to arguments.yaml.
6. Modify the main method to parse the arguments you added to arguments.yaml.
7. Build it, go to a terminal, navigate to the project base directory, type "mvn clean package"
8. Change directory to target, type "jex --install-plugin [command name] --jar [plugin jar name]"
9. Now type, "jex -l" to list the plugins to see if your plugin was installed.

### Plugin JAR Structure

Each plugin is a self-contained JAR file containing:

```
my-plugin.jar
├── com/example/MyPlugin.class    # Implements JexPlugin interface
├── arguments.yaml                 # CLI argument definitions
└── [other plugin classes/resources]
```

### Plugin Interface

All plugins must implement the `JexPlugin` interface:

```java
package org.jex.cli;

public interface JexPlugin {
    String getName();
    void execute(String[] args);
}
```

### Plugin Registry (plugin.yaml)

The `plugin.yaml` file maps plugin names to their JAR files and main classes:

```yaml
my-plugin:
  jar: my-plugin.jar
  class: com.example.MyPlugin
  version: 1.0.0
  description: "Does something useful"

another-plugin:
  jar: another-plugin.jar
  class: com.example.AnotherPlugin
  version: 2.1.0
  description: "Another useful tool"
```

### Plugin Arguments (arguments.yaml)

Each plugin defines its command-line arguments in an `arguments.yaml` file bundled in the JAR:

```yaml
options:
  - name: input
    short: i
    long: input-file
    description: "Input file path"
    required: true
    hasArg: true
  - name: output
    short: o
    long: output-file
    description: "Output file path"
    required: false
    hasArg: true
  - name: verbose
    short: v
    long: verbose
    description: "Enable verbose output"
    required: false
    hasArg: false
```

### Plugin Configuration (config.yaml)

In addition to CLI arguments, plugins can bundle a `config.yaml` file for static configuration - default values,
constants, or feature flags that don't change per-invocation:

```yaml
greeting: "Hello from Jex!"
version: "1.0.0"
debug: false
```

Load it in your plugin with `ConfigParser`:

```java
Map<String, Object> config = ConfigParser.load("/config.yaml", this.getClass());
String greeting = ConfigParser.getString(config, "greeting", "Hello!");
int retries = ConfigParser.getInt(config, "retries", 3);
boolean debug = ConfigParser.getBoolean(config, "debug", false);
```

`config.yaml` is for defaults baked into the JAR; `arguments.yaml`/CLI flags are for what the user overrides at
runtime. `ConfigParser` doesn't merge the two automatically - if you want a CLI flag to override a `config.yaml`
default, check `cmd.hasOption(...)` first and fall back to the config value otherwise.

## Jex Built-in Commands

### Help

Display usage information and available commands:

```bash
jex --help
jex -h
jex        # No arguments also shows help
```

### Install

Initialize and install Jex:

```bash
jex --install
java -jar Jex-<version>.jar --install  # First-time installation
```

This command:
- **Verifies** Maven 3.6+ is installed (required for plugin development) - exits with install/upgrade instructions if missing or too old
- Creates configuration directory (OS-specific location)
- Creates `plugins/` subdirectory
- Generates empty `plugin.yaml` registry template
- Generates default `arguments.yaml` for Jex
- **Installs** `jex.jar` to the lib directory
- **Refreshes** the Jex artifact in the local Maven repository (`~/.m2/...`) so plugin projects always build against the version just installed
- **Creates** and installs OS-specific wrapper script (`jex` or `jex.bat`)
- **Makes** the script executable (Unix/Linux/macOS)
- **Checks** if bin directory is in PATH and provides instructions if needed

### List Plugins

List all installed plugins:

```bash
jex --list
jex -l
```

Shows all registered plugins from `plugin.yaml`. Note: Internal plugins like `new-plugin` are auto-discovered and don't appear in this list.

### Plugin Management

Jex provides commands to manage plugin installation, updates, and removal.

#### Install Plugin

Install a new plugin:

```bash
jex --install-plugin <name> --jar <jar-file>
```

Example:
```bash
jex --install-plugin my-tool --jar target/my-tool-plugin.jar
```

This command:
- Copies the JAR to the plugins directory
- Scans the JAR for a JexPlugin implementation
- Extracts metadata (class name, version, description)
- Registers the plugin in `plugin.yaml`
- Validates the plugin loads correctly

#### Update Plugin

Update an existing plugin:

```bash
jex --update-plugin <name> --jar <jar-file>
```

Example:
```bash
jex --update-plugin my-tool --jar target/my-tool-plugin.jar
```

This command:
- Replaces the existing JAR in the plugins directory
- Updates the plugin entry in `plugin.yaml`
- Validates the updated plugin loads correctly

#### Uninstall Plugin

Remove a plugin:

```bash
jex --uninstall-plugin <name>
```

Example:
```bash
jex --uninstall-plugin my-tool
```

This command:
- Removes the JAR from the plugins directory
- Removes the plugin entry from `plugin.yaml`

## Internal Plugins

### new-plugin - Plugin Generator ✅

**Note:** `new-plugin` is an internal plugin (bundled in Jex.jar) and is automatically discovered. You don't need to install or register it.

Create new plugin projects with complete Maven structure:

```bash
jex new-plugin <plugin-name>
jex new-plugin <plugin-name> --package <package-name>
```

**Examples:**
```bash
# Create plugin with default package
jex new-plugin my-tool

# Create plugin with custom package
jex new-plugin my-tool --package com.mycompany.tools
```

This creates a complete Maven project with:
- Plugin class skeleton implementing the `JexPlugin` interface
- Sample `arguments.yaml` for CLI arguments
- Sample `config.yaml` for static plugin configuration
- Maven `pom.xml` configured for Jex plugins
- `.gitignore` file
- README with build and installation instructions

The generated project is ready to open in your IDE and start developing.

## Usage

### Using Jex

```bash
# Show help
jex --help

# List installed plugins
jex --list

# Install Jex
java -jar Jex-<version>.jar --install

# Create a new plugin project
jex new-plugin my-awesome-tool --package com.example

# Run a plugin
jex <plugin-name> [args...]
```

## Developing Plugins

### Plugin Development Steps

1. **Generate a plugin template**:
   ```bash
   jex new-plugin my-plugin --package com.example
   ```

2. **Implement the `JexPlugin` interface** in your main class:
   ```java
   package com.example;

import org.jex.cli.JexPlugin;

   public class MyPlugin implements JexPlugin {
       @Override
       public String getName() {
           return "my-plugin";
       }

       @Override
       public void execute(String[] args) {
           // Parse arguments - automatically handles help, errors, and validation
           CommandLine cmd = ArgumentParser.parse(args, getName(), this.getClass());
           if (cmd == null) return;  // Help was shown

           // Access arguments using:
           if (cmd.hasOption("input")) {
               String inputFile = cmd.getOptionValue("input");
               // Process input file
           }
       }
   }
   ```

   **Note:** The new `ArgumentParser.parse()` method automatically handles:
   - Empty arguments → Shows help
   - `-h` flag → Shows quick reference (auto-generated from options)
   - `--help` flag → Shows detailed help (custom help.txt if available)
   - Parse errors → Shows error message and quick reference
   - Validation → Checks required options and returns CommandLine

3. **Define CLI arguments** in `src/main/resources/arguments.yaml`:
   ```yaml
   # Optional: Use custom help file for detailed documentation
   # config:
   #   help_text_file: "/help.txt"

   options:
     - name: help        # Primary identifier - use in code as cmd.hasOption("help")
       short: h          # Short option: -h
       description: "Display help information"
       required: false
       hasArg: false
     - name: input       # Use in code as cmd.getOptionValue("input")
       short: i          # Short option: -i, long option: --input
       description: "Input file path"
       required: true
       hasArg: true
       argHelpLabel: "file"
   ```

4. **(Optional) Create custom help file** at `src/main/resources/help.txt`:
   - Uncomment `config.help_text_file` in `arguments.yaml`
   - Add detailed documentation, examples, and usage instructions
   - Shown when user runs `jex <plugin> --help`
   - If not specified, help is auto-generated from options

5. **(Optional) Add static configuration** in `src/main/resources/config.yaml` for defaults, constants, or feature
   flags, and load it with `ConfigParser.load("/config.yaml", this.getClass())` (see [Plugin Configuration](#plugin-configuration-configyaml) above)

6. **Build the plugin JAR** with `arguments.yaml` (and `config.yaml`, if used) included as resources

7. **Install the plugin**:
   ```bash
   jex --install-plugin my-plugin --jar target/my-plugin.jar
   ```

### Plugin Template Structure

```
my-plugin/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/MyPlugin.java
│       └── resources/
│           ├── arguments.yaml
│           └── config.yaml
├── pom.xml
└── README.md
```

## Deployment Patterns

Jex plugins can be deployed in two ways depending on your use case and target audience.

### Pattern 1: Traditional Jex Plugin (Default)

Users install Jex and run your plugin through the `jex` command.

**Use this when:**
- Building internal tools or developer utilities
- Users are comfortable with command-line tools
- You want to leverage Jex's plugin management (`--install-plugin`, `--update-plugin`)
- You're building a suite of related plugins

**Example: HelloWorld Plugin**

Create the plugin:
```bash
jex new-plugin hello-world --package com.example
```

**Plugin code (`HelloWorld.java`):**
```java
public class HelloWorld implements JexPlugin {
    @Override
    public String getName() {
        return "hello-world";
    }

    @Override
    public void execute(String[] args) {
        CommandLine cmd = ArgumentParser.parse(args, getName(), this.getClass());
        if (cmd == null) return;

        String name = cmd.getOptionValue("name", "World");
        System.out.println("Hello, " + name + "!");
    }
}
```

**Arguments (`arguments.yaml`):**
```yaml
options:
  - name: name
    short: n
    description: "Name to greet"
    required: false
    hasArg: true
    argHelpLabel: "name"
```

**Build and install:**
```bash
mvn clean package
jex --install-plugin hello-world --jar target/hello-world-plugin.jar
```

**Users run it:**
```bash
jex hello-world --name Alice
# Output: Hello, Alice!
```

**Dependencies in `pom.xml`:**
```xml
<dependency>
    <groupId>org.jex.cli</groupId>
    <artifactId>Jex</artifactId>
    <!-- Correct version number on existing projects when upgrading Jex to new version -->
    <version>1.0.5</version>
    <scope>provided</scope>  <!-- Jex provides these at runtime -->
</dependency>
```

---

### Pattern 2: Standalone Application

Bundle Jex as a library inside your application JAR. Users run your tool directly without installing Jex.

**Use this when:**
- Distributing to non-technical users or customers
- Building single-purpose utilities
- You want a simple distribution model (single JAR download)
- You're using code obfuscation tools
- Users shouldn't know or care about Jex

**Example: HelloWorld Standalone App**

Start with the same plugin template:
```bash
jex new-plugin hello-world --package com.example
```

**1. Modify `pom.xml` - Change Jex dependency scope:**
```xml
<dependency>
    <groupId>org.jex.cli</groupId>
    <artifactId>Jex</artifactId>
    <!-- Correct version number on existing projects when upgrading Jex to new version -->
    <version>1.0.5</version>
    <!-- Remove <scope>provided</scope> to bundle Jex in JAR -->
</dependency>
```

**2. Add Main-Class to Maven Shade Plugin:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <createDependencyReducedPom>false</createDependencyReducedPom>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.example.HelloWorldMain</mainClass>
                    </transformer>
                </transformers>
                <filters>
                    <filter>
                        <artifact>*:*</artifact>
                        <excludes>
                            <exclude>META-INF/*.SF</exclude>
                            <exclude>META-INF/*.DSA</exclude>
                            <exclude>META-INF/*.RSA</exclude>
                        </excludes>
                    </filter>
                </filters>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**3. Create a Main class (`HelloWorldMain.java`):**
```java
package com.example;

public class HelloWorldMain {
    public static void main(String[] args) {
        HelloWorld plugin = new HelloWorld();
        plugin.execute(args);
    }
}
```

**4. Keep the same plugin code and arguments.yaml**

No changes needed to `HelloWorld.java` or `arguments.yaml` - they work identically in both patterns.

**Build:**
```bash
mvn clean package
```

**Users run it directly (no Jex installation needed):**
```bash
java -jar hello-world-plugin.jar --name Alice
# Output: Hello, Alice!
```

**Distribution:**
- Rename JAR: `cp target/hello-world-plugin.jar hello-world.jar`
- Distribute single JAR file
- Users don't need to install Jex or know it exists

---

### Comparison

| Feature | Traditional Plugin | Standalone App |
|---------|-------------------|----------------|
| **Distribution** | Plugin JAR + Jex installation | Single fat JAR |
| **User runs** | `jex hello-world --name Alice` | `java -jar hello-world.jar --name Alice` |
| **Jex dependency** | `<scope>provided</scope>` | No scope (bundled) |
| **Main class** | Not needed | Required |
| **JAR size** | Small (~10KB plugin only) | Larger (~5MB with Jex + deps) |
| **Updates** | `jex --update-plugin` | Re-download JAR |
| **Plugin management** | Yes | No |
| **Use Jex features** | ArgumentParser, ConfigParser | ArgumentParser, ConfigParser |
| **Best for** | Developer tools, internal use | Customer-facing, external distribution |

---

### Key Takeaway

Both patterns use the **same plugin code**. The only differences are:
1. How you configure `pom.xml` (dependency scope + main class)
2. How users run it (`jex plugin-name` vs `java -jar app.jar`)

Choose the pattern that fits your distribution model and target audience.

## Building Jex from Source

### Prerequisites

- Java 21 or later
- Maven 3.6+

### Build Steps

1. **Clone the repository** (or download source)

2. **Build the fat JAR**:
   ```bash
   mvn clean package
   ```

3. **The built JAR** will be at:
   ```
   target/Jex-<version>.jar
   ```

4. **Install it**:
   ```bash
   java -jar target/Jex-<version>.jar --install
   ```

### Development Commands

```bash
# Compile only
mvn compile

# Run tests
mvn test

# Build without tests
mvn package -DskipTests

# Clean build directory
mvn clean
```

## Project Structure

```
Jex/
├── src/
│   ├── main/
│   │   ├── java/org/jex/cli/
│   │   │   ├── Jex.java              # Main entry point and command routing
│   │   │   ├── Install.java          # Install command implementation
│   │   │   ├── JexPlugin.java        # JexPlugin interface (2 methods)
│   │   │   ├── PluginLoader.java     # Dynamic JAR loading via URLClassLoader
│   │   │   ├── PluginManager.java    # Plugin install/update/uninstall lifecycle
│   │   │   ├── PluginMetadata.java   # Plugin metadata data class
│   │   │   ├── PathConfig.java       # OS-aware path management
│   │   │   ├── ArgumentParser.java   # Argument parsing with automatic help handling
│   │   │   ├── ConfigParser.java     # Static YAML config loading (config.yaml)
│   │   │   ├── JexUtil.java          # Shared utilities (JAR scanning, Maven version checks)
│   │   │   └── JexMavenUtil.java     # Maven utilities (dynamic version detection)
│   │   ├── java/org/jex/plugins/
│   │   │   └── newplugin/
│   │   │       └── NewPlugin.java    # Internal plugin generator
│   │   └── resources/
│   │       ├── jex.sh                # Unix wrapper script template
│   │       ├── jex.bat               # Windows wrapper script template
│   │       └── plugins/
│   │           └── newplugin/        # Plugin generator resources
│   └── test/
│       └── java/org/jex/cli/
│           └── JexTest.java
├── pom.xml                            # Maven build configuration
├── CLAUDE.md                          # Project instructions for Claude Code
└── README.md                          # This file
```

## Implementation Status

### ✅ Completed Features (through v1.0.4)
- **Minimal core architecture** - Only a few built-in commands (`--install`, `--list`/`-l`, `--help`/`-h`, `-v`/`--version`, plugin management)
- **Self-installing fat JAR** with Maven Shade Plugin
- **OS-specific installation** (Linux, macOS, Windows)
- **Wrapper script** generation and installation
- **Help system** (`--help`, `-h`) with two-tier plugin help - auto-generated quick reference vs. custom `help.txt`
- **Install command** (`--install`) - renamed from `--setup`
- **List plugins** command (`--list`, `-l`)
- **Dynamic plugin loading** - URLClassLoader-based JAR loading
- **Plugin instantiation and execution**
- **Plugin lifecycle management** - `--install-plugin`, `--update-plugin`, `--uninstall-plugin`
- **YAML-based argument parsing** for plugins via `ArgumentParser.parse()` - handles help, validation, and errors in one call
- **Configuration directory management**
- **Dynamic version detection** - Uses Maven metadata API for automatic version resolution
- **Internal plugin discovery** - Automatic discovery of plugins in `org.jex.plugins` package
- **Plugin generator** (`new-plugin`) - Internal plugin that creates complete Maven projects with `arguments.yaml` and the correct version
- **Package reorganization** - Migrated from `solutions.cloudbusiness.cli` to `org.jex.cli`

### ✅ New in v1.0.5
- **Fixed** `new-plugin --package` not propagating the package name into the generated `pom.xml`'s groupId
- **Fixed** an `arguments.yaml` typo that silently broke `new-plugin`'s own argument parsing (`--package` was rejected as unrecognized)
- **`--install`** now verifies Maven 3.6+ is installed, exiting with install/upgrade instructions if missing or too old
- **`--install`** now refreshes the Jex artifact in the local Maven repository on every run, so plugin projects never build against a stale cached copy
- **New `config.yaml`/`ConfigParser`** - static plugin configuration (defaults, constants, feature flags), scaffolded by `new-plugin` alongside `arguments.yaml`

### 🚀 Architecture Achievements
- **Lazy loading** - Plugins only loaded when invoked
- **Minimal overhead** - Core has no plugin-specific code
- **Plugin-based extensibility** - Even the generator is a plugin
- **Zero framework complexity** - Simple JexPlugin interface (2 methods)
- **Scales to hundreds of plugins** without performance degradation

## Dependencies

- **Apache Commons CLI 1.11.0**: Command-line argument parsing
- **SnakeYAML 2.5**: YAML configuration file parsing
- **Maven Model 3.9.6**: Maven metadata reading (for dynamic version detection)
- **JUnit 3.8.1**: Testing framework
- **Maven Shade Plugin 3.5.1**: Fat JAR creation

## License

Apache 2.0 License - see LICENSE file for details.

## Contributing

Found a bug or want a feature? Please open an issue or submit a pull request!
