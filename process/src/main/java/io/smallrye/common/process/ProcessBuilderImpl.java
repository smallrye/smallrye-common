package io.smallrye.common.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.os.OS;

final class ProcessBuilderImpl<O> implements ProcessBuilder<O> {

    static final int IN_EMPTY = 0;
    static final int IN_INHERIT = 1;
    static final int IN_FILE = 2;
    static final int IN_HANDLER = 3;
    static final int IN_PIPELINE = 4;
    static final int IN_PIPELINE_SPLIT = 5;

    static final int OUT_DISCARD = 0;
    static final int OUT_INHERIT = 1;
    static final int OUT_FILE_WRITE = 2;
    static final int OUT_FILE_APPEND = 3;
    static final int OUT_HANDLER = 4;
    static final int OUT_PIPELINE = 5;
    static final int OUT_PIPELINE_SPLIT = 6;

    static final int ERR_DISCARD = 0;
    static final int ERR_INHERIT = 1;
    static final int ERR_FILE_WRITE = 2;
    static final int ERR_FILE_APPEND = 3;
    static final int ERR_HANDLER = 4;
    static final int ERR_REDIRECT = 5;

    final ProcessBuilderImpl<O> prev;
    final int depth;
    final Path command;
    final ArgumentRule argumentRule;
    File directory;
    private volatile boolean locked;
    List<String> arguments = List.of();
    IntPredicate exitCodeChecker = e -> e == 0;
    Duration softExitTimeout = DEFAULT_SOFT_TIMEOUT;
    Duration hardExitTimeout = DEFAULT_HARD_TIMEOUT;
    private Input<O> input;
    int inputStrategy = IN_EMPTY;
    ExceptionConsumer<OutputStream, IOException> inputHandler;
    Charset inputCharset = ProcessUtil.nativeCharset();
    File inputFile;
    private Output<O> output;
    int outputStrategy = OUT_DISCARD;
    ExceptionFunction<InputStream, O, IOException> outputHandler;
    List<ExceptionConsumer<InputStream, IOException>> extraOutputHandlers = List.of();
    Charset outputCharset = ProcessUtil.nativeCharset();
    int outputLineLimit = 256;
    boolean outputGatherOnFail = false;
    int outputHeadLines = 5;
    int outputTailLines = 5;
    File outputFile;
    private Error<O> error;
    int errorStrategy = ERR_DISCARD;
    ExceptionConsumer<InputStream, IOException> errorHandler;
    List<ExceptionConsumer<InputStream, IOException>> extraErrorHandlers = List.of();
    Charset errorCharset = ProcessUtil.nativeCharset();
    int errorLineLimit = 256;
    boolean errorLogOnSuccess = true;
    boolean errorGatherOnFail = true;
    int errorHeadLines = 5;
    int errorTailLines = 5;
    File errorFile;
    Consumer<WaitableProcessHandle> whileRunning;
    // we create this very early so we can access the env map
    final java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder();

    private ProcessBuilderImpl(ProcessBuilderImpl<O> prev, Path command) {
        // constructed for pipeline execution
        this.command = command;
        this.argumentRule = computeRule(command);
        this.prev = prev;
        depth = prev.depth + 1;
        softExitTimeout = prev.softExitTimeout;
        hardExitTimeout = prev.hardExitTimeout;
        pb.environment().clear();
        pb.environment().putAll(prev.pb.environment());
        directory = prev.directory;
        inputStrategy = prev.outputStrategy == OUT_PIPELINE_SPLIT ? IN_PIPELINE_SPLIT : IN_PIPELINE;
    }

    ProcessBuilderImpl(Path command) {
        this.command = command;
        this.argumentRule = computeRule(command);
        prev = null;
        depth = 1;
    }

    public ProcessBuilder<O> arguments(final List<String> arguments) {
        check();
        argumentRule.checkArguments(arguments);
        this.arguments = List.copyOf(arguments);
        return this;
    }

    public ProcessBuilder<O> directory(final Path directory) {
        check();
        this.directory = Assert.checkNotNullParam("directory", directory).toFile();
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
        locked = true;
        return makeRunner().run();
    }

    public CompletableFuture<O> runAsync() {
        check();
        locked = true;
        return makeRunner().runAsync();
    }

    private ProcessRunner<O> makeRunner() {
        return new ProcessRunner<O>(this, prev == null ? null : prev.makePipelineRunner());
    }

    private PipelineRunner<O> makePipelineRunner() {
        return new PipelineRunner<>(this, prev == null ? null : prev.makePipelineRunner());
    }

    private void check() {
        if (locked) {
            throw new IllegalStateException("This builder can no longer be configured");
        }
    }

