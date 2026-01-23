package ${PACKAGE_NAME};

import org.jex.cli.JexPlugin;
import org.jex.cli.ArgumentParser;
import org.apache.commons.cli.CommandLine;

public class ${CLASS_NAME} implements JexPlugin {

    @Override
    public String getName() {
        return "${PLUGIN_NAME}";
    }

    @Override
    public void execute(String[] args) {
        // Parse arguments (automatically handles help, errors, and validation)
        CommandLine cmd = ArgumentParser.parse(args, getName(), this.getClass());
        if (cmd == null) return;  // Help was shown

        // TODO: Implement your plugin logic here
        // Access arguments using:
        //   cmd.hasOption("option-name")  - Check if option was provided
        //   cmd.getOptionValue("option-name")  - Get option value

        System.out.println("${CLASS_NAME_CAPITALIZED} plugin executed successfully!");

        // Example: Check for options defined in arguments.yaml
        // if (cmd.hasOption("input")) {
        //     String inputFile = cmd.getOptionValue("input");
        //     System.out.println("Input file: " + inputFile);
        // }
    }
}