package io.smallrye.common.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionConsumer;

/**
 * A builder for creating, running, and performing I/O with subprocesses.
 * <p>
 * The overall process follows this state diagram:
 * <div>
 * <img src="{@docRoot}/io/smallrye/common/process/doc-files/states.png" alt="State machine diagram"/>
 * </div>
 *
 * @param <O> the output type
 */
public sealed interface ProcessBuilder<O>
        extends PipelineBuilder<O> permits ProcessBuilder.Input, ProcessBuilderImpl, ProcessBuilderImpl.ViewImpl {
    /**
     * The default soft timeout duration.
     */
    Duration DEFAULT_SOFT_TIMEOUT = Duration.of(5, ChronoUnit.SECONDS);

    /**
     * The default hard timeout duration.
     */
    Duration DEFAULT_HARD_TIMEOUT = Duration.of(30, ChronoUnit.SECONDS);

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    static void exec(Path command, String... args) {
        exec(command, List.of(args));
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    static void exec(Path command, List<String> args) {
        newBuilder(command).arguments(args).run();
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    static void exec(String command, String... args) {
        exec(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     */
    static void exec(String command, List<String> args) {
        exec(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     */
    static String execToString(Path command, String... args) {
        return execToString(command, List.of(args));
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     */
    static String execToString(Path command, List<String> args) {
        return newBuilder(command).arguments(args).output().toSingleString(65536).run();
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     */
    static String execToString(String command, String... args) {
        return execToString(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     */
    static String execToString(String command, List<String> args) {
        return execToString(Path.of(command), args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    static ProcessBuilder<Void> newBuilder(Path command) {
        return new ProcessBuilderImpl<>(Assert.checkNotNullParam("command", command));
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    static ProcessBuilder<Void> newBuilder(Path command, List<String> args) {
        return newBuilder(command).arguments(args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    static ProcessBuilder<Void> newBuilder(Path command, String... args) {
        return newBuilder(command).arguments(args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    static ProcessBuilder<Void> newBuilder(String command) {
        return newBuilder(Path.of(command));
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    static ProcessBuilder<Void> newBuilder(String command, List<String> args) {
        return newBuilder(Path.of(command), args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     */
    static ProcessBuilder<Void> newBuilder(String command, String... args) {
        return newBuilder(Path.of(command), args);
    }

    @Override
    ProcessBuilder<O> arguments(List<String> command);

    @Override
    default ProcessBuilder<O> arguments(String... command) {
        return arguments(List.of(command));
    }

    @Override
    ProcessBuilder<O> directory(Path directory);

    @Override
    default ProcessBuilder<O> environment(Map<String, String> newEnvironment) {
        Assert.checkNotNullParam("newEnvironment", newEnvironment);
        return modifyEnvironment(env -> {
            env.clear();
            env.putAll(newEnvironment);
        });
    }

    @Override
    ProcessBuilder<O> modifyEnvironment(Consumer<Map<String, String>> action);

    @Override
    ProcessBuilder<O> exitCodeChecker(IntPredicate checker);

    @Override
    ProcessBuilder<O> whileRunning(Consumer<WaitableProcessHandle> action);

    /**
     * Configure the input handling of the process.
     *
     * @return the input configuration view of this builder (not {@code null})
     */
    Input<O> input();

    /**
     * Set the soft exit timeout.
     * This is the time to wait after all I/O is processed before gracefully terminating the subprocess.
     *
     * @param duration the soft exit timeout, or {@code null} for no timeout
     * @return this builder
     */
    ProcessBuilder<O> softExitTimeout(Duration duration);

    /**
     * Set the hard exit timeout.
     * This is the time to wait after all I/O is processed and the soft timeout has elapsed
     * before forcefully terminating the subprocess.
     *
     * @param duration the hard exit timeout, or {@code null} for no timeout
     * @return this builder
     */
    ProcessBuilder<O> hardExitTimeout(Duration duration);

    /**
     * The input handling aspect of the process builder.
     *
     * @param <O> the output type
     */
    sealed interface Input<O> extends ProcessBuilder<O> permits ProcessBuilderImpl.InputImpl {
        /**
         * Instruct the builder to provide no input to the process being built.
         *
         * @return this builder
         */
        Input<O> empty();

        /**
         * Instruct the builder to inherit the input of the current process into the process being built.
         *
         * @return this builder
         */
        Input<O> inherited();

        /**
         * Set the character set to use for input handling.
         *
         * @param charset the character set (must not be {@code null})
         * @return this builder
         */
        Input<O> charset(Charset charset);

        /**
         * Instruct the builder to use the native character set for input handling.
         *
         * @return this builder
         */
        Input<O> nativeCharset();

        /**
         * Use the given string as the input for the process being built.
         *
         * @param string the string (must not be {@code null})
         * @return this builder
         */
        default Input<O> fromString(String string) {
            return produceWith(w -> w.write(string));
        }

        /**
         * Use the given strings as the input for the process being built.
         * Each entry in the collection is converted to a string using {@link String#valueOf(Object)}
         * and written on its own line.
         *
         * @param strings the strings (must not be {@code null})
         * @return this builder
         */
        default Input<O> fromStrings(Collection<?> strings) {
            return produceWith(w -> {
                for (Object s : strings) {
                    w.write(String.valueOf(s));
                    w.write(System.lineSeparator());
                }
            });
        }

        /**
         * Instruct the builder to transfer the given stream's contents to the input of the process.
         * The stream is not closed.
         * The character set is not used for this input mode.
         *
         * @param stream the stream to transfer from (must not be {@code null})
         * @return this builder
         */
        default Input<O> transferFrom(InputStream stream) {
            return produceBytesWith(stream::transferTo);
        }

        /**
         * Instruct the builder to transfer the given reader's contents to the input of the process.
         * The reader is not closed.
         *
         * @param reader the reader to transfer from (must not be {@code null})
         * @return this builder
         */
        default Input<O> transferFrom(Reader reader) {
            return produceWith(reader::transferTo);
        }

        /**
         * Instruct the builder to transfer the entire contents of the file at the given path
         * to the input of the process.
         * The character set is not used for this input mode.
         *
         * @param path the path to transfer from (must not be {@code null})
         * @return this builder
         */
        Input<O> transferFrom(Path path);

        /**
         * Instruct the builder to produce the process input using the given stream consumer.
         * The character set is not used for this input mode.
         *
         * @param consumer the stream consumer (must not be {@code null})
         * @return this builder
         */
        Input<O> produceBytesWith(ExceptionConsumer<OutputStream, IOException> consumer);

        /**
         * Instruct the builder to produce the process input using the given writer consumer.
         *
         * @param consumer the writer consumer (must not be {@code null})
         * @return this builder
         */
        Input<O> produceWith(ExceptionConsumer<Writer, IOException> consumer);
    }
}
