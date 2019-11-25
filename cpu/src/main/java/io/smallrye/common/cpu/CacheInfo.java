package io.smallrye.common.cpu;

import static java.security.AccessController.doPrivileged;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Locale;

/**
 * A class which exposes any available cache line information for the current CPU.
 */
public final class CacheInfo {
    private static final CacheLevelInfo[] cacheLevels;

    /**
     * Get the number of CPU cache level entries. If no cache information could be gathered, 0 is returned.
     *
     * @return the number of CPU cache levels, or 0 if unknown
     */
    public static int getLevelEntryCount() {
        return cacheLevels.length;
    }

    /**
     * Get the CPU cache level information for a cache level. The {@code index} argument must be greater than zero and
     * less than the number of levels returned by {@link #getLevelEntryCount()}.
     *
     * @param index the cache level index
     * @return the CPU cache level information
     */
    public static CacheLevelInfo getCacheLevelInfo(int index) {
        return cacheLevels[index];
    }

    /**
     * Get the smallest known data cache line size. If no cache line sizes are known, 0 is returned. Note that smaller
     * cache lines may exist if one or more cache line sizes are unknown.
     *
     * @return the smallest cache line size, or 0 if unknown
     */
    public static int getSmallestDataCacheLineSize() {
        int minSize = Integer.MAX_VALUE;
        for (CacheLevelInfo cacheLevel : cacheLevels) {
            if (cacheLevel.getCacheType().isData()) {
                final int cacheLineSize = cacheLevel.getCacheLineSize();
                if (cacheLineSize != 0 && cacheLineSize < minSize) {
                    minSize = cacheLineSize;
                }
            }
        }
        return minSize == Integer.MAX_VALUE ? 0 : minSize;
    }

    /**
     * Get the smallest known instruction cache line size. If no cache line sizes are known, 0 is returned. Note that smaller
     * cache lines may exist if one or more cache line sizes are unknown.
     *
     * @return the smallest cache line size, or 0 if unknown
     */
    public static int getSmallestInstructionCacheLineSize() {
        int minSize = Integer.MAX_VALUE;
        for (CacheLevelInfo cacheLevel : cacheLevels) {
            if (cacheLevel.getCacheType().isInstruction()) {
                final int cacheLineSize = cacheLevel.getCacheLineSize();
                if (cacheLineSize != 0 && cacheLineSize < minSize) {
                    minSize = cacheLineSize;
                }
            }
        }
        return minSize == Integer.MAX_VALUE ? 0 : minSize;
    }

    static {
        cacheLevels = doPrivileged((PrivilegedAction<CacheLevelInfo[]>) () -> {
            try {
                String osArch = System.getProperty("os.name", "unknown").toLowerCase(Locale.US);
                if (osArch.contains("linux")) {
                    // try to read /sys fs
                    final File cpu0 = new File("/sys/devices/system/cpu/cpu0/cache");
                    if (cpu0.exists()) {
                        // great!
                        final File[] files = cpu0.listFiles();
                        if (files != null) {
                            ArrayList<File> indexes = new ArrayList<File>();
                            for (File file : files) {
                                if (file.getName().startsWith("index")) {
                                    indexes.add(file);
                                }
                            }
                            final CacheLevelInfo[] levelInfoArray = new CacheLevelInfo[indexes.size()];
                            for (int i = 0; i < indexes.size(); i++) {
                                File file = indexes.get(i);
                                int index = parseIntFile(new File(file, "level"));
                                final CacheType type;
                                switch (parseStringFile(new File(file, "type"))) {
                                    case "Data":
                                        type = CacheType.DATA;
                                        break;
                                    case "Instruction":
                                        type = CacheType.INSTRUCTION;
                                        break;
                                    case "Unified":
                                        type = CacheType.UNIFIED;
                                        break;
                                    default:
                                        type = CacheType.UNKNOWN;
                                        break;
                                }
                                int size = parseIntKBFile(new File(file, "size"));
                                int lineSize = parseIntFile(new File(file, "coherency_line_size"));
                                levelInfoArray[i] = new CacheLevelInfo(index, type, size, lineSize);
                            }
                            return levelInfoArray;
                        }
                    }
                } else if (osArch.contains("mac os x")) {
                    // cache line size
                    final int lineSize = safeParseInt(parseProcessOutput("/usr/sbin/sysctl", "-n", "hw.cachelinesize"));
                    if (lineSize != 0) {
                        // cache sizes
                        final int l1d = safeParseInt(parseProcessOutput("/usr/sbin/sysctl", "-n", "hw.l1dcachesize"));
                        final int l1i = safeParseInt(parseProcessOutput("/usr/sbin/sysctl", "-n", "hw.l1icachesize"));
                        final int l2 = safeParseInt(parseProcessOutput("/usr/sbin/sysctl", "-n", "hw.l2cachesize"));
                        final int l3 = safeParseInt(parseProcessOutput("/usr/sbin/sysctl", "-n", "hw.l3cachesize"));
                        ArrayList<CacheLevelInfo> list = new ArrayList<CacheLevelInfo>();
                        if (l1d != 0) {
                            list.add(new CacheLevelInfo(1, CacheType.DATA, l1d / 1024, lineSize));
                        }
                        if (l1i != 0) {
                            list.add(new CacheLevelInfo(1, CacheType.INSTRUCTION, l1i / 1024, lineSize));
                        }
                        if (l2 != 0) {
                            list.add(new CacheLevelInfo(2, CacheType.UNIFIED, l2 / 1024, lineSize));
                        }
                        if (l3 != 0) {
                            list.add(new CacheLevelInfo(3, CacheType.UNIFIED, l3 / 1024, lineSize));
                        }
                        if (list.size() > 0) {
                            return list.toArray(new CacheLevelInfo[list.size()]);
                        }
                    }
                } else if (osArch.contains("windows")) {
                    // TODO: use the wmic utility to get cache line info
                }
            } catch (Throwable ignored) {
            }
            // all has failed
            return new CacheLevelInfo[0];
        });
    }

