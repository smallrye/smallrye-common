package io.smallrye.common.os;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

class OSTest {

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.LINUX)
    void testLinux() {
        assertTrue(OS.LINUX.isCurrent());
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    void testWindows() {
        assertTrue(OS.WINDOWS.isCurrent());
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.MAC)
    void testMacOS() {
        assertTrue(OS.MAC.isCurrent());
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.SOLARIS)
    void testSolaris() {
        assertTrue(OS.SOLARIS.isCurrent());
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.AIX)
    void testAIX() {
        assertTrue(OS.AIX.isCurrent());
    }

}
