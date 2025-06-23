package io.smallrye.common.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;

/**
 * A builder for the tail portion of a pipeline.
 *
 * @param <O> the output type
 */
public sealed interface PipelineBuilder<O> permits PipelineBuilder.Error, PipelineBuilder.Output, ProcessBuilder {
    /**
     * Set the arguments for this process execution.
     *
     * @param command the arguments for this process execution (must not be {@code null})
     * @return this builder
     */
    PipelineBuilder<O> arguments(List<String> command);

    /**
     * Set the arguments for this process execution.
     *
     * @param command the arguments for this process execution (must not be {@code null})
     * @return this builder
     */
    PipelineBuilder<O> arguments(String... command);

    /**
     * Set the working directory for this process execution.
     *
     * @param directory the working directory (must not be {@code null})
     * @return this builder
     */
    PipelineBuilder<O> directory(Path directory);

    /**
     * Set the environment for this process execution, overriding the inherited environment.
     *
     * @param newEnvironment the new environment (must not be {@code null})
     * @return this builder
     */
    PipelineBuilder<O> environment(Map<String, String> newEnvironment);

    /**
     * Allow the given action to modify the environment of the subprocess.
     * The action is provided a mutable map which can be arbitrarily modified
     * for the duration of the callback.
     * Modifying the map after the callback returns will result in undefined behavior
     * and should not be attempted.
     *
     * @param action the action (must not be {@code null})
     * @return this builder
     */
    PipelineBuilder<O> modifyEnvironment(Consumer<Map<String, String>> action);

    /**
     * Configure the output handling of the process.
     *
     * @return the output configuration view of this builder (not {@code null})
     */
    Output<O> output();

    /**
     * Configure the error output handling of the process.
     *
     * @return the error output configuration view of this builder (not {@code null})
     */
    Error<O> error();

    /**
     * Run the process or pipeline.
     *
     * @return the result of the execution (may be {@code null})
     */
    O run();

    /**
     * Add a failure exit code checker.
     * The checker returns {@code true} if the process execution should fail for this exit code.
     * If so, a {@code ProcessException} will be constructed and thrown.
     * The exception will generally include some or all of the data received from the error stream of
     * the subprocess.
     *
     * @param checker the exit code checker (must not be {@code null})
     * @return this builder
     */
    PipelineBuilder<O> exitCodeChecker(IntPredicate checker);

    /**
     * Set the while-running process handler.
     *
     * @param action the action to run (must not be {@code null})
     * @return this builder
     */
    PipelineBuilder<O> whileRunning(Consumer<WaitableProcessHandle> action);

    /**
     * The output handling aspect of the process builder.
     *
     * @param <O> the output type
     */
    sealed interface Output<O> extends PipelineBuilder<O> permits ProcessBuilderImpl.OutputImpl {
        /**
         * Instruct the builder to discard the output of this process.
         *
         * @return this builder
         */
        Output<Void> discard();

        /**
         * Instruct the builder to inherit the process output from the current process.
         *
         * @return this builder
         */
        Output<Void> inherited();

        /**
         * Set the character set for output character data.
         *
         * @param charset the character set (must not be {@code null})
         * @return this builder
         */
        Output<O> charset(Charset charset);

        /**
         * Instruct the builder to use the native output character set for output character data.
         *
         * @return this builder
         */
        default Output<O> nativeCharset() {
            return charset(ProcessUtil.nativeCharset());
        }

        /**
         * Instruct the builder to return the output of the process as a single string.
         *
         * @param maxChars the maximum number of characters for the returned string
         * @return this builder
         */
        Output<String> toSingleString(int maxChars);

        /**
         * Instruct the builder to return the output of the process as a list of strings.
         *
         * @param maxLines the maximum number of lines for the returned list
         * @param maxLineLength the maximum number of characters allowed per line
         * @return this builder
         */
        Output<List<String>> toStringList(int maxLines, int maxLineLength);

        /**
         * When {@code include} is {@code true} and process execution fails,
         * some or all output lines will be gathered to be included in the thrown exception.
         * The default is {@code false}.
         *
         * @param gather {@code true} to include output lines in the thrown exception, or {@code false} not to
         * @return this builder
         */
        Output<O> gatherOnFail(boolean gather);

