package io.smallrye.common.version;

import static java.lang.invoke.MethodHandles.lookup;

import java.util.NoSuchElementException;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

// base 3000
@MessageBundle(projectCode = "SRCOM", length = 5)
interface Messages {
    Messages msg = org.jboss.logging.Messages.getBundle(lookup(), Messages.class);

    @Message(id = 3000, value = "Expected alpha part")
    IllegalStateException expectedAlpha();

    @Message(id = 3001, value = "Expected numeric part")
    IllegalStateException expectedNumber();

    @Message(id = 3002, value = "Expected separator")
    IllegalStateException expectedSep();

    @Message(id = 3003, value = "Iteration past end of version string")
    NoSuchElementException iterationPastEnd();

    @Message(id = 3004, value = "Invalid code point \"%s\" at offset %d of version string \"%s\"")
    VersionSyntaxException invalidCodePoint(String codePointString, int start, String version);

    @Message(id = 3005, value = "Version string is too long")
    VersionSyntaxException tooLong();

    @Message(id = 3006, value = "Unexpected non-numeric code point \"%s\" at offset %d of argument string \"%s\"")
    IllegalArgumentException nonNumeric(String codePointString, int offs, String value);

    @Message(id = 3007, value = "Unexpected end of version string")
    VersionSyntaxException unexpectedEnd();

    @Message(id = 3008, value = "Unexpected extra content after version string")
    VersionSyntaxException unexpectedExtra();

    @Message(id = 3009, value = "Pre-release string may not be empty")
    VersionSyntaxException emptyPreRelease();

    @Message(id = 3010, value = "Build string may not be empty")
    VersionSyntaxException emptyBuild();

    @Message(id = 3011, value = "Unbounded range: %s")
    IllegalArgumentException unboundedRange(String pattern);

    // 3012

    // 3013

    @Message(id = 3014, value = "Single version must be surrounded by []: %s")
    IllegalArgumentException singleVersionMustBeSurroundedByBrackets(String version);

    @Message(id = 3015, value = "Range defies version ordering: %s")
    IllegalArgumentException rangeDefiesVersionOrdering(String version);

    @Message(id = 3016, value = "Unexpected version range character: %s")
    IllegalArgumentException rangeUnexpected(String version);

    @Message(id = 3017, value = "Standalone version cannot have an upper bound")
    IllegalArgumentException standaloneVersionCannotBeBound();

    @Message(id = 3018, value = "Inclusive versions cannot be empty")
    IllegalArgumentException inclusiveVersionCannotBeEmpty();
}
