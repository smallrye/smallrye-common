package io.smallrye.common.os;

/**
 * Returns information about a Process
 *
 * @deprecated Use the {@link ProcessHandle} API instead.
 */
@Deprecated(since = "2.4", forRemoval = true)
public class ProcessInfo {
    private final long id;
    private final String command;

    /**
     * Construct a new instance.
     *
     * @param id the process ID
     * @param command the process command
     */
    public ProcessInfo(long id, String command) {
        this.id = id;
        this.command = command;
    }

    /**
     * {@return the process ID}
     */
    public long getId() {
        return id;
    }

    /**
     * {@return the process command}
     */
    public String getCommand() {
        return command;
    }
}