        /**
         * Limit the maximum length of each captured line to the given number of characters.
         * The line terminator is not included in the count.
         *
         * @param characters the maximum number of characters
         * @return this builder
         * @throws IllegalArgumentException if the number of characters is less than 1
         */
        Output<O> maxCaptureLineLength(int characters);

        /**
         * Set the number of "head" or leading lines of output to capture for exception messages.
         *
         * @param headLines the number of head lines
         * @return this builder
         */
        Output<O> captureHeadLines(int headLines);

        /**
         * Set the number of "tail" or trailing lines of output to capture for exception messages.
         *
         * @param tailLines the number of tail lines
         * @return this builder
         */
        Output<O> captureTailLines(int tailLines);

        /**
         * Instruct the builder to transfer the output of the process to the given stream.
         * The stream is not closed.
         * The character set is not used for this output mode.
         *
         * @param stream the output stream to transfer to (must not be {@code null})
         * @return this builder
         */
        default Output<Void> transferTo(OutputStream stream) {
            Assert.checkNotNullParam("stream", stream);
            return consumeBytesWith(is -> is.transferTo(stream));
        }

        /**
         * Instruct the builder to transfer a copy of the output of the process to the given stream.
         * The stream is not closed.
         * The character set is not used for this output mode.
         *
         * @param stream the output stream to transfer to (must not be {@code null})
         * @return this builder
         */
        default Output<O> copyAndTransferTo(OutputStream stream) {
            Assert.checkNotNullParam("stream", stream);
            return copyAndConsumeBytesWith(is -> is.transferTo(stream));
        }

        /**
         * Instruct the builder to transfer the output of the process to the given writer.
         * The writer is not closed.
         *
         * @param writer the writer to transfer to (must not be {@code null})
         * @return this builder
         */
        default Output<Void> transferTo(Writer writer) {
            Assert.checkNotNullParam("writer", writer);
            return consumeWith(br -> br.transferTo(writer));
        }

        /**
         * Instruct the builder to transfer a copy of the output of the process to the given writer.
         * The writer is not closed.
         *
         * @param writer the writer to transfer to (must not be {@code null})
         * @return this builder
         */
        default Output<O> copyAndTransferTo(Writer writer) {
            Assert.checkNotNullParam("writer", writer);
            return copyAndConsumeWith(is -> is.transferTo(writer));
        }

        /**
         * Instruct the builder to transfer the output of the process to the given path.
         * The file at the given path will be truncated if it exists.
         * The character set is not used for this output mode.
         *
         * @param path the path to transfer to (must not be {@code null})
         * @return this builder
         */
        Output<Void> transferTo(Path path);

        /**
         * Instruct the builder to transfer a copy of the output of the process to the given path.
         * The file at the given path will be truncated if it exists.
         * The character set is not used for this output mode.
         *
         * @param path the path to transfer to (must not be {@code null})
         * @return this builder
         */
        default Output<O> copyAndTransferTo(Path path) {
            Assert.checkNotNullParam("path", path);
            return copyAndConsumeBytesWith(is -> {
                try (OutputStream os = Files.newOutputStream(path)) {
                    is.transferTo(os);
                }
            });
        }

        /**
         * Instruct the builder to transfer the output of the process to the given path.
         * The file at the given path will be appended to if it exists.
         * The character set is not used for this output mode.
         *
         * @param path the path to transfer to (must not be {@code null})
         * @return this builder
         */
        Output<Void> appendTo(Path path);

