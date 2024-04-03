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

    public ProcessInfo(long id, String command) {
        this.id = id;
        this.command = command;
    }

    public long getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }
}
