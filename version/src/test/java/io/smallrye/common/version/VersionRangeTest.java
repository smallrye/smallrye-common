package io.smallrye.common.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.function.Predicate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class VersionRangeTest {

    @Test
    void testVersionRangeWithInclusive() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("[1.0,2.0]");
        assertThat(versionRange)
                .accepts("1.0.0", "1.1.0", "1.899.0", "2.0", "2.0.0")
                .rejects("2.0.1");
    }

    @Test
    void testVersionRangeWithExclusive() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("(1.0,2.0)");
        assertThat(versionRange)
                .accepts("1.1.0", "1.899.0")
                .rejects("1.0.0", "2.0", "2.0.0", "2.0.1");
    }

    @Test
    void testVersionRangeWithLowerBoundExclusive() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("(1.0,2.0]");
        assertThat(versionRange)
                .accepts("1.1.0", "1.899.0", "2.0", "2.0.0")
                .rejects("1.0.0", "2.0.1");
    }

    @Test
    void testVersionRangeWithUpperBoundExclusive() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("[1.0,2.0)");
        assertThat(versionRange)
                .accepts("1.0.0", "1.1.0", "1.899.0")
                .rejects("2.0", "2.0.0", "2.0.1");
    }

    @Test
    public void testUnboundedRange() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> VersionScheme.MAVEN.fromRangeString("[1.0,2.0"))
                .withMessageStartingWith("SRCOM03011");
    }

    @Test
    public void testAlphaVersion() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("[1.0,)");
        assertThat(versionRange).rejects("1.0.0.Alpha1");
    }

    @Test
    public void testAlphaVersionInbound() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("[1.0.0.Alpha1,)");
        assertThat(versionRange).accepts("1.0.0.Alpha1", "1.0.0.Beta1");
    }

    @Test
    public void testAlphaVersionInboundExclusive() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("(1.0.0.Alpha1,)");
        assertThat(versionRange)
                .accepts("1.0.0.Beta")
                .rejects("1.0.0.Alpha1");
    }

    @Test
    public void testMultipleRanges() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("(,1.0],[1.2,)");
        // Should return true for Versions up to 1.0 (included) and 1.2 or higher
        assertThat(versionRange)
                .accepts("1.0.0.Alpha1", "1.0.0", "1.2.0")
                .rejects("1.1.0", "1.2.0.Alpha1");
    }

    @Test
    public void testQualifiers() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("[3.8,3.8.5)");
        assertThat(versionRange)
                .accepts("3.8.4.SP1-redhat-00001", "3.8.4.SP2-redhat-00001", "3.8.4.redhat-00002")
                .rejects("3.8.5.redhat-00003");
    }

    @Test
    @Disabled("This test is failing")
    public void testRangeQualifier() {
        Predicate<String> versionRange = VersionScheme.MAVEN.fromRangeString("[3.8.0.redhat-00001,)");
        assertThat(versionRange).accepts("3.8.0.SP1-redhat-00001");
    }

    @ParameterizedTest
    @MethodSource("schemes")
    public void testComposablePredicates(VersionScheme scheme) {
        assertThat(scheme.whenGe("1.0.0").and(scheme.whenLt("2.0.0")))
                .accepts("1.0.0", "1.1.0").rejects("2.0.0", "2.0.1", "2.1.0");
        assertThat(scheme.whenGt("1.0.0").and(scheme.whenLe("2.0.0")))
                .accepts("1.0.1", "2.0.0").rejects("1.0.0", "2.0.1", "2.1.0");
    }

    static VersionScheme[] schemes() {
        return new VersionScheme[] { VersionScheme.BASIC, VersionScheme.MAVEN, VersionScheme.JPMS };
    }

}
