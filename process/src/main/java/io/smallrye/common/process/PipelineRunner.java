package io.smallrye.common.process;

import static io.smallrye.common.process.Logging.log;
import static io.smallrye.common.process.ProcessBuilderImpl.ERR_DISCARD;
import static io.smallrye.common.process.ProcessBuilderImpl.ERR_FILE_APPEND;
import static io.smallrye.common.process.ProcessBuilderImpl.ERR_FILE_WRITE;
import static io.smallrye.common.process.ProcessBuilderImpl.ERR_INHERIT;
import static io.smallrye.common.process.ProcessBuilderImpl.ERR_REDIRECT;
import static io.smallrye.common.process.ProcessBuilderImpl.IN_EMPTY;
import static io.smallrye.common.process.ProcessBuilderImpl.IN_FILE;
import static io.smallrye.common.process.ProcessBuilderImpl.IN_HANDLER;
import static io.smallrye.common.process.ProcessBuilderImpl.IN_INHERIT;
import static io.smallrye.common.process.ProcessBuilderImpl.IN_PIPELINE;
import static io.smallrye.common.process.ProcessBuilderImpl.IN_PIPELINE_SPLIT;
import static io.smallrye.common.process.ProcessBuilderImpl.OUT_DISCARD;
import static io.smallrye.common.process.ProcessBuilderImpl.OUT_FILE_APPEND;
import static io.smallrye.common.process.ProcessBuilderImpl.OUT_FILE_WRITE;
import static io.smallrye.common.process.ProcessBuilderImpl.OUT_INHERIT;
import static io.smallrye.common.process.ProcessBuilderImpl.OUT_PIPELINE;
import static io.smallrye.common.process.ProcessBuilderImpl.OUT_PIPELINE_SPLIT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;