    private static ArgumentRule computeRule(final Path command) {
        return switch (OS.current()) {
            case WINDOWS -> {
                String cmdFileName = command.getFileName().toString().toLowerCase(Locale.ROOT);
                if (cmdFileName.endsWith(".bat") || cmdFileName.endsWith(".cmd")) {
                    yield ArgumentRule.BATCH;
                } else if (cmdFileName.endsWith(".ps1")) {
                    yield ArgumentRule.POWERSHELL;
                } else {
                    yield ArgumentRule.DEFAULT;
                }
            }
            default -> ArgumentRule.DEFAULT;
        };
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

        public CompletableFuture<O> runAsync() {
            return ProcessBuilderImpl.this.runAsync();
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
            inputStrategy = IN_EMPTY;
            inputHandler = null;
            return this;
        }

        public Input<O> inherited() {
            check();
            inputStrategy = IN_INHERIT;
            inputHandler = null;
            return this;
        }

        public Input<O> charset(final Charset charset) {
            check();
            inputCharset = Assert.checkNotNullParam("charset", charset);
            return this;
        }

        public Input<O> transferFrom(final Path path) {
            check();
            Assert.checkNotNullParam("path", path);
            if (path.getFileSystem() == FileSystems.getDefault()) {
                inputStrategy = IN_FILE;
                inputFile = path.toFile();
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
            inputStrategy = IN_HANDLER;
            inputHandler = consumer;
            return this;
        }

        public Input<O> produceWith(final ExceptionConsumer<Writer, IOException> consumer) {
            check();
            Assert.checkNotNullParam("consumer", consumer);
            produceBytesWith(os -> {
                try (OutputStreamWriter osw = new OutputStreamWriter(os, inputCharset)) {
                    try (BufferedWriter bw = new BufferedWriter(osw)) {
                        consumer.accept(bw);
                    }
                }
            });
            return this;
        }
    }

    // we do a lot of sketchy casting here for usability
    @SuppressWarnings("unchecked")
    public final class OutputImpl extends ViewImpl implements Output<O> {

        public Output<Void> discard() {
            check();
            outputHandler = null;
            outputStrategy = OUT_DISCARD;
            return (Output<Void>) this;
        }

        public Output<Void> inherited() {
            check();
            outputHandler = null;
            outputStrategy = OUT_INHERIT;
            return (Output<Void>) this;
        }

        public Output<O> charset(final Charset charset) {
            check();
            outputCharset = Assert.checkNotNullParam("charset", charset);
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
                IOUtil.drain(br);
                return sb.toString();
            });
        }

        public Output<List<String>> toStringList(final int maxLines, final int maxLineLength) {
            Assert.checkMinimumParameter("maxLines", 1, maxLines);
            Assert.checkMinimumParameter("maxLineLength", 1, maxLineLength);
            check();
            return processWith(br -> {
                LineReader lr = new LineReader(br, maxLineLength);
                ArrayList<String> list = new ArrayList<>(Math.min(16, maxLines));
                String line;
                while ((line = lr.readLine()) != null) {
                    list.add(line);
                    if (list.size() == maxLines) {
                        IOUtil.drain(br);
                        break;
                    }
                }
                return List.copyOf(list);
            });
        }

        public Output<O> gatherOnFail(final boolean gather) {
            check();
            outputGatherOnFail = gather;
            return this;
        }

        public Output<O> maxCaptureLineLength(final int characters) {
            Assert.checkMinimumParameter("characters", 1, characters);
            check();
            outputLineLimit = characters;
            return this;
        }

        public Output<O> captureHeadLines(final int headLines) {
            Assert.checkMinimumParameter("headLines", 0, headLines);
            check();
            outputHeadLines = headLines;
            return this;
        }

        public Output<O> captureTailLines(final int tailLines) {
            Assert.checkMinimumParameter("tailLines", 0, tailLines);
            check();
            outputTailLines = tailLines;
            return this;
        }

        public <O2> Output<O2> processBytesWith(final ExceptionFunction<InputStream, O2, IOException> processor) {
            check();
            Assert.checkNotNullParam("processor", processor);
            outputStrategy = OUT_HANDLER;
            outputHandler = (ExceptionFunction<InputStream, O, IOException>) processor;
            return (Output<O2>) this;
        }

        public <O2> Output<O2> processWith(final ExceptionFunction<BufferedReader, O2, IOException> processor) {
            check();
            Assert.checkNotNullParam("processor", processor);
            processBytesWith(is -> {
                try (InputStreamReader isr = new InputStreamReader(is, outputCharset)) {
                    try (BufferedReader br = new BufferedReader(isr)) {
                        return (O) processor.apply(br);
                    }
                }
            });
            return (Output<O2>) this;
        }

        public Output<Void> transferTo(final Path path) {
            transferTo(path, OUT_FILE_WRITE);
            return (Output<Void>) this;
        }

        public Output<Void> appendTo(final Path path) {
            transferTo(path, OUT_FILE_APPEND);
            return (Output<Void>) this;
        }

        public Output<O> copyAndConsumeBytesWith(final ExceptionConsumer<InputStream, IOException> consumer) {
            Assert.checkNotNullParam("consumer", consumer);
            List<ExceptionConsumer<InputStream, IOException>> handlers = extraOutputHandlers;
            switch (handlers.size()) {
                case 0 -> extraOutputHandlers = List.of(consumer);
                case 1 -> extraOutputHandlers = List.of(extraOutputHandlers.get(0), consumer);
                case 2 -> {
                    extraOutputHandlers = new ArrayList<>();
                    extraOutputHandlers.addAll(handlers);
                    extraOutputHandlers.add(consumer);
                }
                default -> extraOutputHandlers.add(consumer);
            }
            return this;
        }

