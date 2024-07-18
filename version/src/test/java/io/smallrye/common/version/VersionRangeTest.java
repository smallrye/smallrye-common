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

    @Test
    public void testAlphaVersion() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "[1.0,)");
        assertFalse(versionRange.test("1.0.0.Alpha"));
    }

    @Test
    public void testAlphaVersionInbound() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "[1.0.0.Alpha1,)");
        assertTrue(versionRange.test("1.0.0.Alpha1"));
        assertTrue(versionRange.test("1.0.0.Beta"));
    }

    @Test
    public void testAlphaVersionInboundExclusive() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "(1.0.0.Alpha1,)");
        assertFalse(versionRange.test("1.0.0.Alpha1"));
        assertTrue(versionRange.test("1.0.0.Beta"));
    }

    @Test
    public void testMultipleRanges() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "(,1.0],[1.2,)");
        // Should return true for Versions up to 1.0 (included) and 1.2 or higher
        assertTrue(versionRange.test("1.0.0.Alpha1"));
        assertTrue(versionRange.test("1.0.0"));
        assertFalse(versionRange.test("1.1.0"));
        assertTrue(versionRange.test("1.2.0"));
        assertFalse(versionRange.test("1.2.0.Alpha1"));
    }

    @Test
    public void testQualifiers() {
        VersionRange versionRange = VersionRange.createFromVersionSpec(VersionScheme.MAVEN, "[3.8,3.8.5)");
        assertTrue(versionRange.test("3.8.4.SP1-redhat-00001"));
        assertTrue(versionRange.test("3.8.4.SP2-redhat-00001"));
        assertTrue(versionRange.test("3.8.4.redhat-00002"));
        assertFalse(versionRange.test("3.8.5.redhat-00003"));
    }

}
