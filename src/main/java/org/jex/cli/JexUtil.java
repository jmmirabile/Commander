package org.jex.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for Jex operations.
 */
public class JexUtil {

    private static final Pattern MAVEN_VERSION_PATTERN = Pattern.compile("Apache Maven (\\d+\\.\\d+(?:\\.\\d+)?)");

    /**
     * Get the installed Maven version by running "mvn --version".
     *
     * @return Maven version string (e.g., "3.9.6"), or null if Maven is not installed/runnable
     */
    public static String getMavenVersionOrNull() {
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String version = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (version == null) {
                        Matcher matcher = MAVEN_VERSION_PATTERN.matcher(line);
                        if (matcher.find()) {
                            version = matcher.group(1);
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0 ? version : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Compare a dot-separated version string against a minimum required version.
     *
     * @param actual   Detected version (e.g., "3.9.6")
     * @param required Minimum required version (e.g., "3.6")
     * @return true if actual >= required
     */
    public static boolean isVersionAtLeast(String actual, String required) {
        String[] a = actual.split("\\.");
        String[] r = required.split("\\.");
        int len = Math.max(a.length, r.length);

        for (int i = 0; i < len; i++) {
            int av = i < a.length ? parseIntSafe(a[i]) : 0;
            int rv = i < r.length ? parseIntSafe(r[i]) : 0;
            if (av != rv) {
                return av > rv;
            }
        }
        return true;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Scan a JAR file and find the first class that implements JexPlugin.
     *
     * @param jarPath Path to the JAR file
     * @return Instance of the plugin, or null if none found
     */
    public static JexPlugin findPluginInJar(Path jarPath) throws Exception {
        // Load the JAR file
        URL jarUrl = jarPath.toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(
            new URL[]{jarUrl},
            JexUtil.class.getClassLoader()
        );

        // Scan JAR for JexPlugin implementation
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            java.util.Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class")) {
                    String className = name.replace('/', '.').substring(0, name.length() - 6);

                    try {
                        Class<?> clazz = classLoader.loadClass(className);

                        if (JexPlugin.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            Object instance = clazz.getDeclaredConstructor().newInstance();
                            return (JexPlugin) instance;
                        }
                    } catch (Exception e) {
                        // Skip classes that can't be loaded
                    }
                }
            }
        }

        return null;
    }
}
