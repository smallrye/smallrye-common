package io.smallrye.common.cpu;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 *
 */
public final class CPUTests {

    @Test
    public void testCpu() {
        String expectedCpuName = System.getProperty("expected-cpu");
        Assumptions.assumeTrue(expectedCpuName != null && !expectedCpuName.isEmpty());
        Assertions.assertEquals(CPU.host().name(), expectedCpuName);
    }
}