    static int parseIntFile(final File file) {
        return safeParseInt(parseStringFile(file));
    }

    static int safeParseInt(final String string) {
        try {
            return Integer.parseInt(string);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    static int parseIntKBFile(final File file) {
        try {
            final String s = parseStringFile(file);
            if (s.endsWith("K")) {
                return Integer.parseInt(s.substring(0, s.length() - 1));
            } else if (s.endsWith("M")) {
                return Integer.parseInt(s.substring(0, s.length() - 1)) * 1024;
            } else if (s.endsWith("G")) {
                return Integer.parseInt(s.substring(0, s.length() - 1)) * 1024 * 1024;
            } else {
                return Integer.parseInt(s);
            }
        } catch (Throwable ignored) {
            return 0;
        }
    }

    static String parseStringFile(final File file) {
        try (FileInputStream is = new FileInputStream(file)) {
            return parseStringStream(is);
        } catch (Throwable ignored) {
            return "";
        }
    }

    static String parseStringStream(final InputStream is) {
        try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            StringBuilder b = new StringBuilder();
            char[] cb = new char[64];
            int res;
            while ((res = r.read(cb)) != -1) {
                b.append(cb, 0, res);
            }
            return b.toString().trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    static String parseProcessOutput(final String... args) {
        final ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            final Process process = processBuilder.start();
            process.getOutputStream().close();
            final InputStream errorStream = process.getErrorStream();

            final Thread errorThread = new Thread(null, new StreamConsumer(errorStream), "Process thread", 32768L);
            errorThread.start();

            final String result;
            try (final InputStream inputStream = process.getInputStream()) {
                result = parseStringStream(inputStream);
            }

            boolean intr = false;
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                intr = true;
                return null;
            } finally {
                try {
                    errorThread.join();
                } catch (InterruptedException e) {
                    intr = true;
                } finally {
                    if (intr) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return result;
        } catch (IOException e) {
            return "";
        }
    }

    static class StreamConsumer implements Runnable {

        private final InputStream stream;

        StreamConsumer(final InputStream stream) {
            this.stream = stream;
        }

        public void run() {
            byte[] buffer = new byte[128];
            try {
                while (stream.read(buffer) != -1)
                    ;
            } catch (IOException ignored) {
            } finally {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Detected cache info:");
        for (CacheLevelInfo levelInfo : cacheLevels) {
            System.out.printf("Level %d cache: type %s, size %d KiB, cache line is %d bytes%n",
                    Integer.valueOf(levelInfo.getCacheLevel()),
                    levelInfo.getCacheType(),
                    Integer.valueOf(levelInfo.getCacheLevelSizeKB()),
                    Integer.valueOf(levelInfo.getCacheLineSize()));
        }
    }
}