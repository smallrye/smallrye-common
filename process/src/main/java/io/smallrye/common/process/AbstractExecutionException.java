package io.smallrye.common.process;

import java.io.Serial;

/**
 * The base type of all execution exception types.
 */
public abstract class AbstractExecutionException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 7071176650961828059L;

    /**
     * Constructs a new {@code AbstractExecutionException} instance with an initial message and cause,
     * and configuring whether the stack trace is writable.
     *
     * @param message the message
     * @param cause the cause
     * @param writableStackTrace {@code true} to allow writable stack traces
     */
    protected AbstractExecutionException(final String message, final Throwable cause, final boolean writableStackTrace) {
        super(message, cause, true, writableStackTrace);
    }

    /**
     * Constructs a new {@code AbstractExecutionException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    protected AbstractExecutionException() {
    }

    /**
     * Constructs a new {@code AbstractExecutionException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    protected AbstractExecutionException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code AbstractExecutionException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code AbstractExecutionException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    protected AbstractExecutionException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code AbstractExecutionException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    protected AbstractExecutionException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * Render this exception to the given string builder.
     *
     * @param sb the string builder (must not be {@code null})
     * @return the same string builder (not {@code null})
     */
    public StringBuilder getMessage(StringBuilder sb) {
        String message = super.getMessage();
        if (message != null) {
            sb.append(message);
        }
        return sb;
    }

    @Override
    public String getMessage() {
        return getMessage(new StringBuilder()).toString();
    }
}
