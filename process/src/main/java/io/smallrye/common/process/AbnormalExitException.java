package io.smallrye.common.process;

import java.io.Serial;
import java.util.List;

import io.smallrye.common.os.OS;

/**
 * An exception indicating that a process has exited with an abnormal status.
 * Any additional problems will be recorded as suppressed exceptions.
 */
public class AbnormalExitException extends ProcessExecutionException {
    @Serial
    private static final long serialVersionUID = -2058346021193103167L;

    /**
     * The process exit code, if known.
     */
    private int exitCode = -1;
    /**
     * Set to {@code true} if the soft timeout was known to have elapsed.
     */
    private boolean softTimeoutElapsed;
    /**
     * Set to {@code true} if the hard timeout was known to have elapsed.
     */
    private boolean hardTimeoutElapsed;
    /**
     * The captured lines of error output.
     */
    private List<String> errorOutput = List.of();
    /**
     * The captured lines of output.
     */
    private List<String> output = List.of();

    /**
     * Constructs a new {@code AbnormalExitException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public AbnormalExitException() {
    }

    /**
     * Constructs a new {@code AbnormalExitException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    public AbnormalExitException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code AbnormalExitException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code ProcessException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public AbnormalExitException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code AbnormalExitException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public AbnormalExitException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * {@return the exit code of the process}
     */
    public int exitCode() {
        return exitCode;
    }

    /**
     * Set the exit code of the process.
     *
     * @param exitCode the exit code
     */
    public void setExitCode(final int exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * {@return {@code true} if the soft timeout elapsed before the process exited}
     */
    public boolean softTimeoutElapsed() {
        return softTimeoutElapsed;
    }

    /**
     * Set whether the soft timeout elapsed before the process exited.
     *
     * @param softTimeoutElapsed {@code true} if the soft timeout elapsed before the process exited
     */
    public void setSoftTimeoutElapsed(final boolean softTimeoutElapsed) {
        this.softTimeoutElapsed = softTimeoutElapsed;
    }

    /**
     * {@return {@code true} if the hard timeout elapsed before the process exited}
     */
    public boolean hardTimeoutElapsed() {
        return hardTimeoutElapsed;
    }

    /**
     * Set whether the hard timeout elapsed before the process exited.
     *
     * @param hardTimeoutElapsed {@code true} if the hard timeout elapsed before the process exited
     */
    public void setHardTimeoutElapsed(final boolean hardTimeoutElapsed) {
        this.hardTimeoutElapsed = hardTimeoutElapsed;
    }

    /**
     * {@return the captured error output of the process execution, if any}
     */
    public List<String> errorOutput() {
        return errorOutput;
    }

    /**
     * Set the captured error output of the process execution.
     *
     * @param errorOutput the captured error output of the process execution
     */
    public void setErrorOutput(final List<String> errorOutput) {
        this.errorOutput = List.copyOf(errorOutput);
    }

    /**
     * {@return the captured output of the process execution, if any}
     */
    public List<String> output() {
        return output;
    }

    /**
     * Set the captured output of the process execution.
     *
     * @param output the captured error output of the process execution
     */
    public void setOutput(final List<String> output) {
        this.output = List.copyOf(output);
    }

    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        if (exitCode != -1) {
            b.append(" with exit code ").append(exitCode);
            if (OS.current() != OS.WINDOWS && exitCode > 128 && exitCode <= 192) {
                // todo: add signal names and descriptions to OS module?
                b.append(" (possibly due to signal ").append(exitCode - 128).append(')');
            }
        }
        if (softTimeoutElapsed) {
            if (hardTimeoutElapsed) {
                b.append(" after soft and hard timeouts elapsed");
            } else {
                b.append(" after soft timeout elapsed");
            }
        } else if (hardTimeoutElapsed) {
            b.append(" after hard timeout elapsed");
        }
        List<String> errorOutput = this.errorOutput;
        if (!errorOutput.isEmpty()) {
            b.append(" with error output:");
            for (String line : errorOutput) {
                b.append("\n > ").append(line);
            }
        }
        List<String> output = this.output;
        if (!output.isEmpty()) {
            if (!errorOutput.isEmpty()) {
                b.append("\nand");
            }
            b.append(" with output:");
            for (String line : output) {
                b.append("\n > ").append(line);
            }
        }
        return b;
    }
}
