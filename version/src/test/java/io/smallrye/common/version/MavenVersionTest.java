package io.smallrye.common.version;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.junit.jupiter.api.Test;

@SuppressWarnings("SpellCheckingInspection")
public class MavenVersionTest {

    @Test
    public void testMavenCompare() throws Exception {
        checkMavenConsistency("x", "x.0");
        checkMavenConsistency("x", "x.x");
        checkMavenConsistency("0.1", "0-1");
        // https://issues.apache.org/jira/browse/MNG-7701
        // checkMavenConsistency("0.x", "x");
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
        // https://issues.apache.org/jira/browse/MNG-7701
        checkMavenConsistency("1.x", "1_y");
        checkMavenConsistency("1.y", "1_x");
        checkMavenConsistency("1.y+z", "1_x");
        checkMavenConsistency("1.y+0", "1_x+0");
        checkMavenConsistency("0.1-0-123.xyz-alpha", "0.1");
        checkMavenConsistency("1.0.0-0-final-0", "1");
        checkMavenConsistency("1234", "abcd");
        checkMavenConsistency("1.0.a", "1.0.A");
        checkMavenConsistency("1.0.ga", "1.0.final");
        checkMavenConsistency("1-23", "1.23");
        // https://issues.apache.org/jira/browse/MNG-7701
        checkMavenConsistency("1-alpha", "1.alpha");
        checkMavenConsistency("1-0.beta", "1-0.alpha");
        checkMavenConsistency("12-foo", "foo-12");
        checkMavenConsistency("12.foo", "foo.12");
        checkMavenConsistency("foo", "FOO");
        checkMavenConsistency("12-foo", "12_FOO");
        checkMavenConsistency("0_0", "0");
        checkMavenConsistency("0_0", "0_0final");
        // https://issues.apache.org/jira/browse/MNG-6964
        checkMavenConsistency("1-0.alpha", "1");
        // https://issues.apache.org/jira/browse/MNG-6964
        checkMavenConsistency("1-0.beta", "1");
        // #267
        checkMavenConsistency("3.6.0.CR1", "3.6.0");
        checkMavenConsistency("3.6.0.FINAL", "3.6.0");
        checkMavenConsistency("3.6.0.SP1", "3.6.0");
    }

    @Test
    public void testMavenCompare2() throws Exception {
        checkMavenConsistency("3.6.0-CR1", "3.6.0");
        checkMavenConsistency("3.6.0-FINAL", "3.6.0");
        checkMavenConsistency("3.6.0-SP1", "3.6.0");
        checkMavenConsistency("2.13.5.SP1.redhat-00002", "2.13.5.0.redhat-00002");
        checkMavenConsistency("2.13.5.SP1.redhat-00002", "2.13.5.Final.redhat-00002");
        checkMavenConsistency("2.13.5.SP1-redhat-00002", "2.13.5.Final-redhat-00002");
        checkMavenConsistency("2.13.5-redhat-00002", "2.13.5.Final-redhat-00002");
        checkMavenConsistency("2.1", "2.0.0.0.0.1");
        checkMavenConsistency("2.0.0.0.0.1", "2.final.0.0.0.1");
        checkMavenConsistency("2.1", "2.0.0.final.0.1");
        checkMavenConsistency("2.sp", "2.0.0.0.0.sp");
        checkMavenConsistency("2.beta", "2.0.0.0.0.beta");
        checkMavenConsistency("2.0.0.foo", "2.0.foo");
        checkMavenConsistency("4.0.4", "4.final.4");
        checkMavenConsistency("4.0.4", "4..4");
        checkMavenConsistency("4.final.4", "4..4");
        checkMavenConsistency("4.final.4", "4.0.4");
        checkMavenConsistency("0.4.final.4", "0.4.0.4");
    }

    private static final org.eclipse.aether.version.VersionScheme MR_SCHEME = new GenericVersionScheme();

    private static void checkMavenConsistency(String v1, String v2) throws InvalidVersionSpecificationException {
        Version pv1 = MR_SCHEME.parseVersion(v1);
        Version pv2 = MR_SCHEME.parseVersion(v2);
        // Maven's comparator may return numbers outside the set of (-1, 0, 1)
        int v1v2 = Integer.signum(pv1.compareTo(pv2));
        int v2v1 = Integer.signum(pv2.compareTo(pv1));
        if (v1v2 != -v2v1) {
            System.out.printf(
                    "Skipping check for \"%s\" <-> \"%s\" due to internal inconsistency in the version of maven-resolver-util in use%n",
                    v1, v2);
        } else {
            assertEquals(v1v2, VersionScheme.MAVEN.compare(v1, v2));
            assertEquals(v2v1, VersionScheme.MAVEN.compare(v2, v1));
        }
    }

    @Test
    public void testMavenCanonicalize() throws Exception {
        checkMavenCanonical("1.0.1");
        checkMavenCanonical("0.1.0");
        checkMavenCanonical("0.0.1");
        checkMavenCanonical("0.1");
        checkMavenCanonical("0-1");
        checkMavenCanonical("0.x");
        checkMavenCanonical("0-x");
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

    private static void checkMavenCanonical(String str) throws InvalidVersionSpecificationException {
        Version parsed = MR_SCHEME.parseVersion(str);
        Version canon = MR_SCHEME.parseVersion(VersionScheme.MAVEN.canonicalize(str));
        assertEquals(canon, parsed);
    }
}
