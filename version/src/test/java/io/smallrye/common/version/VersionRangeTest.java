package io.smallrye.common.version;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VersionRangeTest {

    @Test
    void testVersionRangeWithInclusive() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "[1.0,2.0]");
        assertTrue(versionRange.test("1.0.0"));
        assertTrue(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.899.0"));
        assertTrue(versionRange.test("2.0"));
        assertTrue(versionRange.test("2.0.0"));
        assertFalse(versionRange.test("2.0.1"));
    }

    @Test
    void testVersionRangeWithExclusive() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "(1.0,2.0)");
        assertFalse(versionRange.test("1.0.0"));
        assertTrue(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.899.0"));
        assertFalse(versionRange.test("2.0"));
        assertFalse(versionRange.test("2.0.0"));
        assertFalse(versionRange.test("2.0.1"));
    }

    @Test
    void testVersionRangeWithLowerBoundExclusive() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "(1.0,2.0]");
        assertFalse(versionRange.test("1.0.0"));
        assertTrue(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.899.0"));
        assertTrue(versionRange.test("2.0"));
        assertTrue(versionRange.test("2.0.0"));
        assertFalse(versionRange.test("2.0.1"));
    }

    @Test
    void testVersionRangeWithUpperBoundExclusive() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "[1.0,2.0)");
        assertTrue(versionRange.test("1.0.0"));
        assertTrue(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.899.0"));
        assertFalse(versionRange.test("2.0"));
        assertFalse(versionRange.test("2.0.0"));
        assertFalse(versionRange.test("2.0.1"));
    }

    @Test
    public void testUnboundedRange() {
        assertThrows(IllegalArgumentException.class, () -> VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "[1.0,2.0"));
    }
}