        public Output<O> copyAndConsumeWith(final ExceptionConsumer<BufferedReader, IOException> consumer) {
            Assert.checkNotNullParam("consumer", consumer);
            return copyAndConsumeBytesWith(is -> IOUtil.consumeToReader(is, consumer, outputCharset));
        }

        private void transferTo(Path path, int strategy) {
            check();
            Assert.checkNotNullParam("path", path);
            ExceptionFunction<InputStream, O, IOException> handler = switch (strategy) {
                case OUT_FILE_WRITE -> is -> {
                    try (OutputStream os = Files.newOutputStream(path)) {
                        is.transferTo(os);
                    }
                    return null;
                };
                case OUT_FILE_APPEND -> is -> {
                    try (OutputStream os = Files.newOutputStream(path,
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                        is.transferTo(os);
                    }
                    return null;
                };
                default -> throw Assert.impossibleSwitchCase(strategy);
            };
            if (path.getFileSystem() == FileSystems.getDefault()) {
                outputStrategy = strategy;
                outputFile = path.toFile();
                outputHandler = handler;
            } else {
                // exotic file
                processBytesWith(handler);
            }
        }

        public PipelineBuilder<Void> pipeTo(final Path command) {
            check();
            locked = true;
            outputHandler = null;
            outputStrategy = extraOutputHandlers.isEmpty() && !outputGatherOnFail ? OUT_PIPELINE : OUT_PIPELINE_SPLIT;
            return new ProcessBuilderImpl<>((ProcessBuilderImpl<Void>) ProcessBuilderImpl.this, command);
        }
    }

    public final class ErrorImpl extends ViewImpl implements Error<O> {

        public Error<O> discard() {
            check();
            errorStrategy = ERR_DISCARD;
            errorHandler = null;
            return this;
        }

        public Error<O> inherited() {
            check();
            errorStrategy = ERR_INHERIT;
            errorHandler = is -> is.transferTo(System.err);
            return this;
        }

        public Error<O> charset(final Charset charset) {
            check();
            errorCharset = Assert.checkNotNullParam("charset", charset);
            return this;
        }

        public Error<O> logOnSuccess(final boolean log) {
            check();
            errorLogOnSuccess = log;
            return this;
        }

        public Error<O> gatherOnFail(final boolean gather) {
            check();
            errorGatherOnFail = gather;
            return this;
        }

        public Error<O> maxCaptureLineLength(final int characters) {
            check();
            Assert.checkMinimumParameter("characters", 1, characters);
            errorLineLimit = characters;
            return this;
        }

        public Error<O> captureHeadLines(final int headLines) {
            check();
            errorHeadLines = headLines;
            return this;
        }

        public Error<O> captureTailLines(final int tailLines) {
            check();
            errorTailLines = tailLines;
            return this;
        }

        public Error<O> transferTo(final Path path) {
            Assert.checkNotNullParam("path", path);
            consumeBytesWith(is -> {
                try (OutputStream os = Files.newOutputStream(path)) {
                    is.transferTo(os);
                }
            });
            return this;
        }

        public Error<O> appendTo(final Path path) {
            Assert.checkNotNullParam("path", path);
            consumeBytesWith(is -> {
                try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE)) {
                    is.transferTo(os);
                }
            });
            return this;
        }

        public Error<O> consumeBytesWith(final ExceptionConsumer<InputStream, IOException> consumer) {
            check();
            Assert.checkNotNullParam("consumer", consumer);
            errorStrategy = ERR_HANDLER;
            errorHandler = consumer;
            return this;
        }

        public Error<O> copyAndConsumeBytesWith(final ExceptionConsumer<InputStream, IOException> consumer) {
            check();
            Assert.checkNotNullParam("consumer", consumer);
            List<ExceptionConsumer<InputStream, IOException>> handlers = extraErrorHandlers;
            switch (handlers.size()) {
                case 0 -> extraErrorHandlers = List.of(consumer);
                case 1 -> extraErrorHandlers = List.of(extraErrorHandlers.get(0), consumer);
                case 2 -> {
                    extraErrorHandlers = new ArrayList<>();
                    extraErrorHandlers.addAll(handlers);
                    extraErrorHandlers.add(consumer);
                }
                default -> extraErrorHandlers.add(consumer);
            }
            return this;
        }

        public Error<O> consumeWith(final ExceptionConsumer<BufferedReader, IOException> consumer) {
            Assert.checkNotNullParam("consumer", consumer);
            consumeBytesWith(is -> IOUtil.consumeToReader(is, consumer, errorCharset));
            return this;
        }

        public Error<O> copyAndConsumeWith(final ExceptionConsumer<BufferedReader, IOException> consumer) {
            Assert.checkNotNullParam("consumer", consumer);
            copyAndConsumeBytesWith(is -> IOUtil.consumeToReader(is, consumer, errorCharset));
            return this;
        }

        public Error<O> redirect() {
            check();
            errorStrategy = ERR_REDIRECT;
            errorHandler = null;
            return this;
        }
    }
}