class PipelineRunner<O> {
    private static final VarHandle ioCountHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "ioCount",
            VarHandle.class, PipelineRunner.class, int.class);

    final ProcessBuilderImpl<O> processBuilder;
    final PipelineRunner<O> prev;
    @SuppressWarnings("unused") // ioBitsHandle
    private volatile int ioCount;
    private Thread inputThread;
    private ProcessHandlerException inputProblem;
    private Thread outputMainThread;
    private List<Thread> outputExtraThreads = List.of();
    private final List<ProcessHandlerException> outputProblems = new CopyOnWriteArrayList<>();
    private Thread errorMainThread;
    private List<Thread> errorExtraThreads = List.of();
    private final List<ProcessHandlerException> errorProblems = new CopyOnWriteArrayList<>();
    private Thread whileRunningThread;
    private ProcessHandlerException whileRunningProblem;
    private Thread waitForThread;
    private ProcessHandlerException exitCheckerProblem;
    private AbnormalExitException abnormalExit;
    Thread asyncThread;
    Process process;
    private Gatherer errorGatherer;
    private Gatherer outputGatherer;
    private ProcessBuilder pb;

    PipelineRunner(final ProcessBuilderImpl<O> processBuilder, final PipelineRunner<O> prev) {
        this.processBuilder = processBuilder;
        this.prev = prev;
    }

    int createInputThread(ThreadFactory tf, ProcessRunner<?> runner) throws IOException {
        ExceptionConsumer<OutputStream, IOException> inputHandler = processBuilder.inputHandler;
        switch (processBuilder.inputStrategy) {
            case IN_EMPTY -> pb.redirectInput(ProcessBuilder.Redirect.DISCARD.file());
            case IN_INHERIT -> pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            case IN_FILE -> pb.redirectInput(processBuilder.inputFile);
            case IN_HANDLER -> {
                assert inputHandler != null;
                pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                inputThread = tf.newThread(() -> {
                    if (runner.awaitOk()) {
                        Thread.currentThread().setName("process-input-\"%s\"-%d"
                                .formatted(processBuilder.command.getFileName(), process.pid()));
                        try (OutputStream os = process.getOutputStream()) {
                            inputHandler.accept(os);
                        } catch (ProcessHandlerException phe) {
                            inputProblem = phe;
                        } catch (Throwable t) {
                            inputProblem = new ProcessHandlerException("Input generation failed due to exception", t);
                        } finally {
                            ioDone();
                            runner.taskComplete();
                        }
                    }
                });
                if (inputThread == null) {
                    throw noThread(tf);
                }
                inputThread.setName("process-input-\"%s\"-???".formatted(processBuilder.command.getFileName()));
                ioRegistered();
                return 1;
            }
            case IN_PIPELINE -> {
                // nothing
            }
            case IN_PIPELINE_SPLIT -> {
                pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            }
        }
        return 0;
    }

    int createErrorThreads(ThreadFactory tf, ProcessRunner<?> runner) throws IOException {
        int strategy = processBuilder.errorStrategy;
        if (strategy == ERR_REDIRECT) {
            pb.redirectErrorStream(true);
            return 0;
        }
        ExceptionConsumer<InputStream, IOException> eh = processBuilder.errorHandler;
        List<ExceptionConsumer<InputStream, IOException>> extras = processBuilder.extraErrorHandlers;
        List<ExceptionConsumer<InputStream, IOException>> allHandlers = new ArrayList<>(extras.size() + 2);
        if (processBuilder.errorGatherOnFail || processBuilder.errorLogOnSuccess) {
            this.errorGatherer = new Gatherer(processBuilder.errorHeadLines, processBuilder.errorTailLines);
            allHandlers.add(is -> IOUtil.consumeToReader(is,
                    br -> errorGatherer.run(new LineReader(br, processBuilder.errorLineLimit)),
                    processBuilder.errorCharset));
        }
        if (eh != null) {
            allHandlers.add(eh);
        }
        allHandlers.addAll(extras);
        if (allHandlers.isEmpty() || allHandlers.size() == 1 && eh != null) {
            // likely a trivial strategy
            switch (strategy) {
                case ERR_DISCARD -> {
                    pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                    return 0;
                }
                case ERR_INHERIT -> {
                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    return 0;
                }
                case ERR_FILE_WRITE -> {
                    pb.redirectError(ProcessBuilder.Redirect.to(processBuilder.errorFile));
                    return 0;
                }
                case ERR_FILE_APPEND -> {
                    pb.redirectError(ProcessBuilder.Redirect.appendTo(processBuilder.errorFile));
                    return 0;
                }
            }
        }
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        int handlerCnt = allHandlers.size();
        if (handlerCnt == 1) {
            // simple thread
            ExceptionConsumer<InputStream, IOException> handler = allHandlers.get(0);
            errorMainThread = tf.newThread(() -> {
                if (runner.awaitOk()) {
                    Thread.currentThread().setName("process-error-consumer-\"%s\"-%d"
                            .formatted(processBuilder.command.getFileName(), process.pid()));
                    try (InputStream is = process.getErrorStream()) {
                        handler.accept(is);
                    } catch (ProcessHandlerException phe) {
                        errorProblems.add(phe);
                    } catch (Throwable t) {
                        errorProblems.add(new ProcessHandlerException("User error processing failed due to exception", t));
                    } finally {
                        ioDone();
                        runner.taskComplete();
                    }
                }
            });
            if (errorMainThread == null) {
                throw noThread(tf);
            }
            errorMainThread.setName("process-error-consumer-\"%s\"-???".formatted(processBuilder.command.getFileName()));
            ioRegistered();
            return 1;
        } else {
            // we have to make a tee
            final Tee tee = new Tee(handlerCnt);
            List<Tee.TeeInputStream> outputs = tee.outputs();
            errorMainThread = tf.newThread(() -> {
                if (runner.awaitOk()) {
                    Thread.currentThread().setName("process-error-tee-\"%s\"-%d"
                            .formatted(processBuilder.command.getFileName(), process.pid()));
                    try (InputStream is = process.getErrorStream()) {
                        tee.run(is);
                    } catch (ProcessHandlerException phe) {
                        errorProblems.add(phe);
                    } catch (Throwable t) {
                        errorProblems.add(new ProcessHandlerException("User error processing failed due to exception", t));
                    } finally {
                        ioDone();
                        runner.taskComplete();
                    }
                }
            });
            if (errorMainThread == null) {
                throw noThread(tf);
            }
            errorMainThread.setName("process-error-tee-\"%s\"-???".formatted(processBuilder.command.getFileName()));
            ioRegistered();
            ArrayList<Thread> handlerThreads = new ArrayList<>(handlerCnt);
            for (int i = 0; i < handlerCnt; i++) {
                ExceptionConsumer<InputStream, IOException> handler = allHandlers.get(i);
                Tee.TeeInputStream input = outputs.get(i);
                int idx = i;
                Thread thr = tf.newThread(() -> {
                    if (runner.awaitOk()) {
                        Thread.currentThread().setName("process-error-consumer-%d-\"%s\"-%d"
                                .formatted(idx, processBuilder.command.getFileName(), process.pid()));
                        try (input) {
                            handler.accept(input);
                        } catch (ProcessHandlerException phe) {
                            errorProblems.add(phe);
                        } catch (Throwable t) {
                            errorProblems.add(new ProcessHandlerException("User error processing failed due to exception", t));
                        } finally {
                            ioDone();
                            runner.taskComplete();
                        }
                    }
                });
                if (thr == null) {
                    throw noThread(tf);
                }
                thr.setName("process-error-consumer-%d-\"%s\"-???"
                        .formatted(idx, processBuilder.command.getFileName()));
                handlerThreads.add(thr);
                ioRegistered();
            }
            errorExtraThreads = handlerThreads;
            return 1 + handlerCnt;
        }
    }

    int createOutputThreads(ThreadFactory tf, final ProcessRunner<O> runner, final PipelineRunner<O> nextRunner)
            throws IOException {
        ExceptionFunction<InputStream, O, IOException> oh = processBuilder.outputHandler;
        List<ExceptionConsumer<InputStream, IOException>> extras = processBuilder.extraOutputHandlers;
        List<ExceptionConsumer<InputStream, IOException>> allHandlers = new ArrayList<>(extras.size() + 2);
        if (processBuilder.outputGatherOnFail) {
            this.outputGatherer = new Gatherer(processBuilder.outputHeadLines, processBuilder.outputTailLines);
            allHandlers.add(is -> IOUtil.consumeToReader(is,
                    br -> outputGatherer.run(new LineReader(br, processBuilder.outputLineLimit)),
                    processBuilder.outputCharset));
        }
        int strategy = processBuilder.outputStrategy;
        if (oh != null) {
            // only possible on the last stage
            allHandlers.add(is -> runner.result = oh.apply(is));
        }
        if (strategy == OUT_PIPELINE_SPLIT) {
            // SPECIAL: add a handler for the next process
            allHandlers.add(is -> {
                try (OutputStream os = nextRunner.process.getOutputStream()) {
                    is.transferTo(os);
                } finally {
                    // release the next process too
                    nextRunner.ioDone();
                }
            });
            // do not kill the next process before we are done feeding it
            nextRunner.ioRegistered();
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        }
        allHandlers.addAll(extras);
        if (allHandlers.isEmpty() || allHandlers.size() == 1 && oh != null) {
            switch (strategy) {
                case OUT_DISCARD -> {
                    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                    return 0;
                }
                case OUT_INHERIT -> {
                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    return 0;
                }
                case OUT_FILE_WRITE -> {
                    pb.redirectOutput(ProcessBuilder.Redirect.to(processBuilder.outputFile));
                    return 0;
                }
                case OUT_FILE_APPEND -> {
                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(processBuilder.outputFile));
                    return 0;
                }
                case OUT_PIPELINE -> {
                    return 0;
                }
            }
        }
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        int handlerCnt = allHandlers.size();
        if (handlerCnt == 1) {
            // simple thread
            ExceptionConsumer<InputStream, IOException> handler = allHandlers.get(0);
            outputMainThread = tf.newThread(() -> {
                if (runner.awaitOk()) {
                    Thread.currentThread().setName("process-output-consumer-\"%s\"-%d"
                            .formatted(processBuilder.command.getFileName(), process.pid()));
                    try (InputStream is = process.getInputStream()) {
                        handler.accept(is);
                    } catch (ProcessHandlerException phe) {
                        outputProblems.add(phe);
                    } catch (Throwable t) {
                        outputProblems.add(new ProcessHandlerException("User output processing failed due to exception", t));
                    } finally {
                        runner.ioDone();
                        runner.taskComplete();
                    }
                }
            });
            if (outputMainThread == null) {
                throw noThread(tf);
            }
            outputMainThread.setName("process-output-consumer-\"%s\"-???".formatted(processBuilder.command.getFileName()));
            runner.ioRegistered();
            return 1;
        } else {
            // we have to make a tee
            final Tee tee = new Tee(handlerCnt);
            List<Tee.TeeInputStream> outputs = tee.outputs();
            outputMainThread = tf.newThread(() -> {
                if (runner.awaitOk()) {
                    Thread.currentThread().setName("process-output-tee-\"%s\"-%d"
                            .formatted(processBuilder.command.getFileName(), process.pid()));
                    try (InputStream is = process.getInputStream()) {
                        tee.run(is);
                    } catch (ProcessHandlerException phe) {
                        outputProblems.add(phe);
                    } catch (Throwable t) {
                        outputProblems.add(new ProcessHandlerException("User output processing failed due to exception", t));
                    } finally {
                        runner.ioDone();
                        runner.taskComplete();
                    }
                }
            });
            if (outputMainThread == null) {
                throw noThread(tf);
            }
            outputMainThread.setName("process-output-tee-\"%s\"-???".formatted(processBuilder.command.getFileName()));
            runner.ioRegistered();
            ArrayList<Thread> handlerThreads = new ArrayList<>(handlerCnt);
            for (int i = 0; i < handlerCnt; i++) {
                ExceptionConsumer<InputStream, IOException> handler = allHandlers.get(i);
                Tee.TeeInputStream input = outputs.get(i);
                int idx = i;
                Thread thr = tf.newThread(() -> {
                    if (runner.awaitOk()) {
                        Thread.currentThread().setName("process-output-consumer-%d-\"%s\"-%d"
                                .formatted(idx, processBuilder.command.getFileName(), process.pid()));
                        try (input) {
                            handler.accept(input);
                        } catch (ProcessHandlerException phe) {
                            outputProblems.add(phe);
                        } catch (Throwable t) {
                            outputProblems
                                    .add(new ProcessHandlerException("User output processing failed due to exception", t));
                        } finally {
                            runner.ioDone();
                            runner.taskComplete();
                        }
                    }
                });
                if (thr == null) {
                    throw noThread(tf);
                }
                thr.setName("process-output-consumer-%d-\"%s\"-???"
                        .formatted(idx, processBuilder.command.getFileName()));
                handlerThreads.add(thr);
                runner.ioRegistered();
            }
            outputExtraThreads = handlerThreads;
            return 1 + handlerCnt;
        }
    }

    int createWhileRunningThread(ThreadFactory tf, ProcessRunner<?> runner) throws IOException {
        Consumer<WaitableProcessHandle> whileRunning = processBuilder.whileRunning;
        if (whileRunning != null) {
            whileRunningThread = tf.newThread(() -> {
                if (runner.awaitOk()) {
                    Thread.currentThread().setName("process-while-running-\"%s\"-%d"
                            .formatted(processBuilder.command.getFileName(), process.pid()));
                    try {
                        whileRunning.accept(
                                new WaitableProcessHandleImpl(process, processBuilder.command, processBuilder.arguments));
                    } catch (ProcessHandlerException phe) {
                        whileRunningProblem = phe;
                    } catch (Throwable t) {
                        whileRunningProblem = new ProcessHandlerException("While-running task failed due to exception", t);
                    } finally {
                        ioDone();
                        runner.taskComplete();
                    }
                }
            });
            whileRunningThread.setName("process-while-running-\"%s\"-???".formatted(processBuilder.command.getFileName()));
            ioRegistered();
            if (whileRunningThread == null) {
                throw noThread(tf);
            }
            return 1;
        } else {
            return 0;
        }
    }

    int createWaitForThread(ThreadFactory tf, ProcessRunner<?> runner) throws IOException {
        waitForThread = tf.newThread(() -> {
            if (runner.awaitOk()) {
                Thread.currentThread().setName("process-waiter-\"%s\"-%d"
                        .formatted(processBuilder.command.getFileName(), process.pid()));
                try {
                    boolean ste = false;
                    boolean hte = false;
                    awaitIoDone();
                    if (process.isAlive() && processBuilder.softExitTimeout != null) {
                        // start the countdown!
                        if (ProcessUtil.stillRunningAfter(process, processBuilder.softExitTimeout.get(ChronoUnit.NANOS))) {
                            ste = true;
                            if (process.supportsNormalTermination()) {
                                // start knocking on the door
                                process.destroy();
                            }
                        }
                    }
                    if (process.isAlive() && processBuilder.hardExitTimeout != null) {
                        if (ProcessUtil.stillRunningAfter(process, processBuilder.hardExitTimeout.get(ChronoUnit.NANOS))) {
                            hte = true;
                            // obliterate
                            ProcessUtil.destroyAllForcibly(process.toHandle());
                        }
                    }
                    int exitCode;
                    for (;;) {
                        try {
                            exitCode = process.waitFor();
                            break;
                        } catch (InterruptedException ignored) {
                        }
                    }
                    // now, check the exit code
                    List<String> errorLines = errorGatherer.toList();
                    boolean result;
                    try {
                        result = processBuilder.exitCodeChecker.test(exitCode);
                    } catch (ProcessHandlerException phe) {
                        exitCheckerProblem = phe;
                        return;
                    } catch (Throwable t) {
                        exitCheckerProblem = new ProcessHandlerException("Exit code checker task failed due to exception", t);
                        return;
                    }
                    if (!result) {
                        AbnormalExitException aee = new AbnormalExitException("Process exited abnormally");
                        aee.setExitCode(exitCode);
                        aee.setSoftTimeoutElapsed(ste);
                        aee.setHardTimeoutElapsed(hte);
                        if (processBuilder.errorGatherOnFail) {
                            aee.setErrorOutput(errorLines);
                        }
                        if (processBuilder.outputGatherOnFail) {
                            List<String> outputLines = outputGatherer.toList();
                            aee.setOutput(outputLines);
                        }
                        aee.setCommand(processBuilder.command);
                        aee.setArguments(processBuilder.arguments);
                        aee.setPid(process.pid());
                        abnormalExit = aee;
                    } else if (processBuilder.errorLogOnSuccess) {
                        if (!errorLines.isEmpty()) {
                            StringBuilder sb = new StringBuilder(512);
                            errorLines.forEach(line -> sb.append("\n\t").append(line));
                            log.logErrors(processBuilder.command, process.pid(), sb);
                        }
                    }
                } finally {
                    runner.taskComplete();
                }
            }
        });
        if (waitForThread == null) {
            throw noThread(tf);
        }
        waitForThread.setName("process-waiter-\"%s\"-???".formatted(processBuilder.command.getFileName()));
        return 1;
    }

    void startThreads() {
        startThread(inputThread);
        startThread(outputMainThread);
        outputExtraThreads.forEach(Thread::start);
        startThread(errorMainThread);
        errorExtraThreads.forEach(Thread::start);
        startThread(whileRunningThread);
        startThread(waitForThread);
        startThread(asyncThread);
        if (prev != null) {
            prev.startThreads();
        }
    }

    static void startThread(Thread thread) {
        if (thread != null) {
            thread.start();
        }
    }

    private void awaitIoDone() {
        int ioCount = this.ioCount;
        while (ioCount != 0) {
            LockSupport.park(this);
            ioCount = this.ioCount;
        }
    }

    void ioRegistered() {
        // we're single-threaded at this point
        //noinspection NonAtomicOperationOnVolatileField
        ioCount++;
    }

    void ioDone() {
        int oldVal = (int) ioCountHandle.getAndAdd(this, -1);
        if (oldVal == 1) {
            LockSupport.unpark(waitForThread);
        }
    }

    static IOException noThread(final ThreadFactory tf) {
        return new IOException("Thread factory %s did not create a thread".formatted(tf));
    }

    void unpark() {
        LockSupport.unpark(inputThread);
        LockSupport.unpark(outputMainThread);
        outputExtraThreads.forEach(LockSupport::unpark);
        LockSupport.unpark(errorMainThread);
        errorExtraThreads.forEach(LockSupport::unpark);
        LockSupport.unpark(whileRunningThread);
        LockSupport.unpark(waitForThread);
        LockSupport.unpark(asyncThread);
        if (prev != null) {
            prev.unpark();
        }
    }

    void collectProblems(final List<ProcessExecutionException> problems) {
        // add problems in pipeline order
        if (prev != null) {
            prev.collectProblems(problems);
        }
        ProcessExecutionException pe = abnormalExit;
        if (pe == null && (inputProblem != null
                || !outputProblems.isEmpty()
                || !errorProblems.isEmpty()
                || exitCheckerProblem != null
                || whileRunningProblem != null)) {
            pe = newProcessException("Process handle failure");
        }
        if (pe != null) {
            ArrayList<ProcessHandlerException> causes = new ArrayList<>(6);
            if (inputProblem != null) {
                causes.add(inputProblem);
            }
            causes.addAll(outputProblems);
            causes.addAll(errorProblems);
            if (whileRunningProblem != null) {
                causes.add(whileRunningProblem);
            }
            if (exitCheckerProblem != null) {
                causes.add(exitCheckerProblem);
            }
            switch (causes.size()) {
                case 1 -> pe.initCause(causes.get(0));
                default -> causes.forEach(pe::addSuppressed);
            }
            problems.add(pe);
        }
    }

    ProcessExecutionException newProcessException(String message) {
        ProcessExecutionException pe = new ProcessExecutionException(message);
        if (process != null) {
            pe.setPid(process.pid());
        }
        pe.setCommand(processBuilder.command);
        pe.setArguments(processBuilder.arguments);
        return pe;
    }

    int createThreads(final ThreadFactory tf, final ProcessRunner<O> runner, final PipelineRunner<O> nextRunner)
            throws IOException {
        pb = processBuilder.pb;
        pb.command(Stream.concat(Stream.of(processBuilder.command.toString()), processBuilder.arguments.stream()).toList());
        pb.directory(processBuilder.directory);
        int cnt = createInputThread(tf, runner)
                + createErrorThreads(tf, runner)
                + createOutputThreads(tf, runner, nextRunner)
                + createWhileRunningThread(tf, runner)
                + createWaitForThread(tf, runner);
        return prev == null ? cnt : cnt + prev.createThreads(tf, runner, this);
    }

    void startProcesses(int pipelineEnd, List<Process> processes, List<ProcessBuilder> builders) throws IOException {
        int depth = processBuilder.depth;
        int index = depth - 1;
        builders.set(index, pb);
        if (processBuilder.outputStrategy != OUT_PIPELINE_SPLIT) {
            if (prev == null) {
                // start the pipeline, from the beginning
                processes.addAll(ProcessBuilder.startPipeline(builders.subList(0, pipelineEnd)));
            } else {
                // let the previous step start the pipeline
                prev.startProcesses(pipelineEnd, processes, builders);
            }
        } else {
            // split the pipeline here
            // start earlier processes (if any)
            if (prev != null) {
                prev.startProcesses(depth, processes, builders);
            }
            // now start the subsequent processes in the pipeline separately (if any)
            if (depth < pipelineEnd) {
                processes.addAll(ProcessBuilder.startPipeline(builders.subList(depth, pipelineEnd)));
            }
        }
        process = processes.get(index);
    }
}
