package io.smallrye.common.process;

import static io.smallrye.common.process.Logging.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.ProcessBuilder;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.Charset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.Functions;

class PipelineRunner<O> {
    private static final VarHandle ioBitsHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "ioBits",
            VarHandle.class, PipelineRunner.class, int.class);

    static final int IO_INPUT = 1 << 0;
    static final int IO_OUTPUT = 1 << 1;
    static final int IO_ERROR = 1 << 2;
    static final int IO_USER_ERROR = 1 << 3;
    static final int IO_WHILE_RUNNING = 1 << 4;

    final ProcessBuilderImpl<O> processBuilder;
    final PipelineRunner<?> prev;
    @SuppressWarnings("unused") // ioBitsHandle
    private volatile int ioBits;
    private Thread inputThread;
    private Thread errorReaderThread;
    private Thread userErrorThread;
    private Thread whileRunningThread;
    private Thread waitForThread;
    private ProcessHandlerException inputProblem;
    private ProcessHandlerException errorProblem;
    private ProcessHandlerException userErrorProblem;
    private ProcessHandlerException whileRunningProblem;
    private ProcessHandlerException exitCheckerProblem;
    private AbnormalExitException abnormalExit;
    Process process;
    private Gatherer gatherer;

    PipelineRunner(final ProcessBuilderImpl<O> processBuilder, final PipelineRunner<?> prev) {
        this.processBuilder = processBuilder;
        this.prev = prev;
    }

    void assembleBuilders(ProcessBuilder[] array) {
        array[processBuilder.depth - 1] = processBuilder.pb;
        if (prev != null) {
            prev.assembleBuilders(array);
        }
    }

    void setProcesses(List<Process> list) {
        process = list.get(processBuilder.depth - 1);
        if (prev != null) {
            prev.setProcesses(list);
        }
    }

    int createInputThread(ThreadFactory tf, ProcessRunner<?> runner) throws IOException {
        ExceptionConsumer<Process, IOException> inputHandler = processBuilder.inputHandler;
        if (inputHandler != null) {
            inputThread = tf.newThread(() -> {
                if (runner.awaitOk()) {
                    Thread.currentThread().setName("process-input-\"%s\"-%d"
                            .formatted(processBuilder.command, process.pid()));
                    try {
                        inputHandler.accept(process);
                    } catch (ProcessHandlerException phe) {
                        inputProblem = phe;
                    } catch (Throwable t) {
                        inputProblem = new ProcessHandlerException("Input generation failed due to exception", t);
                    } finally {
                        ioDone(IO_INPUT);
                        runner.taskComplete();
                    }
                }
            });
            if (inputThread == null) {
                throw noThread(tf);
            }
            inputThread.setName("process-input-\"%s\"-???".formatted(processBuilder.command));
            ioRegistered(IO_INPUT);
            return 1;
        } else {
            return 0;
        }
    }

    int createErrorThreads(ThreadFactory tf, ProcessRunner<?> runner) throws IOException {
        this.gatherer = new Gatherer(processBuilder.errorHeadLines, processBuilder.errorTailLines);
        ExceptionConsumer<BufferedReader, IOException> eh = processBuilder.errorHandler;
        Consumer<Object> consumer;
        if (eh != null) {
            //noinspection resource
            QueueReader qr = new QueueReader();
            consumer = processBuilder.errorGatherOnFail ? gatherer.andThen(qr::handleLine) : qr::handleLine;
            userErrorThread = tf.newThread(() -> {
                if (runner.awaitOk()) {
                    Thread.currentThread().setName("process-user-error-\"%s\"-%d"
                            .formatted(processBuilder.command, process.pid()));
                    try {
                        eh.accept(qr);
                    } catch (ProcessHandlerException phe) {
                        userErrorProblem = phe;
                    } catch (Throwable t) {
                        userErrorProblem = new ProcessHandlerException("User error processing failed due to exception", t);
                    } finally {
                        ioDone(IO_USER_ERROR);
                        runner.taskComplete();
                    }
                }
            });
            if (userErrorThread == null) {
                throw noThread(tf);
            }
            userErrorThread.setName("process-user-error-\"%s\"-???".formatted(processBuilder.command));
            ioRegistered(IO_USER_ERROR);
        } else {
            consumer = processBuilder.errorGatherOnFail ? gatherer : Functions.discardingConsumer();
        }
        errorReaderThread = tf.newThread(() -> {
            if (runner.awaitOk()) {
                Thread.currentThread().setName("process-error-reader-\"%s\"-%d"
                        .formatted(processBuilder.command, process.pid()));
                try {
                    Charset errorCharset = processBuilder.errorCharset;
                    if (errorCharset == null) {
                        try (BufferedReader reader = process.errorReader()) {
                            LineProcessor proc = new LineProcessor(reader, processBuilder.errorLineLimit, consumer);
                            proc.run();
                        }
                    } else {
                        try (BufferedReader reader = process.errorReader(errorCharset)) {
                            LineProcessor proc = new LineProcessor(reader, processBuilder.errorLineLimit, consumer);
                            proc.run();
                        }
                    }
                } catch (ProcessHandlerException phe) {
                    throw phe;
                } catch (Throwable t) {
                    errorProblem = new ProcessHandlerException("Error processing failed due to exception", t);
                } finally {
                    ioDone(IO_ERROR);
                    runner.taskComplete();
                }
            }
        });
        if (errorReaderThread == null) {
            throw noThread(tf);
        }
        errorReaderThread.setName("process-error-reader-\"%s\"-???".formatted(processBuilder.command));
        ioRegistered(IO_ERROR);
        return eh == null ? 1 : 2;
    }

    int createWhileRunningThread(ThreadFactory tf, ProcessRunner<?> runner) throws IOException {
        Consumer<WaitableProcessHandle> whileRunning = processBuilder.whileRunning;
        if (whileRunning != null) {
            whileRunningThread = tf.newThread(() -> {
                if (runner.awaitOk()) {
                    Thread.currentThread().setName("process-while-running-\"%s\"-%d"
                            .formatted(processBuilder.command, process.pid()));
                    try {
                        whileRunning.accept(
                                new WaitableProcessHandleImpl(process, processBuilder.command, processBuilder.arguments));
                    } catch (ProcessHandlerException phe) {
                        whileRunningProblem = phe;
                    } catch (Throwable t) {
                        whileRunningProblem = new ProcessHandlerException("While-running task failed due to exception", t);
                    } finally {
                        ioDone(IO_WHILE_RUNNING);
                        runner.taskComplete();
                    }
                }
            });
            whileRunningThread.setName("process-while-running-\"%s\"-???".formatted(processBuilder.command));
            ioRegistered(IO_WHILE_RUNNING);
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
                        .formatted(processBuilder.command, process.pid()));
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
                    List<String> errorLines = gatherer.toList();
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
        waitForThread.setName("process-waiter-\"%s\"-???".formatted(processBuilder.command));
        return 1;
    }

    void startThreads() {
        startThread(inputThread);
        startThread(errorReaderThread);
        startThread(userErrorThread);
        startThread(whileRunningThread);
        startThread(waitForThread);
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
        int ioBits = this.ioBits;
        while (ioBits != 0) {
            LockSupport.park(this);
            ioBits = this.ioBits;
        }
    }

    void ioRegistered(int bit) {
        //noinspection NonAtomicOperationOnVolatileField
        ioBits |= bit;
    }

    void ioDone(int bit) {
        int oldVal = (int) ioBitsHandle.getAndBitwiseAnd(this, ~bit);
        if (oldVal == bit) {
            LockSupport.unpark(waitForThread);
        }
    }

    static IOException noThread(final ThreadFactory tf) {
        return new IOException("Thread factory %s did not create a thread".formatted(tf));
    }

    void unpark() {
        LockSupport.unpark(inputThread);
        LockSupport.unpark(errorReaderThread);
        LockSupport.unpark(userErrorThread);
        LockSupport.unpark(whileRunningThread);
        LockSupport.unpark(waitForThread);
        if (prev != null) {
            prev.unpark();
        }
    }

    void collectProblems(final List<ProcessExecutionException> problems, ProcessHandlerException outputProblem) {
        // add problems in pipeline order
        if (prev != null) {
            prev.collectProblems(problems, null);
        }
        ProcessExecutionException pe = abnormalExit;
        if (pe == null && (inputProblem != null
                || exitCheckerProblem != null
                || errorProblem != null
                || userErrorProblem != null
                || whileRunningProblem != null
                || outputProblem != null)) {
            pe = newProcessException("Process handle failure");
        }
        if (pe != null) {
            ArrayList<ProcessHandlerException> causes = new ArrayList<>(6);
            if (inputProblem != null) {
                causes.add(inputProblem);
            }
            if (outputProblem != null) {
                causes.add(outputProblem);
            }
            if (errorProblem != null) {
                causes.add(errorProblem);
            }
            if (userErrorProblem != null) {
                causes.add(userErrorProblem);
            }
            if (whileRunningProblem != null) {
                causes.add(whileRunningProblem);
            }
            if (exitCheckerProblem != null) {
                causes.add(exitCheckerProblem);
            }
            switch (causes.size()) {
                case 0 -> {
                }
                case 1 -> pe.initCause(causes.get(0));
                default -> causes.forEach(pe::addSuppressed);
            }
            problems.add(pe);
        }
    }

    ProcessExecutionException newProcessException(String message, Throwable cause) {
        ProcessExecutionException pe = newProcessException(message);
        pe.initCause(cause);
        return pe;
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

    int createThreads(final ThreadFactory tf, final ProcessRunner<?> runner) throws IOException {
        int cnt = createInputThread(tf, runner)
                + createErrorThreads(tf, runner)
                + createWhileRunningThread(tf, runner)
                + createWaitForThread(tf, runner);
        return prev == null ? cnt : cnt + prev.createThreads(tf, runner);
    }
}
