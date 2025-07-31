package io.smallrye.common.process;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ProcessUtilTest {

    @Test
    void testSearchPath() {
        assertFalse(ProcessUtil.searchPath().isEmpty());
    }
}
