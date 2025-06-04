package io.smallrye.common.process;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

final class WaitableProcessHandleImpl implements WaitableProcessHandle {
    private final Process process;
    private final Path command;
    private final List<String> arguments;

    WaitableProcessHandleImpl(final Process process, final Path command, final List<String> arguments) {
        this.process = process;
        this.command = command;
        this.arguments = arguments;
    }

    public Path command() {
        return command;
    }

    public List<String> arguments() {
        return arguments;
    }

    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    public boolean waitFor(final long timeout, final TimeUnit unit) throws InterruptedException {
        return process.waitFor(timeout, unit);
    }

    public int exitValue() {
        return process.exitValue();
    }

    public long pid() {
        return process.pid();
    }

    public Optional<ProcessHandle> parent() {
        return process.toHandle().parent();
    }

    public Stream<ProcessHandle> children() {
        return process.toHandle().children();
    }

    public Stream<ProcessHandle> descendants() {
        return process.toHandle().descendants();
    }

    public Info info() {
        return process.toHandle().info();
    }

    public CompletableFuture<ProcessHandle> onExit() {
        return process.toHandle().onExit();
    }

    public boolean supportsNormalTermination() {
        return process.supportsNormalTermination();
    }

    public boolean destroy() {
        return process.toHandle().destroy();
    }

    public boolean destroyForcibly() {
        return process.toHandle().destroyForcibly();
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public int hashCode() {
        return process.toHandle().hashCode();
    }

    public boolean equals(final Object other) {
        return this == other || other instanceof WaitableProcessHandleImpl wph && process.equals(wph.process);
    }

    public int compareTo(final ProcessHandle other) {
        return compareTo((WaitableProcessHandleImpl) other);
    }

    int compareTo(final WaitableProcessHandleImpl other) {
        return Long.compare(pid(), other.pid());
    }

    public String toString() {
        return process.toHandle().toString();
    }
}
