package io.smallrye.common.cpu;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
 * Provides general information about the processors on this host.
 */
public class ProcessorInfo {
    private ProcessorInfo() {
    }

    private static final String CPUS_ALLOWED = "Cpus_allowed:";

    /**
     * Returns the number of processors available to this process. On most operating systems this method
     * simply delegates to {@link Runtime#availableProcessors()}. However, on Linux, this strategy
     * is insufficient, since the JVM does not take into consideration the process' CPU set affinity
     * which is employed by cgroups and numactl. Therefore this method will analyze the Linux proc filesystem
     * to make the determination. Since the CPU affinity of a process can be change at any time, this method does
     * not cache the result. Calls should be limited accordingly.
     * <br>
     * Note tha on Linux, both SMT units (Hyper-Threading) and CPU cores are counted as a processor.
     *
     * @return the available processors on this system.
     */
    public static int availableProcessors() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<Integer>) () -> Integer.valueOf(determineProcessors()))
                    .intValue();
        }

        return determineProcessors();
    }

    private static int determineProcessors() {
        int javaProcs = Runtime.getRuntime().availableProcessors();
        if (!isLinux()) {
            return javaProcs;
        }

        int maskProcs = 0;

        try {
            maskProcs = readCPUMask();
        } catch (Exception e) {
            // yum
        }

        return maskProcs > 0 ? Math.min(javaProcs, maskProcs) : javaProcs;
    }

    private static int readCPUMask() throws IOException {
        try (FileInputStream stream = new FileInputStream("/proc/self/status")) {
            try (InputStreamReader inputReader = new InputStreamReader(stream, StandardCharsets.US_ASCII)) {
                try (BufferedReader reader = new BufferedReader(inputReader)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(CPUS_ALLOWED)) {
                            int count = 0;
                            int start = CPUS_ALLOWED.length();
                            for (int i = start; i < line.length(); i++) {
                                final int v = Character.digit(line.charAt(i), 16);
                                if (v != -1) {
                                    count += Integer.bitCount(v);
                                }
                            }
                            return count;
                        }
                    }
                }
            }
        }

        return -1;
    }

    private static boolean isLinux() {
        String osArch = System.getProperty("os.name", "unknown").toLowerCase(Locale.US);
        return (osArch.contains("linux"));
    }
}
