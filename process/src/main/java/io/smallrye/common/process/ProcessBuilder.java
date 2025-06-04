package io.smallrye.common.process;

import java.io.BufferedReader;
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
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
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
 */
public interface ProcessBuilder<O> {
    Duration DEFAULT_SOFT_TIMEOUT = Duration.of(60, ChronoUnit.SECONDS);
    Duration DEFAULT_HARD_TIMEOUT = Duration.of(90, ChronoUnit.SECONDS);

    static Result<Void> exec(String... command) {
        return newBuilder(command).run();
    }

    static Result<String> execToString(String... command) {
        return newBuilder(command).output().asString(65536).run();
    }

    static ProcessBuilder<Void> newBuilder() {
        throw new UnsupportedOperationException("TODO");
    }

    static ProcessBuilder<Void> newBuilder(List<String> command) {
        return newBuilder().command(command);
    }

    static ProcessBuilder<Void> newBuilder(String... command) {
        return newBuilder().command(command);
    }

    static ProcessBuilder<Void> newBuilder(Executor executor) {
        throw new UnsupportedOperationException("TODO");
    }

    static ProcessBuilder<Void> newBuilder(Executor executor, List<String> command) {
        return newBuilder(executor).command(command);
    }

    static ProcessBuilder<Void> newBuilder(Executor executor, String... command) {
        return newBuilder(executor).command(command);
    }

    ProcessBuilder<O> command(List<String> command);

    default ProcessBuilder<O> command(String... command) {
        command(List.of(command));
        return this;
    }

    ProcessBuilder<O> directory(Path directory);

    ProcessBuilder<O> environment(Map<String, String> newEnvironment);

    Input<O> input();

    Output<O> output();

    Error<O> error();

    Result<O> run() throws ProcessException;

    /**
     * Add a failure exit code checker.
     * The checker returns {@code true} if the exit code matches this condition.
     * If so, the given factory is used to construct an exception to be thrown.
     * The exception will generally include some or all of the data received from the error stream of
     * the subprocess.
     *
     * @param checker the exit code checker (must not be {@code null})
     * @param factory the exception factory (must not be {@code null})
     * @return this builder
     */
    ProcessBuilder<O> failOnExit(IntPredicate checker, Function<String, RuntimeException> factory);

    /**
     * Add a success exit code checker.
     * The checker returns {@code true} if the exit code matches this condition.
     * If so, the process execution will be considered a success.
     *
     * @param checker the exit code checker (must not be {@code null})
     * @return this builder
     */
    ProcessBuilder<O> succeedOnExit(IntPredicate checker);

    /**
     * Set the soft exit timeout.
     * This is the time to wait after all I/O is processed before gracefully terminating the subprocess.
     *
     * @param duration the soft exit timeout, or {@code null} for no timeout
     * @return this builder
     */
    ProcessBuilder<O> softExitTimeout(Duration duration);

    ProcessBuilder<O> hardExitTimeout(Duration duration);

    ProcessBuilder<O> whileRunning(Consumer<ProcessHandle> action);

    interface Input<O> extends ProcessBuilder<O> {
        Input<O> empty();

        Input<O> inherited();

        default Input<O> fromString(String string, Charset charset) {
            return fromWriterConsumer(w -> w.write(string), charset);
        }

        default Input<O> fromString(String string) {
            return fromWriterConsumer(w -> w.write(string));
        }

        default Input<O> fromStrings(Collection<?> strings) {
            return fromWriterConsumer(w -> {
                for (Object s : strings) {
                    w.write(String.valueOf(s));
                    w.write(System.lineSeparator());
                }
            });
        }

        default Input<O> fromStream(InputStream stream) {
            return fromStreamConsumer(stream::transferTo);
        }

        default Input<O> fromReader(Reader reader, Charset charset) {
            return fromWriterConsumer(reader::transferTo, charset);
        }

        default Input<O> fromReader(Reader reader) {
            return fromWriterConsumer(reader::transferTo);
        }

        Input<O> fromPath(Path path);

        Input<O> fromStreamConsumer(ExceptionConsumer<OutputStream, IOException> consumer);

        Input<O> fromWriterConsumer(ExceptionConsumer<Writer, IOException> consumer, Charset charset);

        Input<O> fromWriterConsumer(ExceptionConsumer<Writer, IOException> consumer);

        Input<O> timeout(Duration duration);
    }

    interface Output<O> extends ProcessBuilder<O> {
        Output<Void> discard();

        Output<Void> inherited();

        Output<String> asString(int maxChars);

        Output<List<String>> asStringList(int maxLines);

        default Output<Void> toStream(OutputStream stream) {
            Assert.checkNotNullParam("stream", stream);
            return toStreamConsumer(is -> is.transferTo(stream));
        }

        Output<Void> toStreamConsumer(ExceptionConsumer<InputStream, IOException> consumer);

