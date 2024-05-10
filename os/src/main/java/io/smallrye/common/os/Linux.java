package io.smallrye.common.os;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utilities pertaining to the Linux operating system.
 */
public final class Linux {
    private Linux() {
    }

    /**
     * {@return true if the operating system is the Windows Subsystem for Linux, or false if it is not}
     */
    public static boolean isWSL() {
        return WSL.version >= 1;
    }

    /**
     * {@return true if the WSL version is 2 or later, or false if it is not}
     */
    public static boolean isWSLv2() {
        return WSL.version >= 2;
    }

    /**
     * Lazy constants for WSL.
     */
    private static final class WSL {
        private static final int version;

        static {
            if (OS.current() != OS.LINUX) {
                version = 0;
            } else {
                int v;
                try {
                    String procVersion = Files.readString(Path.of("/proc/version"));
                    if (procVersion.contains("Microsoft")) {
                        // likely version 1
                        v = 1;
                    } else if (procVersion.contains("microsoft")) {
                        // likely version 2 or newer
                        v = 2;
                    } else {
                        v = 0;
                    }
                } catch (IOException e) {
                    v = 0;
                }
                version = v;
            }
        }
    }
}
