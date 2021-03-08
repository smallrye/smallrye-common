package io.smallrye.common.os;

/**
 * Returns information about a Process
 */
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
