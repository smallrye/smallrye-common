package io.smallrye.common.version;

import static org.junit.Assert.assertEquals;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Test;

@SuppressWarnings("SpellCheckingInspection")
public class MavenVersionTest {

    @Test
    public void testMavenCompare() {
        checkMavenConsistency("x", "x.0");
        checkMavenConsistency("x", "x.x");
        checkMavenConsistency("0.1", "0-1");
        checkMavenConsistency("0.x", "x");
        checkMavenConsistency("0.x", "0-x");
        checkMavenConsistency("1.1", "1-1");
        checkMavenConsistency("1.x", "1-x");
        checkMavenConsistency("0.1", "1-1");
        checkMavenConsistency("0.x", "1-x");
        checkMavenConsistency("1.1", "0-1");
        checkMavenConsistency("1.x", "0-x");
        checkMavenConsistency("1.0", "1.1");
        checkMavenConsistency("1.0", "1_1");
        checkMavenConsistency("1.1", "1_0");
        checkMavenConsistency("1.1", "1_1");
        checkMavenConsistency("1.x", "1.y");
        checkMavenConsistency("1.x", "1_y");
        checkMavenConsistency("1.y", "1_x");
        checkMavenConsistency("1.y", "1_x");
        checkMavenConsistency("1.y+z", "1_x");
        checkMavenConsistency("1.y+0", "1_x+0");
        checkMavenConsistency("0.1-0-123.xyz-alpha", "0.1");
        checkMavenConsistency("1.0.0-0-final-0", "1");
        checkMavenConsistency("1234", "abcd");
        checkMavenConsistency("1.0.a", "1.0.A");
        checkMavenConsistency("1.0.ga", "1.0.final");
        checkMavenConsistency("1-23", "1.23");
        checkMavenConsistency("1-alpha", "1.alpha");
        checkMavenConsistency("1-0.beta", "1-0.alpha");
        checkMavenConsistency("12-foo", "foo-12");
        checkMavenConsistency("12.foo", "foo.12");
        checkMavenConsistency("foo", "FOO");
        checkMavenConsistency("12-foo", "12_FOO");
        checkMavenConsistency("0_0", "0");
        checkMavenConsistency("0_0", "0_0final");
        // https://issues.apache.org/jira/browse/MNG-6964
        // checkMavenConsistency("1-0.alpha", "1");
        assertEquals(1, VersionScheme.MAVEN.compare("1-0.alpha", "1"));
        // https://issues.apache.org/jira/browse/MNG-6964
        //checkMavenConsistency("1-0.beta", "1");
        assertEquals(1, VersionScheme.MAVEN.compare("1-0.beta", "1"));
    }

    private static void checkMavenConsistency(String v1, String v2) {
        // Maven's comparator may return numbers outside the set of (-1, 0, 1)
        assertEquals(Integer.signum(new ComparableVersion(v1).compareTo(new ComparableVersion(v2))),
                VersionScheme.MAVEN.compare(v1, v2));
        assertEquals(Integer.signum(new ComparableVersion(v2).compareTo(new ComparableVersion(v1))),
                VersionScheme.MAVEN.compare(v2, v1));
    }

    @Test
    public void testMavenCanonicalize() {
        checkMavenCanonical("1");
        checkMavenCanonical("1.0");
        checkMavenCanonical("1-0");
        checkMavenCanonical("1--0");
        checkMavenCanonical("1-ga-0");
        checkMavenCanonical("1.ga-0");
        checkMavenCanonical("1_0");
        checkMavenCanonical("1+_0");
        checkMavenCanonical("ga");
        checkMavenCanonical("...");
    }

    private static void checkMavenCanonical(String str) {
        assertEquals(new ComparableVersion(str).getCanonical(), VersionScheme.MAVEN.canonicalize(str));
    }
}
