package io.smallrye.common.process;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.os.OS;

/**
 * A collection of useful process-related utility methods.
 */
public final class ProcessUtil {
    private ProcessUtil() {
    }

    private static final class NativeCharset {
        private static final Charset charset;

        static {
            Charset c;
            try {
                c = Charset.forName(System.getProperty("native.encoding", "UTF-8"));
            } catch (UnsupportedCharsetException ignored) {
                c = Charset.defaultCharset();
            }
            charset = c;
        }
    }

    /**
     * {@return the native character set (not {@code null})}
     */
    public static Charset nativeCharset() {
        return NativeCharset.charset;
    }

    /**
     * Forcibly destroy the process and all of its descendants.
     *
     * @param handle the root-most process handle (must not be {@code null})
     */
    public static void destroyAllForcibly(ProcessHandle handle) {
        // capture the child processes *before* killing them
        for (ProcessHandle processHandle : handle.children().toList()) {
            destroyAllForcibly(processHandle);
        }
        handle.destroyForcibly();
    }

    /**
     * Get the absolute path of a command at the given path.
     * If the path is relative, then the location of the executable is determined in a platform-specific manner
     * (typically by reading the {@code PATH} environment variable).
     * If the path is absolute, it is returned if the target file is executable.
     *
     * @param path the command to locate (must not be {@code null})
     * @return the optional path of the executable, or the empty optional if there is no such path (not {@code null})
     */
    public static Optional<Path> pathOfCommand(Path path) {
        Assert.checkNotNullParam("path", path);
        if (path.isAbsolute()) {
            return Files.isExecutable(path) ? Optional.of(path) : Optional.empty();
        }
        String pathEnv = System.getenv("PATH");
        for (String segment : pathEnv.split(Pattern.quote(File.pathSeparator))) {
            if (!segment.isEmpty()) {
                Path execPath = Path.of(segment).resolve(path);
                if (Files.isExecutable(execPath)) {
                    return Optional.of(execPath);
                }
            }
        }
        if (OS.current() == OS.WINDOWS) {
            pathEnv = System.getenv("PATHEXT");
            for (String segment : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                if (!segment.isEmpty()) {
                    Path execPath = Path.of(segment).resolve(path);
                    if (Files.isExecutable(execPath)) {
                        return Optional.of(execPath);
                    }
                }
            }
        }
        return Optional.empty();
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

    /**
     * Wait (uninterruptibly) for some amount of time for the given process to finish.
     *
     * @param proc the process (must not be {@code null})
     * @param nanos the number of nanoseconds to wait
     * @return {@code true} if the process is still running after the elapsed time, or {@code false} if it has exited
     */
    public static boolean stillRunningAfter(Process proc, long nanos) {
        boolean intr = false;
        try {
            long start = System.nanoTime();
            for (;;) {
                if (nanos <= 0) {
                    return proc.isAlive();
                }
                try {
                    return !proc.waitFor(nanos, TimeUnit.NANOSECONDS);
                } catch (InterruptedException ignored) {
                    intr = true;
                }
                nanos -= -start + (start = System.nanoTime());
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Wait (uninterruptibly) for some amount of time for the given process to finish.
     *
     * @param proc the process handle (must not be {@code null})
     * @param nanos the number of nanoseconds to wait
     * @return {@code true} if the process is still running after the elapsed time, or {@code false} if it has exited
     */
    public static boolean stillRunningAfter(ProcessHandle proc, long nanos) {
        boolean intr = false;
        try {
            long start = System.nanoTime();
            for (;;) {
                if (nanos <= 0) {
                    return proc.isAlive();
                }
                try {
                    proc.onExit().get(nanos, TimeUnit.NANOSECONDS);
                } catch (InterruptedException ignored) {
                    intr = true;
                } catch (ExecutionException | TimeoutException ignored) {
                }
                nanos -= -start + (start = System.nanoTime());
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
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
                if (javaHome == null) {
                    javaHome = System.getenv("JAVA_HOME");
                }
                if (javaHome != null) {
                    javaPath = Path.of(javaHome, "bin", javaName);
                    if (!Files.isExecutable(javaPath)) {
                        javaPath = null;
                    }
                }
            }
            Path javaRelativePath = Path.of(javaName);
            if (javaPath == null) {
                javaPath = pathOfCommand(javaRelativePath).orElse(javaRelativePath);
            }
            path = javaPath;
        }
    }
}
