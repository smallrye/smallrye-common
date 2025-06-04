package io.smallrye.common.process;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A process handle that can be waited for and whose exit value may be read.
 */
public interface WaitableProcessHandle extends ProcessHandle {
    /**
     * {@return the initial process command (not {@code null})}
     * This may differ from what {@link Info#command()} returns.
     */
    Path command();

    /**
     * {@return the initial process arguments as an immutable list (not {@code null})}
     * This may differ from what {@link Info#arguments()} returns.
     */
    List<String> arguments();

    /**
     * Wait indefinitely for the process to exit.
     *
     * @return the exit code of the process
     * @throws InterruptedException if the current thread is interrupted
     * @see Process#waitFor()
     */
    int waitFor() throws InterruptedException;

    /**
     * Wait indefinitely and uninterruptibly for the process to exit.
     *
     * @return the exit code of the process
     * @see Process#waitFor()
     */
    default int waitUninterruptiblyFor() {
        boolean intr = false;
        try {
            for (;;) {
                try {
                    return waitFor();
                } catch (InterruptedException ignored) {
                    intr = true;
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Wait for the process to exit for up to the given time.
     *
     * @param timeout the amount of time to wait
     * @param unit the time unit (must not be {@code null})
     * @return {@code true} if the process has exited, or {@code false} if it is still running
     * @throws InterruptedException if the current thread is interrupted
     * @see Process#waitFor(long,TimeUnit)
     */
    boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Wait uninterruptibly for the process to exit for up to the given time.
     *
     * @param timeout the amount of time to wait
     * @param unit the time unit (must not be {@code null})
     * @return {@code true} if the process has exited, or {@code false} if it is still running
     * @see Process#waitFor(long,TimeUnit)
     */
    default boolean waitUninterruptiblyFor(long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            return !isAlive();
        }
        // impl note:
        // this will only wait for a maximum of 292.277 years!
        long nanos = unit.toNanos(timeout);
        long start = System.nanoTime();
        boolean intr = false;
        try {
            while (nanos > 0) {
                try {
                    if (waitFor(nanos, TimeUnit.NANOSECONDS)) {
                        return true;
                    }
                } catch (InterruptedException e) {
                    intr = true;
                }
                long elapsed = -start + (start = System.nanoTime());
                nanos -= elapsed;
            }
            return !isAlive();
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * {@return the process exit value}
     *
     * @throws IllegalThreadStateException if the process has not exited
     * @see Process#exitValue()
     */
    int exitValue();
}
