package io.smallrye.common.process;

/**
 * The result of process execution.
 *
 * @param <R> the result type
 */
public final class ProcessResult<R> {
    private final R result;
    private final int exitCode;

    static final ProcessResult<Void> OK_EMPTY = of(null, 0);

    private ProcessResult(final R result, final int exitCode) {
        this.result = result;
        this.exitCode = exitCode;
    }

    /**
     * Construct a new instance.
     *
     * @param result the execution result, if any
     * @param exitCode the exit code of the process
     * @return the new result object
     * @param <R> the result type
     */
    public static <R> ProcessResult<R> of(R result, int exitCode) {
        return new ProcessResult<>(result, exitCode);
    }

    /**
     * {@return the result returned by the output handler (if any)}
     */
    public R result() {
        return result;
    }

    /**
     * {@return the process exit code}
     */
    public int exitCode() {
        return exitCode;
    }
}
