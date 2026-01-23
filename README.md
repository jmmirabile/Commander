# Jex

A plugin-based CLI framework for Java that allows developers to create modular, extensible command-line applications.

## Overview

I had multiple Java "test" apps spread out across project directories. I needed a way to organize them and combine them 
into a single framework. It seemed I could mimic similar implementation I did in Python. 

I'm calling it Jex, short for Java Executor. It is a Java-based command line, plugin framework. Plugins are self-contained 
JAR files that can be dropped into the plugins directory and registered in a plugin.yaml file.

As I worked through the design, I decided to use Claude Code as well. The amount of time saved using
AI tools like Claude is invaluable. Coding with AI is like having a robot or a development partner that can type 1000 
characters per second. The more specific you can be when describing code or components, the better the result.

However, I will point out, I came up with the architecture, plugin system, had written some of the code already and 
decided on which libraries to use. When using AI for coding, we still need know what you're building, what good design is,
as well as best practices. Use AI as a powerful assistant, but not a replacement for thinking.

It was an iterative process, certainly not a single prompt that produced final application. It's cool to discuss design,
code and changes with AI. It really does feel natural and they are doing a great job with the natural conversational 
interaction. The cooperative coding session is always conversational, resulting in refinement, testing and iteration. 
Code has to be reviewed, tested, retested and changed. I knew what I wanted, reviewed and tested the code, suggested changes, 
and drove the effort. 

I think its important to talk about the AI interaction. While AI can write code way faster than us humans, it does make 
mistakes and lose the thread. There are also technical issues. If your IDE crashes, you may have to retrain your AI partner.
When you start a new AI session, you need to instruct the AI to relearn your project and it does sometimes seem to have a 
slightly different perspective on where you left off. For Claude Code specifically, you have to tell it to write the 
TODO list to the project CLAUDE.md file. I did that, my IDE hung (Intellij) and when I restarted, did some work and 
instructed Claude to update the TODO list, it created a new TODO.md file despite the fact there were existing TODO items
in the CLAUDE.md file.

## Architecture Origins
The idea or concept behind this is based on the concept and lessons learned from my Python-based multi-platform REST client framework 
project. Again, this was created for my use case, consolidating Java test classes. For example, I have a Java Test class that 
connects to HTTPS endpoints with different TLS versions, ciphers, and keystores and certificate bundles. While that can be 
done with openssl, sometimes showing others that the HTTPS endpont works with Java too. As we all know, sometimes you have to 
prove something works. So, it's meant to be simple to migrate test programs to Jex and simple to run. The user should be able to 
type "jex" and see help and the available options. So, you can type "jex -h" or "jex --list" to see plugins, or "jex plugin-name -h" 
to see the Plugin's help and options.  

- **Lazy loading** or **on-demand loading** of plugins. The first argument is either a Jex argument or the name of a plugin. Only that plugin is loaded
  - As the number of plugins increases, loading them all would make Jex slower. No need to load all the plugins at once.
- **Simplicity beats complexity** - Frameworks can become overhead, introduce unnecessary complexity, design challenges 
  due to the framework requirements. I reviewed several Python "cli" tools, but they seemed to add unnecessary complexity.
  For Jex, I could have used Picocli, but it doesn't lazy load and requires changes to the bootstrap or main 
  application. Jex is simple, small and extensible and adding new plugins has no impact on Jex speed and 
  does not require a recompile.
- **YAML-based plugin registries** are cleaner than code-based registration.
  - I considered using a simple properties file, but they would get messy as plugin count increased.
  - Yaml is easier to read. A fair trade off I think. 
- Each plugin is isolated and only loaded when invoked.
- Minimal framework complexity - It's really just a bootstrap for command line apps.
- Complete separation of concerns - Each plugin does it's own thing and should have no dependencies on others (unless you want them to)
  - There is nothing keeping anyone from writing plugins that are executed in serieds or have some relationship.
  - A plugin can use any Java library or framework as needed, but those resources should be within the Jar file as plugins are installed as jar files. 