        default Output<Void> toWriter(Writer writer, Charset charset) {
            Assert.checkNotNullParam("writer", writer);
            return toReaderConsumer(br -> br.transferTo(writer), charset);
        }

        default Output<Void> toWriter(Writer writer) {
            Assert.checkNotNullParam("writer", writer);
            return toReaderConsumer(br -> br.transferTo(writer));
        }

        Output<Void> toReaderConsumer(ExceptionConsumer<BufferedReader, IOException> consumer, Charset charset);

        Output<Void> toReaderConsumer(ExceptionConsumer<BufferedReader, IOException> consumer);

        <O2> Output<O2> processedStream(ExceptionFunction<InputStream, O2, IOException> processor);

        <O2> Output<O2> processedReader(ExceptionFunction<BufferedReader, O2, IOException> processor, Charset charset);

        <O2> Output<O2> processedReader(ExceptionFunction<BufferedReader, O2, IOException> processor);

        Output<Void> toPath(Path path);

        Output<O> timeout(Duration duration);

        /**
         * Push the output of this process into the input of another process,
         * returning a new builder for that process.
         * The returned process builder may not have its input configured,
         * because its input will always come from the output of the process built by this builder.
         * Running the final process in the pipeline will start the entire pipeline.
         * After calling this method, this process builder may no longer be modified
         * or executed; therefore, it must be the last method called when building a pipeline.
         *
         * @return the new process builder for the next stage (not {@code null})
         */
        ProcessBuilder<Void> pipeline();

        /**
         * Push the output of this process into the input of another process,
         * returning a new builder for that process.
         * The returned process builder may not have its input configured,
         * because its input will always come from the output of the process built by this builder.
         * Running the final process in the pipeline will start the entire pipeline.
         * After calling this method, this process builder may no longer be modified
         * or executed; therefore, it must be the last method called when building a pipeline.
         *
         * @param command the next stage command (must not be {@code null})
         * @return the new process builder for the next stage (not {@code null})
         */
        default ProcessBuilder<Void> pipeline(List<String> command) {
            return pipeline().command(command);
        }

        /**
         * Push the output of this process into the input of another process,
         * returning a new builder for that process.
         * The returned process builder may not have its input configured,
         * because its input will always come from the output of the process built by this builder.
         * Running the final process in the pipeline will start the entire pipeline.
         * After calling this method, this process builder may no longer be modified
         * or executed; therefore, it must be the last method called when building a pipeline.
         *
         * @param command the next stage command (must not be {@code null})
         * @return the new process builder for the next stage (not {@code null})
         */
        default ProcessBuilder<Void> pipeline(String... command) {
            return pipeline().command(command);
        }
    }

    interface Error<O> extends ProcessBuilder<O> {
        Error<O> discard();

        Error<O> inherited();

        /**
         * When {@code log} is {@code true} and process execution completes successfully,
         * any non-empty error lines will be logged at level {@code WARN}.
         *
         * @param log {@code true} to log error lines on success, or {@code false} to not log them
         * @return this builder
         */
        Error<O> logOnSuccess(boolean log);

        /**
         * When {@code include} is {@code true} and process execution fails,
         * some or all error lines will be included in the thrown exception.
         *
         * @param include {@code true} to include error lines in the thrown exception, or {@code false} not to
         * @return this builder
         */
        Error<O> includeOnFailure(boolean include);

        Error<O> toPath(Path path);

        default Error<O> toWriter(Writer writer, Charset charset) {
            Assert.checkNotNullParam("writer", writer);
            return toReaderConsumer(br -> br.transferTo(writer), charset);
        }

        default Error<O> toWriter(Writer writer) {
            Assert.checkNotNullParam("writer", writer);
            return toReaderConsumer(br -> br.transferTo(writer));
        }

        Error<O> toReaderConsumer(ExceptionConsumer<BufferedReader, IOException> errorConsumer, Charset charset);

        Error<O> toReaderConsumer(ExceptionConsumer<BufferedReader, IOException> errorConsumer);

        /**
         * Redirect error output to the output stream.
         * This implicitly sets {@link #logOnSuccess(boolean)} and {@link #includeOnFailure(boolean)}
         * to {@code false}.
         *
         * @return this builder
         */
        Error<O> redirect();
    }

    /**
     * The result of process execution.
     *
     * @param <O> the result output type
     */
    final class Result<O> {
        private final O output;
        private final int exitCode;

        private Result(final O output, final int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }

        public static <O> Result<O> of(O output, int exitCode) {
            return new Result<>(output, exitCode);
        }

        public static Result<Void> of(int exitCode) {
            return new Result<>(null, exitCode);
        }

        /**
         * {@return the output object, if any (may be {@code null})}
         */
        public O output() {
            return output;
        }

        /**
         * {@return the process exit code}
         */
        public int exitCode() {
            return exitCode;
        }
    }
}
