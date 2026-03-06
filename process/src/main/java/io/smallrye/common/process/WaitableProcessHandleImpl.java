package io.smallrye.common.process;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * An implementation of {@link WaitableProcessHandle}.
 *
 * @param <O> the process output type
 */
final class WaitableProcessHandleImpl<O> implements WaitableProcessHandle<O> {
    /**
     * The underlying operating system process.
     */
    private final Process process;

    /**
     * The process executable command path.
     */
    private final Path command;

    /**
     * The process command arguments.
     */
    private final List<String> arguments;

    /**
     * The process runner for retrieving execution results and awaiting completion.
     */
    private final ProcessRunner<O> runner;

    /**
     * Constructs a new {@code WaitableProcessHandleImpl} instance.
     *
     * @param process the underlying operating system process (must not be {@code null})
     * @param command the process executable command path (must not be {@code null})
     * @param arguments the process command arguments (must not be {@code null})
     * @param runner the process runner (must not be {@code null})
     */
    WaitableProcessHandleImpl(final Process process, final Path command, final List<String> arguments,
            final ProcessRunner<O> runner) {
        this.process = process;
        this.command = command;
        this.arguments = arguments;
        this.runner = runner;
    }

    @Override
    public Path command() {
        return command;
    }

    @Override
    public List<String> arguments() {
        return arguments;
    }

    @Override
    public int waitFor() throws InterruptedException {
        Thread thread = runner.asyncThread;
        if (thread != null) {
            thread.join();
        } else {
            process.waitFor();
        }
        return process.exitValue();
    }

    @Override
    public boolean waitFor(final long timeout, final TimeUnit unit) throws InterruptedException {
        Thread thread = runner.asyncThread;
        if (thread != null) {
            if (timeout <= 0) {
                return !isAlive();
            }
            long millis = unit.toMillis(timeout);
            if (millis <= 0) {
                long nanos = unit.toNanos(timeout);
                thread.join(0, (int) nanos);
            } else {
                long nanos = unit.toNanos(timeout) % 1_000_000;
                thread.join(millis, (int) nanos);
            }
            return !thread.isAlive();
        } else {
            return process.waitFor(timeout, unit);
        }
    }

    @Override
    public int exitValue() {
        return process.exitValue();
    }

    @Override
    public void close() {
        // see https://bugs.openjdk.org/browse/JDK-8364361 for behavior rationale
        try (var es = process.getErrorStream(); var is = process.getInputStream(); var os = process.getOutputStream()) {
            destroy();
            boolean intr = false;
            try {
                for (;;) {
                    try {
                        waitFor();
                        return;
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
            } finally {
                if (intr) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public long pid() {
        return process.pid();
    }

    @Override
    public Optional<ProcessHandle> parent() {
        return process.toHandle().parent();
    }

    @Override
    public Stream<ProcessHandle> children() {
        return process.toHandle().children();
    }

    @Override
    public Stream<ProcessHandle> descendants() {
        return process.toHandle().descendants();
    }

    @Override
    public Info info() {
        return process.toHandle().info();
    }

    @Override
    public CompletableFuture<ProcessHandle> onExit() {
        return process.toHandle().onExit();
    }

    @Override
    public boolean supportsNormalTermination() {
        return process.supportsNormalTermination();
    }

    @Override
    public boolean destroy() {
        return process.toHandle().destroy();
    }

    @Override
    public boolean destroyForcibly() {
        return process.toHandle().destroyForcibly();
    }

    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public int hashCode() {
        return process.toHandle().hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof WaitableProcessHandleImpl<?> t && equals(t);
    }

    /**
     * Specific equality check for {@code WaitableProcessHandleImpl}.
     *
     * @param other the other handle (may be {@code null})
     * @return {@code true} if equal, {@code false} otherwise
     */
    public boolean equals(final WaitableProcessHandleImpl<?> other) {
        return this == other || other != null && process.equals(other.process);
    }

    @Override
    public int compareTo(final ProcessHandle other) {
        return compareTo((WaitableProcessHandleImpl<?>) other);
    }

    /**
     * Compare this process handle to another implementation instance.
     *
     * @param other the other instance (must not be {@code null})
     * @return the comparison result
     */
    int compareTo(final WaitableProcessHandleImpl<?> other) {
        return Long.compare(pid(), other.pid());
    }

    @Override
    public String toString() {
        return process.toHandle().toString();
    }

    @Override
    public O result() throws IllegalStateException, PipelineExecutionException {
        return runner.result();
    }
}