        /**
         * Instruct the builder to transfer a copy of the output of the process to the given path.
         * The file at the given path will be appended to if it exists.
         * The character set is not used for this output mode.
         *
         * @param path the path to transfer to (must not be {@code null})
         * @return this builder
         */
        default Output<O> copyAndAppendTo(Path path) {
            Assert.checkNotNullParam("path", path);
            return copyAndConsumeBytesWith(is -> {
                try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND)) {
                    is.transferTo(os);
                }
            });
        }

        /**
         * Instruct the builder to consume the bytes of the output with the given consumer.
         * The character set is not used for this output mode.
         *
         * @param consumer the consumer (must not be {@code null})
         * @return this builder
         */
        default Output<Void> consumeBytesWith(ExceptionConsumer<InputStream, IOException> consumer) {
            Assert.checkNotNullParam("consumer", consumer);
            return processBytesWith(is -> {
                consumer.accept(is);
                return null;
            });
        }

        /**
         * Instruct the builder to consume a copy of the bytes of the output with the given consumer.
         * The character set is not used for this output mode.
         *
         * @param consumer the consumer (must not be {@code null})
         * @return this builder
         */
        Output<O> copyAndConsumeBytesWith(ExceptionConsumer<InputStream, IOException> consumer);

        /**
         * Instruct the builder to consume the output with the given consumer.
         *
         * @param consumer the consumer (must not be {@code null})
         * @return this builder
         */
        default Output<Void> consumeWith(ExceptionConsumer<BufferedReader, IOException> consumer) {
            Assert.checkNotNullParam("consumer", consumer);
            return processWith(rd -> {
                consumer.accept(rd);
                return null;
            });
        }

        /**
         * Instruct the builder to consume a copy of the output with the given consumer.
         *
         * @param consumer the consumer (must not be {@code null})
         * @return this builder
         */
        Output<O> copyAndConsumeWith(ExceptionConsumer<BufferedReader, IOException> consumer);

        /**
         * Instruct the builder to consume each line of the output with the given consumer.
         *
         * @param maxLineLength the maximum number of characters allowed per line
         * @param consumer the consumer (must not be {@code null})
         * @return this builder
         */
        default Output<Void> consumeLinesWith(int maxLineLength, ExceptionConsumer<String, IOException> consumer) {
            Assert.checkMinimumParameter("maxLineLength", 1, maxLineLength);
            Assert.checkNotNullParam("consumer", consumer);
            return consumeWith(br -> {
                LineReader lr = new LineReader(br, maxLineLength);
                String line;
                while ((line = lr.readLine()) != null) {
                    consumer.accept(line);
                }
            });
        }

        /**
         * Instruct the builder to consume a copy of each line of the output with the given consumer.
         *
         * @param maxLineLength the maximum number of characters allowed per line
         * @param consumer the consumer (must not be {@code null})
         * @return this builder
         */
        default Output<O> copyAndConsumeLinesWith(int maxLineLength, ExceptionConsumer<String, IOException> consumer) {
            Assert.checkNotNullParam("consumer", consumer);
            return copyAndConsumeWith(br -> {
                LineReader lr = new LineReader(br, maxLineLength);
                String line;
                while ((line = lr.readLine()) != null) {
                    consumer.accept(line);
                }
            });
        }

        /**
         * Instruct the builder to process the bytes of the output with the given function,
         * whose return value is passed to the caller of {@link PipelineBuilder#run}.
         * The character set is not used for this output mode.
         *
         * @param processor the processor (must not be {@code null})
         * @return this builder
         * @param <O2> the new output type
         */
        <O2> Output<O2> processBytesWith(ExceptionFunction<InputStream, O2, IOException> processor);

        /**
         * Instruct the builder to process the output with the given function,
         * whose return value is passed to the caller of {@link PipelineBuilder#run}.
         *
         * @param processor the processor (must not be {@code null})
         * @return this builder
         * @param <O2> the new output type
         */
        <O2> Output<O2> processWith(ExceptionFunction<BufferedReader, O2, IOException> processor);

        /**
         * Push the output of this process into the input of another process,
         * returning a new builder for that process.
         * The returned process builder cannot have its input configured,
         * because its input will always come from the output of the process built by this builder.
         * Running the final process in the pipeline will start the entire pipeline.
         * After calling this method, this process builder may no longer be modified
         * or executed; therefore, it must be the last method called when building a pipeline.
         * The character set is not used for this output mode.
         *
         * @param command the next stage command (must not be {@code null})
         * @return the new process builder for the next stage (not {@code null})
         */
        PipelineBuilder<Void> pipeTo(Path command);

        /**
         * Push the output of this process into the input of another process,
         * returning a new builder for that process.
         * The returned process builder may not have its input configured,
         * because its input will always come from the output of the process built by this builder.
         * Running the final process in the pipeline will start the entire pipeline.
         * After calling this method, this process builder may no longer be modified
         * or executed; therefore, it must be the last method called when building a pipeline.
         * The character set is not used for this output mode.
         *
         * @param command the next stage command (must not be {@code null})
         * @param args the next stage arguments (must not be {@code null})
         * @return the new process builder for the next stage (not {@code null})
         */
        default PipelineBuilder<Void> pipeTo(Path command, List<String> args) {
            return pipeTo(command).arguments(args);
        }

        /**
         * Push the output of this process into the input of another process,
         * returning a new builder for that process.
         * The returned process builder may not have its input configured,
         * because its input will always come from the output of the process built by this builder.
         * Running the final process in the pipeline will start the entire pipeline.
         * After calling this method, this process builder may no longer be modified
         * or executed; therefore, it must be the last method called when building a pipeline.
         * The character set is not used for this output mode.
         *
         * @param command the next stage command (must not be {@code null})
         * @param args the next stage arguments (must not be {@code null})
         * @return the new process builder for the next stage (not {@code null})
         */
        default PipelineBuilder<Void> pipeTo(Path command, String... args) {
            return pipeTo(command).arguments(args);
        }
    }

    /**
     * The error handling aspect of the process builder.
     *
     * @param <O> the output type
     */
    sealed interface Error<O> extends PipelineBuilder<O> permits ProcessBuilderImpl.ErrorImpl {
        /**
         * Instruct the builder to discard the error output.
         *
         * @return this builder
         */
        Error<O> discard();

        /**
         * Instruct the builder to copy the error output of the process being built
         * to the error output of the current process.
         *
         * @return this builder
         */
        Error<O> inherited();

        /**
         * Set the character set to use for error output.
         *
         * @param charset the character set (must not be {@code null})
         * @return this builder
         */
        Error<O> charset(Charset charset);

        /**
         * Instruct the builder to use the native character set for error output
         *
         * @return this builder
         */
        default Error<O> nativeCharset() {
            return charset(ProcessUtil.nativeCharset());
        }

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
         * some or all error lines will be gathered to be included in the thrown exception.
         * The default is {@code true}.
         *
         * @param gather {@code true} to include error lines in the thrown exception, or {@code false} not to
         * @return this builder
         */
        Error<O> gatherOnFail(boolean gather);

        /**
         * Limit the maximum length of each captured line to the given number of characters.
         * The line terminator is not included in the count.
         *
         * @param characters the maximum number of characters
         * @return this builder
         * @throws IllegalArgumentException if the number of characters is less than 1
         */
        Error<O> maxCaptureLineLength(int characters);

        /**
         * Set the number of "head" or leading lines of error to capture for exception messages.
         *
         * @param headLines the number of head lines
         * @return this builder
         */
        Error<O> captureHeadLines(int headLines);

        /**
         * Set the number of "tail" or trailing lines of error to capture for exception messages.
         *
         * @param tailLines the number of tail lines
         * @return this builder
         */
        Error<O> captureTailLines(int tailLines);

        /**
         * Instruct the builder to transfer the error output of the process to the given stream.
         *
         * @param writer the output stream to transfer to (must not be {@code null})
         * @return this builder
         */
        default Error<O> transferTo(OutputStream stream) {
            Assert.checkNotNullParam("stream", stream);
            return consumeBytesWith(is -> is.transferTo(stream));
        }

        /**
         * Instruct the builder to transfer a copy of the error output of the process to the given stream.
         *
         * @param writer the output stream to transfer to (must not be {@code null})
         * @return this builder
         */
        default Error<O> copyAndTransferTo(OutputStream stream) {
            Assert.checkNotNullParam("stream", stream);
            return copyAndConsumeBytesWith(is -> is.transferTo(stream));
        }

        /**
         * Instruct the builder to transfer the error output of the process to the given writer.
         *
         * @param writer the writer to transfer to (must not be {@code null})
         * @return this builder
         */
        default Error<O> transferTo(Writer writer) {
            Assert.checkNotNullParam("writer", writer);
            return consumeWith(br -> br.transferTo(writer));
        }

        /**
         * Instruct the builder to transfer a copy of the error output of the process to the given writer.
         *
         * @param writer the writer to transfer to (must not be {@code null})
         * @return this builder
         */
        default Error<O> copyAndTransferTo(Writer writer) {
            Assert.checkNotNullParam("writer", writer);
            return copyAndConsumeWith(br -> br.transferTo(writer));
        }

        /**
         * Instruct the builder to transfer the error output of the process to the given path.
         * The file at the given path will be truncated if it exists.
         *
         * @param path the path to transfer to (must not be {@code null})
         * @return this builder
         */
        Error<O> transferTo(Path path);

        /**
         * Instruct the builder to transfer a copy of the error output of the process to the given path.
         * The file at the given path will be truncated if it exists.
         *
         * @param path the path to transfer to (must not be {@code null})
         * @return this builder
         */
        default Error<O> copyAndTransferTo(Path path) {
            Assert.checkNotNullParam("path", path);
            return copyAndConsumeBytesWith(is -> {
                try (OutputStream os = Files.newOutputStream(path)) {
                    is.transferTo(os);
                }
            });
        }

        /**
         * Instruct the builder to transfer the error output of the process to the given path.
         * The file at the given path will be appended to if it exists.
         *
         * @param path the path to transfer to (must not be {@code null})
         * @return this builder
         */
        Error<O> appendTo(Path path);

        /**
         * Instruct the builder to transfer a copy of the error output of the process to the given path.
         * The file at the given path will be appended to if it exists.
         *
         * @param path the path to transfer to (must not be {@code null})
         * @return this builder
         */
        default Error<O> copyAndAppendTo(Path path) {
            Assert.checkNotNullParam("path", path);
            return copyAndConsumeBytesWith(is -> {
                try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND)) {
                    is.transferTo(os);
                }
            });
        }

        /**
         * Instruct the builder to consume the bytes of the error output with the given consumer.
         *
         * @param errorConsumer the consumer (must not be {@code null})
         * @return this builder
         */
        Error<O> consumeBytesWith(ExceptionConsumer<InputStream, IOException> consumer);

        /**
         * Instruct the builder to consume a copy of the bytes of the error output with the given consumer.
         *
         * @param errorConsumer the consumer (must not be {@code null})
         * @return this builder
         */
        Error<O> copyAndConsumeBytesWith(ExceptionConsumer<InputStream, IOException> consumer);

        /**
         * Instruct the builder to consume the error output with the given consumer.
         *
         * @param errorConsumer the consumer (must not be {@code null})
         * @return this builder
         */
        Error<O> consumeWith(ExceptionConsumer<BufferedReader, IOException> errorConsumer);

        /**
         * Instruct the builder to consume a copy of the error output with the given consumer.
         *
         * @param errorConsumer the consumer (must not be {@code null})
         * @return this builder
         */
        Error<O> copyAndConsumeWith(ExceptionConsumer<BufferedReader, IOException> errorConsumer);

        /**
         * Instruct the builder to consume each line of the error output with the given consumer.
         *
         * @param maxLineLength the maximum number of characters allowed per line
         * @param consumer the consumer (must not be {@code null})
         * @return this builder
         */
        default Error<O> consumeLinesWith(int maxLineLength, ExceptionConsumer<String, IOException> consumer) {
            Assert.checkMinimumParameter("maxLineLength", 1, maxLineLength);
            Assert.checkNotNullParam("consumer", consumer);
            return consumeWith(br -> {
                LineReader lr = new LineReader(br, maxLineLength);
                String line;
                while ((line = lr.readLine()) != null) {
                    consumer.accept(line);
                }
            });
        }

        /**
         * Instruct the builder to consume each line of a copy of the error output with the given consumer.
         *
         * @param maxLineLength the maximum number of characters allowed per line
         * @param consumer the consumer (must not be {@code null})
         * @return this builder
         */
        default Error<O> copyAndConsumeLinesWith(int maxLineLength, ExceptionConsumer<String, IOException> consumer) {
            Assert.checkMinimumParameter("maxLineLength", 1, maxLineLength);
            Assert.checkNotNullParam("consumer", consumer);
            return copyAndConsumeWith(br -> {
                LineReader lr = new LineReader(br, maxLineLength);
                String line;
                while ((line = lr.readLine()) != null) {
                    consumer.accept(line);
                }
            });
        }

        /**
         * Redirect error output to the output stream, overriding all other output considerations.
         * This implicitly sets {@link #logOnSuccess(boolean)} and {@link #gatherOnFail(boolean)}
         * to {@code false} and ignores all {@code copyAnd...()} handlers.
         *
         * @return this builder
         */
        Error<O> redirect();
    }
}
