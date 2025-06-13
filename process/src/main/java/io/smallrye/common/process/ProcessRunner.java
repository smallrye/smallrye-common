package io.smallrye.common.process;

import static io.smallrye.common.process.Logging.log;

import java.io.IOException;
import java.lang.ProcessBuilder;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 *
 */
final class ProcessRunner<O> extends PipelineRunner<O> {
    private static final VarHandle taskCountHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "taskCount",
            VarHandle.class, MethodHandles.lookup().lookupClass(), int.class);

    /**
     * Waiting for processes to successfully start.
     */
    private static final int STATUS_WAITING = 0;
    /**
     * Processes have started, {@code taskCount} is set, and the {@code process} field is populated.
     */
    private static final int STATUS_STARTED = 1;
    /**
     * One or more processes failed to start, so just exit.
     */
    private static final int STATUS_FAILED = 2;

    /**
     * The main thread; unparked once all I/O tasks are done.
     */
    private final CopyOnWriteArraySet<Thread> waiters = new CopyOnWriteArraySet<>(List.of(Thread.currentThread()));
    private volatile int status;
    private Thread outputThread;
    private ProcessHandlerException outputProblem;
    private O result;

    /**
     * Number of tasks still running. Main thread blocks until this reaches zero.
     */
    @SuppressWarnings("unused") // taskCountHandle
    private volatile int taskCount;

    ProcessRunner(final ProcessBuilderImpl<O> processBuilder, final PipelineRunner<?> prev) {
        super(processBuilder, prev);
    }

    void startThreads() {
        startThread(outputThread);
        super.startThreads();
    }

    void taskComplete() {
        int oldVal = (int) taskCountHandle.getAndAdd(this, -1);
        if (oldVal == 1) {
            // the last task was completed
            waiters.removeIf(thread -> {
                LockSupport.unpark(thread);
                return true;
            });
        }
    }

    int createThreads(ThreadFactory tf, final ProcessRunner<?> runner) throws IOException {
        return super.createThreads(tf, runner) + createOutputThread(tf);
    }

    O run() {
        ThreadFactory tf;
        // todo: if (Thread.currentThread().isVirtual()) { ... }
        tf = task -> new Thread(() -> {
            log.trace("Starting process thread");
            try {
                task.run();
            } finally {
                log.trace("Ending process thread");
            }
        });
        int taskCnt;
        try {
            // start up input, output, error, and wait-for threads
            taskCnt = createThreads(tf, this);
        } catch (IOException e) {
            throw new PipelineExecutionException("Failed to create process thread(s)", e);
        }
        try {
            startThreads();
        } catch (Throwable t) {
            status = STATUS_FAILED;
            unpark();
            throw new PipelineExecutionException("Failed to start process thread(s)", t);
        }
        try {
            java.lang.ProcessBuilder[] array = new java.lang.ProcessBuilder[processBuilder.depth];
            assembleBuilders(array);
            List<Process> processes = ProcessBuilder.startPipeline(List.of(array));
            setProcesses(processes);
            taskCount = taskCnt;
        } catch (Throwable t) {
            status = STATUS_FAILED;
            unpark();
            throw new PipelineExecutionException("Failed to start process pipeline", t);
        }
        status = STATUS_STARTED;
        unpark();
        Thread shutdownHook = new Thread(() -> {
            int cnt = taskCnt;
            if (cnt != 0) {
                waiters.add(Thread.currentThread());
                do {
                    Thread.interrupted();
                    LockSupport.park(this);
                    cnt = taskCount;
                } while (cnt != 0);
            }
        }, "process-shutdown-\"%s\"-%d".formatted(processBuilder.command, process.pid()));
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            int cnt = taskCnt;
            while (cnt != 0) {
                Thread.interrupted();
                LockSupport.park(this);
                cnt = taskCount;
            }
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        List<ProcessExecutionException> problems = new ArrayList<>(4);
        collectProblems(problems);
        return switch (problems.size()) {
            case 0 -> result;
            case 1 -> throw problems.get(0);
            default -> {
                PipelineExecutionException ex = new PipelineExecutionException("Pipeline execution failed");
                problems.forEach(ex::addSuppressed);
                throw ex;
            }
        };
    }

    void collectProblems(final List<ProcessExecutionException> problems) {
        collectProblems(problems, outputProblem);
    }

    int createOutputThread(ThreadFactory tf) throws IOException {
        if (processBuilder.outputHandler != null) {
            outputThread = tf.newThread(() -> {
                if (awaitOk()) {
                    Thread.currentThread().setName("process-output-\"%s\"-%d"
                            .formatted(processBuilder.command, process.pid()));
                    try {
                        result = processBuilder.outputHandler.apply(process);
                    } catch (ProcessHandlerException phe) {
                        outputProblem = phe;
                    } catch (Throwable t) {
                        outputProblem = new ProcessHandlerException("Output processing failed due to exception", t);
                    } finally {
                        ioDone(IO_OUTPUT);
                        taskComplete();
                    }
                }
            });
            if (outputThread == null) {
                throw noThread(tf);
            }
            outputThread.setName("process-output-\"%s\"-???".formatted(processBuilder.command));
            ioRegistered(IO_OUTPUT);
            return 1;
        } else {
            return 0;
        }
    }

    void unpark() {
        LockSupport.unpark(outputThread);
        super.unpark();
    }

    boolean awaitOk() {
        while (status == STATUS_WAITING) {
            LockSupport.park(processBuilder);
        }
        return status == STATUS_STARTED;
    }
}