**Key Features:**
- Self-installing fat JAR with OS-specific wrapper scripts
- YAML-based configuration for arguments and plugins
- **Arguments** are defined in a yaml file, not code!
- Dynamic plugin loading from JAR files
- Built-in help and plugin management commands
- Zero configuration required - just download and run "java -jar <jex jar file> --install"
- Update Jex without overwriting the installed plugins.
- Runs on Linux, MacOS and Windows

## Quick Start

### Installation

1. **Download** the `Jex-1.0.3.jar` file (fat JAR with all dependencies) from the Releases link.

2. **Run install** to set up Jex:
   ```bash
   java -jar Jex-1.0.3.jar --install
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
   jex new-plugin --help    # Test internal plugin generator
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
java -jar Jex-1.0.2.2.jar --install  # First-time installation
```

This command:
- Creates configuration directory (OS-specific location)
- Creates `plugins/` subdirectory
- Generates empty `plugin.yaml` registry template
- Generates default `arguments.yaml` for Jex
- **Installs** `jex.jar` to the lib directory
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
java -jar Jex-1.0.2.2.jar --install

# Create a new plugin project
jex new-plugin my-awesome-tool --package com.example

# Run a plugin
jex <plugin-name> [args...]
```

### Installing a Plugin ⏳ Manual Process (Automated loading pending)

1. **Copy** the plugin JAR to the plugins directory:
   - Linux: `~/.config/Jex/plugins/`
   - macOS: `~/Library/Application Support/Jex/plugins/`
   - Windows: `%APPDATA%\Jex\plugins\`

2. **Register** the plugin in `plugin.yaml`:
   ```yaml
   my-plugin:
     jar: my-plugin.jar
     class: com.example.MyPlugin
     version: 1.0.0
     description: "My custom plugin"
   ```

3. **Run** the plugin:
   ```bash
   Jex my-plugin [args...]
   ```
   *Note: Automatic plugin loading is in progress*

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
     - name: help
       short: h
       long: help
       description: "Display help information"
       required: false
       hasArg: false
     - name: input
       short: i
       long: input-file
       description: "Input file path"
       required: true
       hasArg: true
       argName: "file"
   ```

4. **(Optional) Create custom help file** at `src/main/resources/help.txt`:
   - Uncomment `config.help_text_file` in `arguments.yaml`
   - Add detailed documentation, examples, and usage instructions
   - Shown when user runs `jex <plugin> --help`
   - If not specified, help is auto-generated from options

5. **Build the plugin JAR** with `arguments.yaml` included as a resource

6. **Install the plugin**:
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
│           └── arguments.yaml
├── pom.xml
└── README.md
```

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
   target/Jex-1.0.2.2.jar
   ```

4. **Install it**:
   ```bash
   java -jar target/Jex-1.0.2.2.jar --install
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
│   │   │   ├── PathConfig.java       # OS-aware path management
│   │   │   ├── ArgumentParser.java   # Argument parsing with automatic help handling
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
│           └── AppTest.java
├── pom.xml                            # Maven build configuration
├── CLAUDE.md                          # Project instructions for Claude Code
├── TODO.md                            # Persistent TODO list
└── README.md                          # This file
```

## Implementation Status

### ✅ Completed Features (v1.0.2)
- **Minimal core architecture** - Only 3 built-in commands
- **Self-installing fat JAR** with Maven Shade Plugin
- **OS-specific installation** (Linux, macOS, Windows)
- **Wrapper script** generation and installation
- **Help system** (`--help`, `-h`)
- **Install command** (`--install`) - renamed from `--setup`
- **List plugins** command (`--list`, `-l`)
- **Dynamic plugin loading** - URLClassLoader-based JAR loading
- **Plugin instantiation and execution**
- **YAML-based argument parsing** for plugins
- **Configuration directory management**
- **Dynamic version detection** - Uses Maven metadata API for automatic version resolution
- **Internal plugin discovery** - Automatic discovery of plugins in `org.jex.plugins` package
- **Plugin generator** (`new-plugin`) - Internal plugin that creates complete Maven projects with correct version
- **Package reorganization** - Migrated from `solutions.cloudbusiness.cli` to `org.jex.cli`

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
