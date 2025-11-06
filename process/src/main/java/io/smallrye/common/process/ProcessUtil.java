package io.smallrye.common.process;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

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
     * @param handle the root-most process handle
     */
    public static void destroyAllForcibly(ProcessHandle handle) {
        if (handle == null) {
            return;
        }
        // capture the child processes *before* killing them
        for (ProcessHandle processHandle : handle.children().toList()) {
            destroyAllForcibly(processHandle);
        }
        handle.destroyForcibly();
    }

    /**
     * Forcibly destroy the process and all of its descendants.
     *
     * @param process the root-most process
     */
    public static void destroyAllForcibly(Process process) {
        if (process == null) {
            return;
        }
        // capture the child processes *before* killing them
        for (ProcessHandle processHandle : process.children().toList()) {
            destroyAllForcibly(processHandle);
        }
        process.destroyForcibly();
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
        for (Path segment : searchPath()) {
            Path execPath = segment.resolve(path);
            if (OS.current() == OS.WINDOWS) {
                for (String ext : Windows.pathExt) {
                    Path execPathExt = execPath.getParent().resolve(execPath.getFileName() + ext);
                    if (Files.isExecutable(execPathExt)) {
                        return Optional.of(execPathExt);
                    }
                }
            }
            if (Files.isExecutable(execPath)) {
                return Optional.of(execPath);
            }
        }
        return Optional.empty();
    }

    /**
     * {@return the system search path (i.e. the {@code PATH} environment variable) as a list of {@link Path} (not
     * {@code null})}
     */
    public static List<Path> searchPath() {
        return PathEnv.path;
    }

    /**
     * {@return the {@code Path} of the current Java executable (not {@code null})}
     * The returned path may be absolute, or it may be relative to the current {@code PATH} of this process.
     * If the path cannot be determined, a relative path containing {@link #nameOfJava()}
     * is returned.
     */
    public static Path pathOfJava() {
        return JavaPath.javaPath;
    }

    /**
     * {@return the path corresponding to {@code java.home} or the {@code JAVA_HOME} environment variable, if any}
     */
    public static Optional<Path> javaHome() {
        return JavaPath.javaHome;
    }

    /**
     * {@return the name of the standard Java executable on this OS (not {@code null})}
     */
    public static String nameOfJava() {
        return JavaPath.javaName;
    }

    private static final Duration LONGEST_NS = Duration.ofNanos(Long.MAX_VALUE);

    /**
     * Wait (uninterruptibly) for some amount of time for the given process to finish.
     *
     * @param proc the process (must not be {@code null})
     * @param nanos the number of nanoseconds to wait
     * @return {@code true} if the process is still running after the elapsed time, or {@code false} if it has exited
     */
    public static boolean stillRunningAfter(Process proc, long nanos) {
        return stillRunningAfter(proc, Duration.ofNanos(nanos));
    }

    /**
     * Wait (uninterruptibly) for some amount of time for the given process to finish.
     *
     * @param proc the process (must not be {@code null})
     * @param time the amount of time to wait (must not be {@code null})
     * @return {@code true} if the process is still running after the elapsed time, or {@code false} if it has exited
     */
    public static boolean stillRunningAfter(Process proc, Duration time) {
        long nanos = time.compareTo(LONGEST_NS) > 0 ? Long.MAX_VALUE : time.toNanos();
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

    private static final class PathEnv {
        private static final List<Path> path = Stream.of(getenv("PATH").split(File.pathSeparator))
                .filter(s -> !s.isEmpty())
                .map(Path::of)
                .toList();
    }

    private static final class Windows {
        private static final List<String> pathExt = Stream
                .of(getenv("PATHEXT").split(File.pathSeparator))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Do not use System.getenv().getOrDefault(name, "") as it is not case insensitive,
     * and PATH is referenced as Path on Windows.
     */
    private static String getenv(String name) {
        String envVariable = System.getenv(name);
        if (envVariable == null) {
            return "";
        }
        return envVariable;
    }

    private static final class JavaPath {
        private JavaPath() {
        }

        private static final String javaName;
        private static final Optional<Path> javaHome;
        private static final Path javaPath;

        static {
            String javaHomeStr = System.getProperty("java.home");
            if (javaHomeStr == null) {
                javaHomeStr = System.getenv("JAVA_HOME");
            }
            javaHome = javaHomeStr == null ? Optional.empty() : Optional.of(Path.of(javaHomeStr));
            javaName = OS.current() == OS.WINDOWS ? "java.exe" : "java";
            Path javaTestPath = ProcessHandle.current().info().command().map(Path::of).orElse(null);
            if ((javaTestPath == null || !Files.isExecutable(javaTestPath)) && javaHomeStr != null) {
                javaTestPath = Path.of(javaHomeStr, "bin", javaName);
                if (!Files.isExecutable(javaTestPath)) {
                    javaTestPath = null;
                    if (OS.current() == OS.WINDOWS) {
                        for (String ext : Windows.pathExt) {
                            Path execPathExt = Path.of(javaHomeStr, "bin", "java" + ext);
                            if (Files.isExecutable(execPathExt)) {
                                javaTestPath = execPathExt;
                                break;
                            }
                        }
                    }
                }
            }
            Path javaRelativePath = Path.of(javaName);
            if (javaTestPath == null) {
                javaTestPath = pathOfCommand(javaRelativePath).orElse(javaRelativePath);
            }
            javaPath = javaTestPath;
        }
    }
}
