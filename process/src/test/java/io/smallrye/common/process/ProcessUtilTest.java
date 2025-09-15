package io.smallrye.common.process;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class ProcessUtilTest {

    @Test
    void testSearchPath() {
        assertFalse(ProcessUtil.searchPath().isEmpty());
    }
}
