package io.smallrye.common.os;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ProcessTest {

    @Test
    public void testProcessInfo() {
        assertNotEquals(0L, Process.getProcessId());
        assertNotNull(Process.getProcessName());
    }

}
