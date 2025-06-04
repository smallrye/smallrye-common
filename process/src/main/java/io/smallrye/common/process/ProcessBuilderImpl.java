package io.smallrye.common.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.os.OS;

final class ProcessBuilderImpl<O> implements ProcessBuilder<O> {

    final java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder();
    final ProcessBuilderImpl<?> prev;
    final int depth;
    final Path command;
    private volatile boolean locked;
    List<String> arguments = List.of();
    IntPredicate exitCodeChecker = e -> e == 0;
    Duration softExitTimeout = DEFAULT_SOFT_TIMEOUT;
    Duration hardExitTimeout = DEFAULT_HARD_TIMEOUT;
    private Input<O> input;
    ExceptionConsumer<Process, IOException> inputHandler;
    Charset inputCharset;
    private Output<O> output;
    ExceptionFunction<Process, O, IOException> outputHandler;
    Charset outputCharset;
    private Error<O> error;
    ExceptionConsumer<BufferedReader, IOException> errorHandler;
    Charset errorCharset;
    int errorLineLimit = 256;
    boolean errorLogOnSuccess = true;
    boolean errorGatherOnFail = true;
    int errorHeadLines = 5;
    int errorTailLines = 5;
    Consumer<WaitableProcessHandle> whileRunning;

    private ProcessBuilderImpl(ProcessBuilderImpl<?> prev, Path command) {
        // constructed for pipeline execution
        this.command = command;
        this.prev = prev;
        depth = prev.depth + 1;
        softExitTimeout = prev.softExitTimeout;
        hardExitTimeout = prev.hardExitTimeout;
        pb.redirectInput(Redirect.PIPE);
        pb.redirectOutput(Redirect.DISCARD);
        // error is captured in some form by default
        pb.redirectError(Redirect.PIPE);
    }

    public ProcessBuilderImpl(Path command) {
        this.command = command;
        prev = null;
        depth = 1;
        pb.redirectInput(Redirect.DISCARD.file());
        pb.redirectOutput(Redirect.DISCARD);
        // error is captured in some form by default
        pb.redirectError(Redirect.PIPE);
    }

    public ProcessBuilder<O> arguments(final List<String> arguments) {
        check();
        this.arguments = List.copyOf(arguments);
        return this;
    }

    public ProcessBuilder<O> directory(final Path directory) {
        check();
        pb.directory(Assert.checkNotNullParam("directory", directory).toFile());
        return this;
    }

    public ProcessBuilder<O> modifyEnvironment(final Consumer<Map<String, String>> action) {
        check();
        action.accept(pb.environment());
        return this;
    }

    public Input<O> input() {
        check();
        if (prev != null) {
            throw new UnsupportedOperationException(
                    "Input may not be reconfigured on this process builder because it is a stage in a pipeline");
        }
        Input<O> input = this.input;
        if (input == null) {
            input = this.input = new InputImpl();
        }
        return input;
    }

    public Output<O> output() {
        check();
        Output<O> output = this.output;
        if (output == null) {
            output = this.output = new OutputImpl();
        }
        return output;
    }

    public Error<O> error() {
        check();
        Error<O> error = this.error;
        if (error == null) {
            error = this.error = new ErrorImpl();
        }
        return error;
    }

    public ProcessBuilder<O> exitCodeChecker(final IntPredicate checker) {
        check();
        this.exitCodeChecker = Assert.checkNotNullParam("checker", checker);
        return this;
    }

    public ProcessBuilder<O> softExitTimeout(final Duration duration) {
        check();
        this.softExitTimeout = Assert.checkNotNullParam("duration", duration);
        return this;
    }

    public ProcessBuilder<O> hardExitTimeout(final Duration duration) {
        check();
        this.hardExitTimeout = Assert.checkNotNullParam("duration", duration);
        return this;
    }

    public ProcessBuilder<O> whileRunning(final Consumer<WaitableProcessHandle> action) {
        check();
        this.whileRunning = Assert.checkNotNullParam("action", action);
        return this;
    }

    public O run() {
        check();
        commitCommand();
        locked = true;
        return makeRunner().run();
    }

    private ProcessRunner<O> makeRunner() {
        return new ProcessRunner<O>(this, prev == null ? null : prev.makeRunner());
    }

    private void check() {
        if (locked) {
            throw new IllegalStateException("This builder can no longer be configured");
        }
    }

    public abstract sealed class ViewImpl implements ProcessBuilder<O> {
        public ProcessBuilder<O> arguments(final List<String> command) {
            return ProcessBuilderImpl.this.arguments(command);
        }

        public ProcessBuilder<O> directory(final Path directory) {
            return ProcessBuilderImpl.this.directory(directory);
        }

