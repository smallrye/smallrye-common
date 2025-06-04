package io.smallrye.common.process;

import java.util.concurrent.ExecutionException;

/**
 *
 */
public final class ProcessUtil {
    private ProcessUtil() {
    }

    public static void destroyAllForcibly(ProcessHandle handle) {
        ProcessException e = null;
        // capture the child processes *before* killing them
        for (ProcessHandle processHandle : handle.children().toList()) {
            try {
                destroyAllForcibly(processHandle);
            } catch (ProcessException pe) {
                if (e == null) {
                    e = new ProcessException("Failed to kill multiple subprocesses");
                }
                e.addSuppressed(pe.getCause());
            }
        }
        handle.destroyForcibly();
        boolean intr = Thread.interrupted();
        try {
            for (;;) {
                try {
                    handle.onExit().get();
                    return;
                } catch (InterruptedException ignored) {
                    intr = true;
                } catch (ExecutionException ee) {
                    throw new ProcessException("Failed to kill process " + handle.pid(), ee.getCause());
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
