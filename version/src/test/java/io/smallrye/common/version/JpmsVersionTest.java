package io.smallrye.common.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings("SpellCheckingInspection")
public class JpmsVersionTest {

    @Test
    public void testJpmsCompare() {
        checkJpmsConsistency("1", "1.0");
        // empty comes before zero
        checkJpmsConsistency("1.0-0", "1");
        checkJpmsConsistency("1.0-0+y", "1+y");
        // zero trailers are trimmed
        checkJpmsConsistency("1.0-0+y", "1-0+y");
        checkJpmsConsistency("1.0-x.0+y", "1-x+y");
        // other
        checkJpmsConsistency("1", "1.x");
        checkJpmsConsistency("1x", "1.x");
        checkJpmsConsistency("1_x", "1.x");
        checkJpmsConsistency("12", "1.2");
        checkJpmsConsistency("12", "1-2");
        checkJpmsConsistency("12", "1+2");
        checkJpmsConsistency("1-2", "1+2");
        checkJpmsConsistency("1.2-2", "1+2");
        checkJpmsConsistency("1.2-2", "1-2");
        checkJpmsConsistency("1-2+3", "1+2-3");
        checkJpmsConsistency("1-3", "1+2-3");
    }

    private static void checkJpmsConsistency(String v1, String v2) {
        assumeTrue(JDKSpecific.hasJpms());
        assertEquals(JDKSpecific.compareJpms(v1, v2), VersionScheme.JPMS.compare(v1, v2));
        assertEquals(JDKSpecific.compareJpms(v2, v1), VersionScheme.JPMS.compare(v2, v1));
    }
}
