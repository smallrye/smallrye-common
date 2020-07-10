package io.smallrye.common.version;

/**
 * An exception thrown when a version string has an invalid syntax.
 */
public class VersionSyntaxException extends IllegalArgumentException {
    private static final long serialVersionUID = 1678400739307484818L;

    /**
     * Constructs a new {@code VersionSyntaxException} instance. The message is left blank ({@code null}), and no cause
     * is specified.
     */
    public VersionSyntaxException() {
    }

    /**
     * Constructs a new {@code VersionSyntaxException} instance with an initial message. No cause is specified.
     *
     * @param msg the message
     */
    public VersionSyntaxException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code VersionSyntaxException} instance with an initial cause. If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code VersionSyntaxException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public VersionSyntaxException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code VersionSyntaxException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public VersionSyntaxException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