        public ProcessBuilder<O> modifyEnvironment(final Consumer<Map<String, String>> action) {
            return ProcessBuilderImpl.this.modifyEnvironment(action);
        }

        public Input<O> input() {
            return ProcessBuilderImpl.this.input();
        }

        public Output<O> output() {
            return ProcessBuilderImpl.this.output();
        }

        public Error<O> error() {
            return ProcessBuilderImpl.this.error();
        }

        public O run() throws AbnormalExitException {
            return ProcessBuilderImpl.this.run();
        }

        public ProcessBuilder<O> exitCodeChecker(final IntPredicate checker) {
            return ProcessBuilderImpl.this.exitCodeChecker(checker);
        }

        public ProcessBuilder<O> softExitTimeout(final Duration duration) {
            return ProcessBuilderImpl.this.softExitTimeout(duration);
        }

        public ProcessBuilder<O> hardExitTimeout(final Duration duration) {
            return ProcessBuilderImpl.this.hardExitTimeout(duration);
        }

        public ProcessBuilder<O> whileRunning(final Consumer<WaitableProcessHandle> action) {
            return ProcessBuilderImpl.this.whileRunning(action);
        }
    }

    public final class InputImpl extends ViewImpl implements Input<O> {
        public Input<O> empty() {
            check();
            pb.redirectInput(Redirect.DISCARD.file());
            inputHandler = null;
            return this;
        }

        public Input<O> inherited() {
            check();
            pb.redirectInput(Redirect.INHERIT);
            inputHandler = null;
            return this;
        }

        public Input<O> charset(final Charset charset) {
            check();
            inputCharset = Assert.checkNotNullParam("charset", charset);
            return this;
        }

        public Input<O> nativeCharset() {
            check();
            inputCharset = null;
            return this;
        }

        public Input<O> transferFrom(final Path path) {
            check();
            Assert.checkNotNullParam("path", path);
            if (path.getFileSystem() == FileSystems.getDefault()) {
                pb.redirectInput(Redirect.from(path.toFile()));
                inputHandler = null;
            } else {
                // exotic file
                produceBytesWith(os -> {
                    try (InputStream is = Files.newInputStream(path)) {
                        is.transferTo(os);
                    }
                });
            }
            return this;
        }

        public Input<O> produceBytesWith(final ExceptionConsumer<OutputStream, IOException> consumer) {
            check();
            Assert.checkNotNullParam("consumer", consumer);
            inputHandler = proc -> {
                try (OutputStream os = proc.getOutputStream()) {
                    consumer.accept(os);
                }
            };
            pb.redirectInput(Redirect.PIPE);
            return this;
        }

        public Input<O> produceWith(final ExceptionConsumer<Writer, IOException> consumer) {
            check();
            Assert.checkNotNullParam("consumer", consumer);
            inputHandler = proc -> {
                // evaluate charset late
                Charset inputCharset = ProcessBuilderImpl.this.inputCharset;
                if (inputCharset == null) {
                    try (BufferedWriter bw = proc.outputWriter()) {
                        consumer.accept(bw);
                    }
                } else {
                    try (BufferedWriter bw = proc.outputWriter(inputCharset)) {
                        consumer.accept(bw);
                    }
                }
            };
            pb.redirectInput(Redirect.PIPE);
            return this;
        }
    }

    // we do a lot of sketchy casting here for usability
    @SuppressWarnings("unchecked")
    public final class OutputImpl extends ViewImpl implements Output<O> {

        public Output<Void> discard() {
            check();
            outputHandler = null;
            pb.redirectOutput(Redirect.DISCARD);
            return (Output<Void>) this;
        }

        public Output<Void> inherited() {
            check();
            outputHandler = null;
            pb.redirectOutput(Redirect.INHERIT);
            return (Output<Void>) this;
        }

        public Output<O> charset(final Charset charset) {
            check();
            outputCharset = Assert.checkNotNullParam("charset", charset);
            return this;
        }

        public Output<O> nativeCharset() {
            check();
            outputCharset = null;
            return this;
        }

        public Output<String> toSingleString(final int maxChars) {
            check();
            return processWith(br -> {
                StringBuilder sb = new StringBuilder(Math.min(192, maxChars));
                int ch;
                for (int n = 0; n < maxChars; n++) {
                    ch = br.read();
                    if (ch == -1) {
                        return sb.toString();
                    }
                    sb.append((char) ch);
                }
                for (;;) {
                    long res = br.skip(Integer.MAX_VALUE);
                    if (res == 0) {
                        ch = br.read();
                        if (ch == -1) {
                            return sb.toString();
                        }
                    }
                }
            });
        }

        public Output<List<String>> toStringList(final int maxLines) {
            check();
            return processWith(br -> {
                ArrayList<String> list = new ArrayList<>(Math.min(maxLines, 16));
                String line;
                while ((line = br.readLine()) != null) {
                    list.add(line);
                }
                return List.copyOf(list);
            });
        }

