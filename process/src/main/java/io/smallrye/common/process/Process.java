package io.smallrye.common.process;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.smallrye.common.os.OS;

/**
 * TODO
 */
public final class Process {
    Process() {
    }

    /**
     * {@return the {@code Path} of the current Java executable (not {@code null})}
     * The returned path may be absolute, or it may be relative to the current {@code PATH} of this process.
     * If the path cannot be determined, a relative path containing {@link #nameOfJava()}
     * is returned.
     */
    public static Path pathOfJava() {
        return JavaPath.path;
    }

    /**
     * {@return the name of the standard Java executable on this OS (not {@code null})}
     */
    public static String nameOfJava() {
        return JavaPath.javaName;
    }

    private static final class JavaPath {
        private JavaPath() {
        }

        private static final String javaName;
        private static final Path path;

        static {
            javaName = OS.current() == OS.WINDOWS ? "java.exe" : "java";
            Path javaPath = null;
            Optional<String> javaCommand = ProcessHandle.current().info().command();
            if (javaCommand.isPresent()) {
                javaPath = Path.of(javaCommand.get());
            }
            if (javaPath == null) {
                String javaHome = System.getProperty("java.home");
                if (javaHome != null) {
                    javaPath = Path.of(javaHome, "bin", javaName);
                    if (!Files.isExecutable(javaPath)) {
                        javaPath = null;
                    }
                }
            }
            if (javaPath == null && OS.current() == OS.WINDOWS) {
                // Try executable extensions if windows
                String pathExt = System.getenv("PATHEXT");
                if (pathExt != null) {
                    for (String item : pathExt.split(File.pathSeparator)) {
                        javaPath = Path.of(item, javaName);
                        if (Files.isExecutable(javaPath)) {
                            // it is executable and reachable from here
                            break;
                        } else {
                            javaPath = null;
                        }
                    }
                }
            }
            if (javaPath == null) {
                // give up
                javaPath = Path.of(javaName);
            }
            path = javaPath;
        }
    }
}
