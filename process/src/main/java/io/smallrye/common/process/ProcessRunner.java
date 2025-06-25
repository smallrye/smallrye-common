package io.smallrye.common.process;

import static io.smallrye.common.process.Logging.log;

import java.io.IOException;
import java.lang.ProcessBuilder;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

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
    O result;

    /**
     * Number of tasks still running. Main thread blocks until this reaches zero.
     */
    @SuppressWarnings("unused") // taskCountHandle
    private volatile int taskCount;

    ProcessRunner(final ProcessBuilderImpl<O> processBuilder, final PipelineRunner<O> prev) {
        super(processBuilder, prev);
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

    CompletableFuture<O> runAsync() {
        CompletableFuture<O> cf = new CompletableFuture<>();
        ThreadFactory tf = threadFactory();
        asyncThread = tf.newThread(() -> {
            if (awaitOk()) {
                Thread shutdownHook = registerHook();
                await();
                try {
                    cf.complete(complete());
                } catch (Throwable t) {
                    cf.completeExceptionally(t);
                } finally {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                }
            }
        });
        if (asyncThread == null) {
            throw new PipelineExecutionException("Failed to start process thread(s)", noThread(tf));
        }
        asyncThread.setName("process-async-handler");
        initialize(tf);
        return cf;
    }

    O run() {
        initialize(threadFactory());
        Thread shutdownHook = registerHook();
        try {
            await();
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        return complete();
    }

    private void initialize(final ThreadFactory tf) {
        int taskCnt;
        try {
            // start up input, output, error, and wait-for threads
            taskCnt = createThreads(tf, this, null);
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
            int depth = processBuilder.depth;
            final List<Process> processes = new ArrayList<>(depth);
            final List<ProcessBuilder> processBuilders = Arrays.asList(new ProcessBuilder[depth]);
            try {
                startProcesses(depth, processes, processBuilders);
            } catch (Throwable t) {
                processes.forEach(ProcessUtil::destroyAllForcibly);
                throw t;
            }
            taskCount = taskCnt;
        } catch (Throwable t) {
            status = STATUS_FAILED;
            unpark();
            throw new PipelineExecutionException("Failed to start process pipeline", t);
        }
        status = STATUS_STARTED;
        unpark();
    }

    private void await() {
        int cnt = taskCount;
        if (cnt != 0) {
            waiters.add(Thread.currentThread());
        }
        do {
            Thread.interrupted();
            LockSupport.park(this);
            cnt = taskCount;
        } while (cnt != 0);
    }

    private Thread registerHook() {
        Thread shutdownHook = new Thread(() -> {
            int cnt = taskCount;
            if (cnt != 0) {
                waiters.add(Thread.currentThread());
                do {
                    log.debugf("Waiting for processes to exit (%d subtasks remaining)", cnt);
                    Thread.interrupted();
                    LockSupport.park(this);
                    cnt = taskCount;
                } while (cnt != 0);
                log.debug("All process exit tasks are complete");
            }
        }, "pipeline-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        return shutdownHook;
    }

    private O complete() {
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

    private static ThreadFactory threadFactory() {
        // todo: if (Thread.currentThread().isVirtual()) { ... }
        return task -> new Thread(() -> {
            log.trace("Starting process thread");
            try {
                task.run();
            } finally {
                log.trace("Ending process thread");
            }
        });
    }

    boolean awaitOk() {
        while (status == STATUS_WAITING) {
            LockSupport.park(this);
        }
        log.trace("Process thread released");
        return status == STATUS_STARTED;
    }
}
