package io.smallrye.common.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 */
public final class ResourceUtilsTests {

    @Test
    public void testEmpty() {
        assertCanonical("", "");
        assertCanonical("", ".");
        assertCanonical("", "..");
        assertCanonical("", "/");
        assertCanonical("", "/.");
        assertCanonical("", "./");
        assertCanonical("", "./.");
        assertCanonical("", "../.");
        assertCanonical("", "../");
        assertCanonical("", "..///");
        assertCanonical("", "..//./.");
        assertCanonical("", "..//././//");
    }

    @Test
    public void testSimple() {
        assertCanonical("foo", "foo");
        assertCanonical("foo", "./foo");
        assertCanonical("foo", "../foo");
        assertCanonical("foo", "/foo");
        assertCanonical("foo", "/foo/");
        assertCanonical("foo", "/foo/.");
        assertCanonical("foo", "./foo/.");
        assertCanonical("foo", "../foo/.");
        assertCanonical("foo", "/foo/.//");
        assertCanonical("foo", "./foo/.//");
        assertCanonical("foo", "../foo/.//");
        assertCanonical("foo", "foo/.//");
        assertCanonical("foo", "foo//.//");
    }

    @Test
    public void testDotDot() {
        assertCanonical("foo", "foo/bar/..");
        assertCanonical("foo", "/foo/bar/..");
        assertCanonical("foo", "./foo/bar/..");
        assertCanonical("foo", "../foo/bar/..");
        assertCanonical("foo", "bar/../foo");
        assertCanonical("foo", "/bar/../foo");
        assertCanonical("foo", "./bar/../foo");
        assertCanonical("foo", "../bar/../foo");
        assertCanonical("foo", "../bar/../baz/../foo");
        assertCanonical("foo", "bar/baz/../../foo");
        assertCanonical("foo/bar", "foo/bat/baz/../../bar");
        assertCanonical("foo/bar", "foo/bat/../baz/../bar");
    }

    @Test
    public void testCopying() {
        assertCanonical("foo/bar/buz", "foo/bat/../baz/../bar/./buz/.");
        assertCanonical("foo/bar/buz", "foo/././bar/././buz/.");
        assertCanonical("foo/bar/buz", "./foo/././bar/././buz/.");
        assertCanonical("foo/bar/buz", "./foo/././bar/././buz/");
        assertCanonical("foo/bar/buz", "./foo/././bar/././buz//");
        assertCanonical("foo/bar/buz", "./foo/.//./bar/././buz");
        assertCanonical("foo/bar/buz", "../foo/././bar/././buz/.");
    }

    @Test
    public void testWithDot() {
        assertCanonical("Object.class", "Object.class");
        assertCanonical("Object.class", "/Object.class");
        assertCanonical("Object.class", "./Object.class");
        assertCanonical("foo.", "foo.");
        assertCanonical("foo.", "foo./");
        assertCanonical("foo.", "foo./.");
        assertCanonical("foo..", "foo..");
        assertCanonical("foo..", "foo../");
        assertCanonical("foo..", "foo../.");
        assertCanonical("foo..", "foo.././");
        assertCanonical(".foo", ".foo");
        assertCanonical(".foo", "/.foo");
        assertCanonical(".foo", "./.foo");
    }

    static void assertCanonical(String expect, String path) {
        Assertions.assertEquals(expect, ResourceUtils.canonicalizeRelativePath(path),
                "Path \"" + path + "\" canonicalize to \"" + expect + "\"");
    }
}