        public <O2> Output<O2> processBytesWith(final ExceptionFunction<InputStream, O2, IOException> processor) {
            check();
            Assert.checkNotNullParam("processor", processor);
            pb.redirectOutput(Redirect.PIPE);
            outputHandler = proc -> {
                try (InputStream is = proc.getInputStream()) {
                    return (O) processor.apply(is);
                }
            };
            return (Output<O2>) this;
        }

        public <O2> Output<O2> processWith(final ExceptionFunction<BufferedReader, O2, IOException> processor) {
            check();
            Assert.checkNotNullParam("processor", processor);
            pb.redirectOutput(Redirect.PIPE);
            outputHandler = proc -> {
                Charset outputCharset = ProcessBuilderImpl.this.outputCharset;
                if (outputCharset == null) {
                    try (BufferedReader br = proc.inputReader()) {
                        return (O) processor.apply(br);
                    }
                } else {
                    try (BufferedReader br = proc.inputReader(outputCharset)) {
                        return (O) processor.apply(br);
                    }
                }
            };
            return (Output<O2>) this;
        }

        public Output<Void> transferTo(final Path path) {
            transferTo(path, false);
            return (Output<Void>) this;
        }

        public Output<Void> appendTo(final Path path) {
            transferTo(path, true);
            return (Output<Void>) this;
        }

        private void transferTo(Path path, boolean append) {
            check();
            Assert.checkNotNullParam("path", path);
            if (path.getFileSystem() == FileSystems.getDefault()) {
                pb.redirectOutput(append ? Redirect.appendTo(path.toFile()) : Redirect.to(path.toFile()));
                outputHandler = null;
            } else {
                // exotic file
                processBytesWith(is -> {
                    try (OutputStream os = append ? Files.newOutputStream(path, StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE, StandardOpenOption.APPEND) : Files.newOutputStream(path)) {
                        is.transferTo(os);
                    }
                    return null;
                });
            }
        }

        public PipelineBuilder<Void> pipeTo(final Path command) {
            check();
            pb.redirectOutput(Redirect.PIPE);
            commitCommand();
            outputHandler = null;
            locked = true;
            return new ProcessBuilderImpl<Void>(ProcessBuilderImpl.this, command);
        }
    }

    private void commitCommand() {
        if (OS.current() == OS.WINDOWS) {
            String fileNameString = command.getFileName().toString();
            if (fileNameString.endsWith(".bat") || fileNameString.endsWith(".cmd")) {
                // todo: wrap with cmd.exe/etc. with correct argument escaping
                throw new UnsupportedOperationException("Execution of batch scripts on Windows is not yet supported");
            }
        }
        Stream<String> stream = Stream.concat(Stream.of(command.toString()), arguments.stream());
        pb.command(stream.toList());
    }

    public final class ErrorImpl extends ViewImpl implements Error<O> {

        public Error<O> discard() {
            errorHandler = null;
            return this;
        }

        public Error<O> inherited() {
            consumeWith(br -> {
                String line;
                while ((line = br.readLine()) != null) {
                    System.err.println(line);
                }
            });
            return this;
        }

        public Error<O> charset(final Charset charset) {
            errorCharset = Assert.checkNotNullParam("charset", charset);
            return this;
        }

        public Error<O> nativeCharset() {
            errorCharset = null;
            return this;
        }

        public Error<O> logOnSuccess(final boolean log) {
            errorLogOnSuccess = log;
            return this;
        }

        public Error<O> gatherOnFail(final boolean gather) {
            errorGatherOnFail = gather;
            return this;
        }

        public Error<O> maxLineLength(final int characters) {
            errorLineLimit = characters;
            return this;
        }

        public Error<O> captureHeadLines(final int headLines) {
            errorHeadLines = headLines;
            return this;
        }

        public Error<O> captureTailLines(final int tailLines) {
            errorTailLines = tailLines;
            return this;
        }

        public Error<O> transferTo(final Path path) {
            consumeWith(br -> {
                try (BufferedWriter bw = Files.newBufferedWriter(path)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        bw.write(line);
                        bw.write(System.lineSeparator());
                    }
                }
            });
            return this;
        }

        public Error<O> appendTo(final Path path) {
            consumeWith(br -> {
                try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        bw.write(line);
                        bw.write(System.lineSeparator());
                    }
                }
            });
            return this;
        }

        public Error<O> consumeWith(final ExceptionConsumer<BufferedReader, IOException> processor) {
            Assert.checkNotNullParam("processor", processor);
            errorHandler = processor;
            return this;
        }

        public Error<O> redirect() {
            pb.redirectErrorStream(true);
            errorHandler = null;
            return this;
        }
    }
}
