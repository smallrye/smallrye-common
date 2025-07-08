package io.smallrye.common.process;

import java.nio.file.Path;
import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 * A factory class for creating new process builders.
 */
public final class Exec {
    private Exec() {
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    public static void exec(Path command, String... args) {
        exec(command, List.of(args));
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    public static void exec(Path command, List<String> args) {
        newBuilder(command).arguments(args).run();
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    public static void exec(String command, String... args) {
        exec(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    public static void exec(String command, List<String> args) {
        exec(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     */
    public static String execToString(Path command, String... args) {
        return execToString(command, List.of(args));
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     */
    public static String execToString(Path command, List<String> args) {
        return newBuilder(command).arguments(args).output().toSingleString(65536).run();
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     */
    public static String execToString(String command, String... args) {
        return execToString(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     */
    public static String execToString(String command, List<String> args) {
        return execToString(Path.of(command), args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    public static ProcessBuilder<Void> newBuilder(Path command) {
        return new ProcessBuilderImpl<>(Assert.checkNotNullParam("command", command));
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    public static ProcessBuilder<Void> newBuilder(Path command, List<String> args) {
        return newBuilder(command).arguments(args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    public static ProcessBuilder<Void> newBuilder(Path command, String... args) {
        return newBuilder(command).arguments(args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    public static ProcessBuilder<Void> newBuilder(String command) {
        return newBuilder(Path.of(command));
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    public static ProcessBuilder<Void> newBuilder(String command, List<String> args) {
        return newBuilder(Path.of(command), args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    public static ProcessBuilder<Void> newBuilder(String command, String... args) {
        return newBuilder(Path.of(command), args);
    }

}
