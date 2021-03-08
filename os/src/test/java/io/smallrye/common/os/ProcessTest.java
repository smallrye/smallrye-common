package io.smallrye.common.os;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ProcessTest {

    @Test
    public void testProcessInfo() {
        assertNotEquals(0L, Process.getProcessId());
        assertNotNull(Process.getProcessName());
        assertNotNull(Process.getCurrentProcess());
    }

    @Test
    public void testAllProcessInfo() {
        assertFalse(Process.getAllProcesses().isEmpty());
    }

}
