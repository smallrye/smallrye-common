package io.smallrye.common.process;

import java.io.Serial;

/**
 * An exception indicating a problem in one of the handler callbacks for the execution of a process.
 * The cause of the problem will typically be recorded as the cause of this exception.
 */
public class ProcessHandlerException extends AbstractExecutionException {
    @Serial
    private static final long serialVersionUID = -7501352008357214573L;

    /**
     * Constructs a new {@code ProcessHandlerException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public ProcessHandlerException() {
    }

    /**
     * Constructs a new {@code ProcessHandlerException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    public ProcessHandlerException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ProcessHandlerException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code ProcessHandlerException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ProcessHandlerException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ProcessHandlerException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public ProcessHandlerException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
