package io.smallrye.common.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
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
import io.smallrye.common.function.ExceptionFunction;

/**
 * A builder for creating, running, and performing I/O with subprocesses.
 * <p>
 * The overall process follows this state diagram:
 * <div>
 * <img src="{@docRoot}/io/smallrye/common/process/doc-files/states.png" alt="State machine diagram"/>
 * </div>
 * <h2>Problems with the JDK {@link java.lang.ProcessBuilder}</h2>
 * The JDK provides an interface to create and reap processes, which this API is based on.
 * This API contains all of the pieces necessary to perform this function.
 * However, there are many unstated requirements and caveats when using this API:
 * <ul>
 * <li>All I/O has to be performed in separate, individual threads to avoid various deadlocks</li>
 * <li>The ordering of {@linkplain Process#waitFor() process waiting} and I/O is tricky to manage</li>
 * <li>Processes may need to be cleaned up on JDK exit</li>
 * <li>Error and output can be tricky to capture when they also need to be processed</li>
 * </ul>
 * The goal of this process API is to make it easy to create and manage processes,
 * while also making it difficult to inadvertently cause deadlocks or other similar non-obvious problems.
 * <h2>Running processes</h2>
 * Trivial process execution can be initiated using the {@link Exec#exec(Path,List) exec(...)} and
 * {@link Exec#execToString(Path, List) execToString(...)} methods.
 * To configure and create a single process beyond trivial cases,
 * the full builder API must be used.
 * <p>
 * The builder API is used by calling {@link Exec#newBuilder(Path) newBuilder(...)},
 * chaining additional calls from the result of that method to configure the process,
 * and finally calling {@link #run()} to start the process execution.
 * Once {@link #run()} is called, the builder may no longer be used.
 * Here's an example:
 *
 * <pre>
 * <code>
    String result = ProcessBuilder.newBuilder("echo", "hello world")
      .output().toSingleString(500)
      .run();
 * </code>
 * </pre>
 *
 * In this example, a new process builder which calls the program {@code echo} is created.
 * A single argument (the string {@code "hello world"}) is passed to the program.
 * The output of the program is configured to be captured as a single string of up to 500 characters,
 * and then the process is executed.
 * Since the output was configured using {@link Output#toSingleString(int)}, the process execution
 * returns a {@code String}.
 * If, for example, the output is {@linkplain Output#discard() discarded}, then {@link #run()} would
 * return a {@code Void} (i.e., {@code null}).
 * <h2>Configuring builder parameters</h2>
 * <h3>Environment</h3>
 * When a new process builder is created, the environment of the current process is copied into the builder.
 * This can be overridden by calling {@link #environment(Map)} to replace the entire environment for the subprocess,
 * or {@link #modifyEnvironment(Consumer)} to use a handler callback (typically a lambda) to perform arbitrary
 * manipulations of the environment map.
 * <p>
 * When creating a pipeline, the environment of the previous stage is used as the default environment for all
 * subsequent stages.
 * <h3>Working directory</h3>
 * Process builders are initialized to use the working directory of the current process by default.
 * This can be overridden by calling {@link #directory(Path)}.
 * The given path must exist on the {@linkplain FileSystems#getDefault() default filesystem}.
 * <p>
 * Subsequent pipeline stages inherit the working directory of previous stages by default.
 * <h3>Arguments</h3>
 * The command arguments may be given when constructing the builder,
 * or the {@link #arguments(List) arguments(...)} methods may be used to configure the arguments separately.
 * <h3>Timeouts</h3>
 * It is possible to configure a {@linkplain #softExitTimeout(Duration) soft exit timeout}
 * and a {@linkplain #hardExitTimeout(Duration) hard exit timeout}.
 * These timeouts are used to cause the process to exit in the case where it lingers after all I/O is complete.
 * The soft timeout will request a "normal" termination, if it is supported by that platform (i.e. not Windows),
 * if that time elapses after I/O is complete but before the process exits.
 * The hard timeout will forcefully terminate the subprocess if the process lingers for the given duration after
 * the soft timeout has been triggered.
 * <h2>Processing input and output</h2>
 * The methods {@link #input()}, {@link #output()}, and {@link #error()} are used to configure the behavior
 * of the process input, output, and error output, respectively.
 * Each of these methods returns a view of this builder which has methods to configure that aspect of process I/O.
 * <h3>Input processing</h3>
 * Process input may be {@linkplain Input#empty() empty}, {@linkplain Input#inherited() inherited}
 * from the current process, transferred from a {@linkplain Input#transferFrom(Path) file}, a
 * {@linkplain Input#transferFrom(Reader) reader}, or a {@linkplain Input#transferFrom(InputStream) stream},
 * generated from a {@linkplain Input#fromString(String) string} or a
 * {@linkplain Input#fromStrings(Collection) collection of strings}, or produced via callback as
 * {@linkplain Input#produceWith(ExceptionConsumer) characters} or
 * {@link Input#produceBytesWith(ExceptionConsumer) bytes}.
 * <p>
 * When generating input as characters, the input character set is used.
 * By default, the builder will use the {@linkplain Input#nativeCharset() native character set},
 * but a specific character set can be configured by calling {@link Input#charset(Charset)}.
 * These methods have no effect when generating byte input.
 * <h3>Output processing</h3>
 * In addition to determining whether and how process output is consumed,
 * configuring the output handling strategy also affects the type of the value returned by the {@link #run()} method.
 * In cases where the output type is impacted, the chaining method will return a view builder
 * whose type will reflect the new output type.
 * Therefore, the return value of each builder method should normally only be used
 * to call the next builder method,
 * and should not normally be saved or reused.
 * <p>
 * As with input, the {@linkplain Output#nativeCharset() native character set} is used for output
 * processing by default, but this can be {@linkplain Output#charset(Charset) configured} as well.
 * <h4>Basic output targets</h4>
 * Process output can be {@linkplain Output#discard() discarded}, {@linkplain Output#inherited() inherited}
 * from the current process, transferred to a {@linkplain Output#transferTo(Path) file}
 * (optionally in {@linkplain Output#appendTo(Path) append mode}), a
 * {@linkplain Output#transferTo(Writer) writer}, or a {@linkplain Output#transferTo(OutputStream) stream},
 * consumed as {@linkplain Output#consumeBytesWith(ExceptionConsumer) bytes},
 * {@linkplain Output#consumeWith(ExceptionConsumer) characters}, or
 * {@linkplain Output#consumeLinesWith(int, ExceptionConsumer) lines}.
 * <p>
 * The output can also be processed to produce a value of some type.
 * The {@linkplain Output#toSingleString(int) single string} and
 * {@linkplain Output#toStringList(int, int) list of strings} processors are provided,
 * but a custom processor which handles process output as {@linkplain Output#processBytesWith(ExceptionFunction) bytes}
 * or {@linkplain Output#processWith(ExceptionFunction) characters} may be provided.
 * The processor returns an object of the desired type, and on success, that value is returned
 * from {@link #run()}.
 * <p>
 * It is also possible to configure the builder to capture the process output to include in
 * any thrown exception.
 * This is disabled by default but can be enabled by calling {@link Output#gatherOnFail(boolean) Output.gatherOnFail(true)}.
 * <h4>Multiple output handlers</h4>
 * It is also possible to split or "tee" a copy of the process output to one or more additional
 * consumers.
 * This can be useful, for example, to log the output of a process while also passing it on to another handler.
 * The {@code Output.copyAndXxx(...)} methods work similarly to the primary output control methods,
 * but also copy the data to the given handler.
 * <h3>Error processing</h3>
 * Error output may be processed similarly to how output is processed, with the exception that
 * error output cannot be piped to another pipeline stage.
 * <p>
 * It is also possible to merge the error output of the process into its regular output by calling
 * {@link Error#redirect()}.
 * The combined standard error and standard output will be merged and handled using the output handler.
 * Note that using this mode will prevent error output from being captured in the case of an exception.
 * <h2>Process pipelines</h2>
 * Process output can be configured to feed directly into the input of another process
 * by calling one of the {@link Output#pipeTo(Path) Output.pipeTo(...)} methods.
 * These methods return a <em>new</em> process builder to configure the subsequent pipeline stage.
 * Once the {@code pipeTo(...)} method is called, the builder for the original process can no longer be modified.
 * Thus, the output stage should generally be configured <em>last</em>.
 * <p>
 * Since the new process builder represents a subsequent pipeline stage
 * whose input always comes from the previous stage,
 * there is no way to configure input handling for subsequent pipeline stages.
 * Additionally, timeouts apply to the entire pipeline, not to individual stages;
 * thus, timeouts must be configured only on the <em>first</em> pipeline stage.
 * Other parameters, such as the working directory, environment, etc. may be configured on a per-stage basis
 * and normally inherit their settings from the previous stage.
 * <h2>Exit code validation</h2>
 * By default, if the process exits with a nonzero exit code it is assumed to have failed,
 * which will trigger an exception to be thrown that details the problem.
 * A custom exit code checker can be {@linkplain #exitCodeChecker(IntPredicate) injected} which can be used to
 * capture and save the exit code and/or establish an alternative validation policy for exit codes.
 * The checker is a predicate which returns {@code true} if the exit code is successful or {@code false} if
 * it should result in a failure exception being thrown.
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
     * @deprecated Use {@link Exec#exec(Path, String...)} instead.
     */
    @Deprecated
    static void exec(Path command, String... args) {
        exec(command, List.of(args));
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @deprecated Use {@link Exec#exec(Path, List)} instead.
     */
    @Deprecated
    static void exec(Path command, List<String> args) {
        newBuilder(command).arguments(args).run();
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @deprecated Use {@link Exec#exec(String, String...)} instead.
     */
    @Deprecated
    static void exec(String command, String... args) {
        exec(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, discarding the result.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @deprecated Use {@link Exec#exec(String, List)} instead.
     */
    @Deprecated
    static void exec(String command, List<String> args) {
        exec(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     * @deprecated Use {@link Exec#execToString(Path, String...)} instead.
     */
    @Deprecated
    static String execToString(Path command, String... args) {
        return execToString(command, List.of(args));
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     * @deprecated Use {@link Exec#execToString(Path, List)} instead.
     */
    @Deprecated
    static String execToString(Path command, List<String> args) {
        return newBuilder(command).arguments(args).output().toSingleString(65536).run();
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     * @deprecated Use {@link Exec#exec(String, String...)} instead.
     */
    @Deprecated
    static String execToString(String command, String... args) {
        return execToString(Path.of(command), args);
    }

    /**
     * Execute the given command and arguments, returning the result as a single string.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the result as a single string (not {@code null})
     * @deprecated Use {@link Exec#execToString(String, List)} instead.
     */
    @Deprecated
    static String execToString(String command, List<String> args) {
        return execToString(Path.of(command), args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @return the new process builder (not {@code null})
     * @deprecated Use {@link Exec#newBuilder(Path)} instead.
     */
    @Deprecated
    static ProcessBuilder<Void> newBuilder(Path command) {
        return new ProcessBuilderImpl<>(Assert.checkNotNullParam("command", command));
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     * @deprecated Use {@link Exec#newBuilder(Path, List)} instead.
     */
    @Deprecated
    static ProcessBuilder<Void> newBuilder(Path command, List<String> args) {
        return newBuilder(command).arguments(args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     * @deprecated Use {@link Exec#newBuilder(Path, String...)} instead.
     */
    @Deprecated
    static ProcessBuilder<Void> newBuilder(Path command, String... args) {
        return newBuilder(command).arguments(args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @return the new process builder (not {@code null})
     * @deprecated Use {@link Exec#newBuilder(String)} instead.
     */
    @Deprecated
    static ProcessBuilder<Void> newBuilder(String command) {
        return newBuilder(Path.of(command));
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     * @deprecated Use {@link Exec#newBuilder(String, List)} instead.
     */
    @Deprecated
    static ProcessBuilder<Void> newBuilder(String command, List<String> args) {
        return newBuilder(Path.of(command), args);
    }

    /**
     * Create a new process builder.
     *
     * @param command the command to execute (must not be {@code null})
     * @param args the arguments (must not be {@code null})
     * @return the new process builder (not {@code null})
     * @deprecated Use {@link Exec#newBuilder(String, String...)} instead.
     */
    @Deprecated
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
        default Input<O> nativeCharset() {
            return charset(ProcessUtil.nativeCharset());
        }

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
