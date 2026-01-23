# ${PLUGIN_NAME_CAPITALIZED} Plugin

A Jex plugin.

## TODO

- [ ] Change package name in `pom.xml` and `${CLASS_NAME}.java` (currently: `${PACKAGE_NAME}`)
- [ ] Change class name in `${CLASS_NAME}.java` if needed
- [ ] Add custom arguments to `arguments.yaml`
- [ ] Implement plugin logic in `${CLASS_NAME}.java`
- [ ] Update this README with plugin description

## Description

TODO: Describe what this plugin does.

## Installation

### Prerequisites

- Java 21 or later
- Maven 3.6+
- Jex installed

### Build

```bash
mvn clean package
```

### Install

1. **Copy plugin JAR to Jex plugins directory:**

   **Linux:**
   ```bash
   cp target/${ARTIFACT_ID}.jar ~/.config/Jex/plugins/
   ```

   **macOS:**
   ```bash
   cp target/${ARTIFACT_ID}.jar ~/Library/Application\ Support/Jex/plugins/
   ```

   **Windows (PowerShell):**
   ```powershell
   Copy-Item target/${ARTIFACT_ID}.jar $env:APPDATA\Jex\plugins\
   ```

2. **Register plugin in Jex:**

   Edit the plugin registry file:
   - **Linux/macOS:** `~/.config/Jex/plugin.yaml` (Linux) or `~/Library/Application Support/Jex/plugin.yaml` (macOS)
   - **Windows:** `%APPDATA%\Jex\plugin.yaml`

   Add this entry:

   ```yaml
   ${PLUGIN_NAME}:
     jar: ${ARTIFACT_ID}.jar
     class: ${PACKAGE_NAME}.${CLASS_NAME}
     version: 1.0.0
     description: "TODO: Add description"
   ```

## Usage

```bash
# Show help
jex ${PLUGIN_NAME} --help

# Run the plugin
jex ${PLUGIN_NAME} [options]
```

## Development

### Project Structure

```
${ARTIFACT_ID}/
├── src/
│   ├── main/
│   │   ├── java/${PACKAGE_PATH}/
│   │   │   └── ${CLASS_NAME}.java
│   │   └── resources/
│   │       └── arguments.yaml
│   └── test/
├── pom.xml
└── README.md
```

### Customization

1. Edit `${CLASS_NAME}.java` to implement your logic
2. Update `arguments.yaml` to define CLI arguments
3. (Optional) Create `src/main/resources/help.txt` for custom help text
   - Uncomment `config.help_text_file` in `arguments.yaml` to enable
   - If not specified, help is auto-generated from options
4. Update this README with usage information

## License

TODO: Add license information
