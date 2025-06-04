package io.smallrye.common.process;

import java.io.Serial;

/**
 * An exception indicating a problem with process execution.
 */
public class ProcessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -2058346021193103167L;

    /**
     * Constructs a new {@code ProcessException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public ProcessException() {
    }

    /**
     * Constructs a new {@code ProcessException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    public ProcessException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ProcessException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code ProcessException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ProcessException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ProcessException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public ProcessException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
