package io.smallrye.common.os;

import java.util.Locale;

/**
 * Enumerated type for operating systems.
 */
public enum OS {

    /**
     * IBM AIX operating system.
     */
    AIX,

    /**
     * Linux-based operating system.
     */
    LINUX,

    /**
     * Apple Macintosh operating system (e.g., macOS).
     */
    MAC,

    /**
     * Oracle Solaris operating system.
     */
    SOLARIS,

    /**
     * Microsoft Windows operating system.
     */
    WINDOWS,

    /**
     * IBM z/OS.
     */
    Z,

    /**
     * Anything else different from the above.
     */
    OTHER;

    private static final OS CURRENT_OS = determineCurrentOs();

    private static OS determineCurrentOs() {
        return parse(System.getProperty("os.name", "unknown"));
    }

    static OS parse(String osName) {
        osName = osName.toLowerCase(Locale.ENGLISH);

        if (osName.contains("linux")) {
            return LINUX;
        }
        if (osName.contains("windows")) {
            return WINDOWS;
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return MAC;
        }
        if (osName.contains("sunos") || osName.contains("solaris")) {
            return SOLARIS;
        }
        if (osName.contains("aix")) {
            return AIX;
        }
        if (osName.contains("z/os")) {
            return Z;
        }
        return OTHER;
    }

    /**
     * {@return {@code true} if <em>this</em> {@code OS} is known to be the
     * operating system on which the current JVM is executing}
     */
    public boolean isCurrent() {
        return this == CURRENT_OS;
    }

    /**
     * {@return the current OS}
     */
    public static OS current() {
        return CURRENT_OS;
    }
}