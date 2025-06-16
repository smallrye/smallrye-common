package io.smallrye.common.process;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 * An exception in the execution of a single process.
 * If this is due to a single problem, that problem will be recorded as the cause.
 * Otherwise, the list of problems will be recorded as suppressed exceptions.
 */
public class ProcessExecutionException extends AbstractExecutionException {
    @Serial
    private static final long serialVersionUID = -6038501979928698849L;

    /**
     * The process ID.
     */
    private long pid = -1;
    /**
     * The executed command.
     */
    private Path command = null;
    /**
     * The command arguments.
     */
    private List<String> arguments = List.of();
    /**
     * Set to {@code true} to include the command text in the exception message.
     */
    private boolean showCommand;

    /**
     * Constructs a new {@code ProcessExecutionException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public ProcessExecutionException() {
    }

    /**
     * Constructs a new {@code ProcessExecutionException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    public ProcessExecutionException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ProcessExecutionException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code ProcessExecutionException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ProcessExecutionException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ProcessExecutionException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public ProcessExecutionException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * {@return the process ID of the failed process, or {@code -1} if it is unknown or if the process failed to start}
     */
    public long pid() {
        return pid;
    }

    /**
     * Set the process ID of the failed process.
     *
     * @param pid the process ID of the failed process
     */
    public void setPid(final long pid) {
        this.pid = pid;
    }

    /**
     * {@return the command of the failed process, or {@code null} if it is unset}
     */
    public Path command() {
        return command;
    }

    /**
     * Set the command of the failed process.
     *
     * @param command the command of the failed process (must not be {@code null})
     */
    public void setCommand(final Path command) {
        this.command = Assert.checkNotNullParam("command", command);
    }

    /**
     * {@return the arguments of the failed process (not {@code null})}
     * The returned list is immutable.
     */
    public List<String> arguments() {
        return arguments;
    }

    /**
     * Set the arguments of the failed process.
     *
     * @param arguments the arguments of the failed process (must not be {@code null})
     */
    public void setArguments(final List<String> arguments) {
        this.arguments = List.copyOf(arguments);
    }

    /**
     * {@return {@code true} if the command and arguments should be shown in the exception message}
     */
    public boolean showCommand() {
        return showCommand;
    }

    /**
     * Indicate whether the command and arguments should be shown in the exception message.
     *
     * @param showCommand {@code true} to show the command and arguments
     */
    public void setShowCommand(final boolean showCommand) {
        this.showCommand = showCommand;
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb);
        if (showCommand && command != null) {
            sb.append(" for command \"").append(command).append('"');
            if (!arguments.isEmpty()) {
                arguments.forEach(arg -> sb.append(",\"").append(arg).append('"'));
            }
        }
        if (pid != -1) {
            sb.append(" (pid ").append(pid).append(')');
        }
        return sb;
    }
}
