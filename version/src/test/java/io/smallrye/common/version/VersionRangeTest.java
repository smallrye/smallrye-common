package io.smallrye.common.version;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VersionRangeTest {

    @Test
    void testVersionRangeWithInclusive() {
        VersionRange versionRange = new VersionRange(VersionScheme.MAVEN, "[1.0,2.0]");
        assertTrue(versionRange.test("1.0.0"));
        assertTrue(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.899.0"));
        assertTrue(versionRange.test("2.0"));
        assertTrue(versionRange.test("2.0.0"));
        assertFalse(versionRange.test("2.0.1"));
    }

    @Test
    void testVersionRangeWithExclusive() {
        VersionRange versionRange = new VersionRange(VersionScheme.MAVEN, "(1.0,2.0)");
        assertFalse(versionRange.test("1.0.0"));
        assertTrue(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.899.0"));
        assertFalse(versionRange.test("2.0"));
        assertFalse(versionRange.test("2.0.0"));
        assertFalse(versionRange.test("2.0.1"));
    }

    @Test
    void testVersionRangeWithLowerBoundExclusive() {
        VersionRange versionRange = new VersionRange(VersionScheme.MAVEN, "(1.0,2.0]");
        assertFalse(versionRange.test("1.0.0"));
        assertTrue(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.899.0"));
        assertTrue(versionRange.test("2.0"));
        assertTrue(versionRange.test("2.0.0"));
        assertFalse(versionRange.test("2.0.1"));
    }

    @Test
    void testVersionRangeWithUpperBoundExclusive() {
        VersionRange versionRange = new VersionRange(VersionScheme.MAVEN, "[1.0,2.0)");
        assertTrue(versionRange.test("1.0.0"));
        assertTrue(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.899.0"));
        assertFalse(versionRange.test("2.0"));
        assertFalse(versionRange.test("2.0.0"));
        assertFalse(versionRange.test("2.0.1"));
    }

    @Test
    public void testVersionRangeWithInvalidRangePattern() {
        assertThrows(IllegalArgumentException.class, () -> new VersionRange(VersionScheme.MAVEN, "1.0,2.0"),
                "Invalid range pattern: 1.0,2.0");
    }
}
