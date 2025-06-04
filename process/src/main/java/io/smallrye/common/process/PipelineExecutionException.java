package io.smallrye.common.process;

import java.io.Serial;
import java.util.List;
import java.util.stream.Stream;

/**
 * An exception indicating a problem in multiple process stages of a pipeline.
 * Exceptions of this type will generally have suppressed exceptions for the various
 * problems that occurred within a pipeline.
 * The convenience method {@link #processExecutionExceptions()} provides easy access
 * to these causes.
 */
public final class PipelineExecutionException extends AbstractExecutionException {
    @Serial
    private static final long serialVersionUID = -412352445235180802L;

    /**
     * Constructs a new {@code PipelineExecutionException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public PipelineExecutionException() {
    }

    /**
     * Constructs a new {@code PipelineExecutionException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    public PipelineExecutionException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code PipelineExecutionException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code PipelineExecutionException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public PipelineExecutionException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code PipelineExecutionException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public PipelineExecutionException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * {@return the list of suppressed process execution exceptions}
     */
    public List<ProcessExecutionException> processExecutionExceptions() {
        return Stream.of(getSuppressed())
                .filter(ProcessExecutionException.class::isInstance)
                .map(ProcessExecutionException.class::cast)
                .toList();
    }
}
