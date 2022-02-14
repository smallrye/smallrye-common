package io.smallrye.common.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

@SuppressWarnings("SpellCheckingInspection")
public class BasicVersionTest {

    private static void parseFail(String str) {
        try {
            parse(str);
            fail("Expected parsing to fail for " + str);
        } catch (IllegalArgumentException ok) {
        }
    }

    private static void parse(final String version) {
        VersionScheme.BASIC.validate(version);
    }

    private boolean testCompare(String v1, String v2) {
        final boolean eq = VersionScheme.BASIC.equals(v1, v2);
        //noinspection SimplifiableJUnitAssertion
        assertTrue(eq == VersionScheme.BASIC.equals(v2, v1));
        assertEquals(Integer.signum(VersionScheme.BASIC.compare(v1, v2)), -Integer.signum(VersionScheme.BASIC.compare(v2, v1)));
        return eq;
    }

    @Test
    public void testBasicParsing() {
        parse("1");
        parse("a");
        parseFail(".");
        parse("1.1");
        parse("1.a");
        parse("1a");
        parse("a1");
        parse("1+1");
        parse("1-1");
        parse("1_1");
        parse("1_1a.1993-12-31");
        parseFail("1.");
        parseFail("1..");
        parseFail(".1");
    }

    @Test
    public void testBasicEquals() {
        assertTrue(testCompare("1", "1.0"));
        assertTrue(testCompare("1.0", "1.0"));
        assertTrue(testCompare("a1", "a1"));
        assertTrue(testCompare("1.1", "1.01"));
        assertFalse(testCompare("1.1", "1.0"));
        assertFalse(testCompare("1.1", "1.10"));
        assertFalse(testCompare("1.1", "1.100"));
        assertFalse(testCompare("1.1", "1-1"));
        assertFalse(testCompare("1.1", "1_1"));
        assertFalse(testCompare("1.1", "1+1"));
        assertFalse(testCompare("1-1", "1+1"));
        assertFalse(testCompare("1_1", "1+1"));
        assertFalse(testCompare("1_1", "1-1"));
    }

    @Test
    public void testCompareTo() {
        assertEquals(0, VersionScheme.BASIC.compare("1.0", "1.0"));
        assertEquals(0, VersionScheme.BASIC.compare("1.0", "1.0.0"));
        assertEquals(0, VersionScheme.BASIC.compare("1.0.0.0", "1.0.0"));
        assertEquals(1, VersionScheme.BASIC.compare("5u1", "5"));
        assertEquals(-1, VersionScheme.BASIC.compare("5u1", "5.1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5-1", "5.1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5_1", "5.1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5_1.1", "5.1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5_foo.1", "5.1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5+1", "5.1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5+1", "5-1+1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5+1", "5-1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5-alpha", "5-1"));
        assertEquals(-1, VersionScheme.BASIC.compare("5-alpha", "5.0"));
        assertEquals(-1, VersionScheme.BASIC.compare("5-alpha", "5"));
        assertEquals(1, VersionScheme.BASIC.compare("5.1-alpha", "5"));
        assertEquals(-1, VersionScheme.BASIC.compare("alpha", "5"));
    }
}
