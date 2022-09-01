package io.smallrye.common.cpu;

/**
 * Provides general information about the processors on this host (Java 9 version).
 */
public class ProcessorInfo {
    private ProcessorInfo() {
    }

    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
}
