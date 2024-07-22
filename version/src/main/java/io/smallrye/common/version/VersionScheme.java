package io.smallrye.common.version;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A versioning scheme, which has distinct sorting, iteration, and canonicalization rules.
 */
public interface VersionScheme extends Comparator<String> {
    /**
     * Compare two versions according to this version scheme.
     *
     * @param v1 the first version (must not be {@code null})
     * @param v2 the second version (must not be {@code null})
     * @return {@code -1}, {@code 0}, or {@code 1} if the first version is less than, equal to, or greater than the
     *         second version according to this version scheme
     * @throws VersionSyntaxException if one or both of the versions have syntactic errors according to this scheme
     */
    int compare(String v1, String v2);

    /**
     * Determine if the first version is less than the second version according to this version scheme.
     *
     * @param base the base version
     * @param other the other version
     * @return {@code true} if the first version is less than the second version, or {@code false} otherwise
     */
    default boolean lt(String base, String other) {
        return compare(base, other) < 0;
    }

    /**
     * Determine if the first version is less than or equal to the second version according to this version scheme.
     *
     * @param base the base version
     * @param other the other version
     * @return {@code true} if the first version is less than or equal to the second version, or {@code false} otherwise
     */
    default boolean le(String base, String other) {
        return compare(base, other) <= 0;
    }

    /**
     * Determine if the first version is greater than or equal to the second version according to this version scheme.
     *
     * @param base the base version
     * @param other the other version
     * @return {@code true} if the first version is greater than or equal to the second version, or {@code false} otherwise
     */
    default boolean gt(String base, String other) {
        return compare(base, other) > 0;
    }

    /**
     * Determine if the first version is greater than the second version according to this version scheme.
     *
     * @param base the base version
     * @param other the other version
     * @return {@code true} if the first version is greater than the second version, or {@code false} otherwise
     */
    default boolean ge(String base, String other) {
        return compare(base, other) >= 0;
    }

    /**
     * {@return the lesser (earlier) of the two versions}
     *
     * @param a the first version (must not be {@code null})
     * @param b the second version (must not be {@code null})
     */
    default String min(String a, String b) {
        return le(a, b) ? a : b;
    }

    /**
     * {@return the greater (later) of the two versions}
     *
     * @param a the first version (must not be {@code null})
     * @param b the second version (must not be {@code null})
     */
    default String max(String a, String b) {
        return ge(a, b) ? a : b;
    }

    /**
     * Returns a predicate that tests if the version is equal to the base version.
     *
     * @param other the other version
     * @return {@code true} if the first version is equal to the second version, or {@code false} otherwise
     */
    default Predicate<String> whenEquals(String other) {
        return base -> equals(base, other);
    }

    /**
     * Returns a predicate that tests if the version is greater than or equal to the base version.
     *
     * @param other the other version
     * @return {@code true} if the first version is less than the second version, or {@code false} otherwise
     */
    default Predicate<String> whenGt(String other) {
        return base -> gt(base, other);
    }

    /**
     * Returns a predicate that tests if the version is greater than or equal to the base version.
     *
     * @param other the other version
     * @return a predicate that tests if the version is greater than or equal to the base version
     */
    default Predicate<String> whenGe(String other) {
        return base -> ge(base, other);
    }

    /**
     * Returns a predicate that tests if the version is less than or equal to the base version.
     *
     * @param other the other version
     * @return a predicate that tests if the version is less than or equal to the base version
     */
    default Predicate<String> whenLe(String other) {
        return base -> le(base, other);
    }

    /**
     * Returns a predicate that tests if the version is less than the base version.
     *
     * @param other the other version
     * @return a predicate that tests if the version is less than the base version
     */
    default Predicate<String> whenLt(String other) {
        return base -> lt(base, other);
    }

    /**
     * Parse a range specification and return it as a predicate.
     * This method behaves as a call to {@link #fromRangeString(String, int, int) fromRangeString(range, 0, range.length())}.
     *
     * @param range the range string to parse (must not be {@code null})
     * @return the parsed range (not {@code null})
     * @throws IllegalArgumentException if there is a syntax error in the range or the range cannot match any version
     */
    default Predicate<String> fromRangeString(String range) {
        return fromRangeString(range, 0, range.length());
    }

    /**
     * Parse a range specification and return it as a predicate.
     * Version ranges are governed by the following general syntax:
     * <code><pre>
range ::= range-spec ',' range
        | range-spec

range-spec ::= '[' version ']
             | min-version ',' max-version

min-version ::= '[' version
              | '(' version
              | '('

max-version ::= version ']'
              | version ')'
              | ')'
</pre></code>
     * This is aligned with the syntax used by Maven, however it can be applied to any
     * supported version scheme.
     * <p>
     * It is important to note that within a range specification, the {@code ,} separator
     * indicates a logical "and" or "intersection" operation, whereas the {@code ,} separator
     * found in between range specifications acts as a logical "or" or "union" operation.
     * <p>
     * Here are some examples of valid version range specifications:
     * <ul>
     * <li><code>1.0</code> Version 1.0 as a recommended version (like {@code whenEquals("1.0")})</li>
     * <li><code>[1.0]</code> Version 1.0 explicitly only (like {@code whenEquals("1.0")})</li>
     * <li><code>[1.0,2.0)</code> Versions 1.0 (included) to 2.0 (not included) (like {@code whenGe("1.0").and(whenLt("2.0"))})</li>
     * <li><code>[1.0,2.0]</code> Versions 1.0 to 2.0 (both included) (like {@code whenGe("1.0").and(whenLe("2.0"))})</li>
     * <li><code>[1.5,)</code> Versions 1.5 and higher (like {@code whenGe("1.5")})</li>
     * <li><code>(,1.0],[1.2,)</code> Versions up to 1.0 (included) and 1.2 or higher (like {@code whenLe("1.0").or(whenGe("1.2"))})</li>
     * </ul>
     *
     * @param range the range string to parse (must not be {@code null})
     * @param start the start of the range within the string (inclusive)
     * @param end the end of the range within the string (exclusive)
     * @return the parsed range (not {@code null})
     * @throws IllegalArgumentException if there is a syntax error in the range or the range cannot match any version
     * @throws IndexOutOfBoundsException if the values for {@code start} or {@code end} are not valid
     */
    default Predicate<String> fromRangeString(String range, int start, int end) {
        Objects.checkFromToIndex(start, end, range.length());
        return parseRange(range, start, end);
    }

    /**
     * Determine if two versions are equal according to this version scheme.
     *
     * @param v1 the first version (must not be {@code null})
     * @param v2 the second version (must not be {@code null})
     * @return {@code true} if the versions are equal, or {@code false} otherwise
     */
    default boolean equals(String v1, String v2) {
        return compare(v1, v2) == 0;
    }

    /**
     * Get the canonical representation of this version, according to the current version scheme.
     *
     * @param original the possibly non-canonical version (must not be {@code null})
     * @return the canonical representation of the version (not {@code null})
     */
    default String canonicalize(String original) {
        return appendCanonicalized(new StringBuilder(original.length()), original).toString();
    }

    /**
     * Append the canonical representation of this version to the given builder,
     * according to the current version scheme.
     *
     * @param target the string builder to append to (must not be {@code null})
     * @param original the possibly non-canonical version (must not be {@code null})
     * @return the string builder that was passed in
     */
    StringBuilder appendCanonicalized(StringBuilder target, String original);

    /**
     * Validate the syntax of the given version.
     *
     * @param version the version to validate (must not be {@code null})
     * @throws VersionSyntaxException if the syntax of the version is invalid according to this version scheme
     */
    default void validate(String version) throws VersionSyntaxException {
        final VersionIterator itr = iterate(version);
        while (itr.hasNext()) {
            itr.next();
        }
    }

    /**
     * Iterate the canonicalized components of the given version according to the rules of this versioning scheme.
     *
     * @param version the version to iterate (must not be {@code null})
     * @return the version iterator (not {@code null})
     */
    VersionIterator iterate(String version);

    // @formatter:off
    /**
     * A basic versioning scheme which is roughly compatible with semantic versioning, GNU's scheme, the OpenSSL scheme,
     * the numeric scheme used by Unix-style and ELF dynamic linkers, etc. The syntax of this scheme can be described
     * with this grammar:
     * <pre><code>

version ::= underscore-sequence
          | version "." underscore-sequence

underscore-sequence ::= dash-sequence
                      | underscore-sequence "_" dash-sequence

dash-sequence ::= plus-sequence
                | dash-sequence "-" plus-sequence

plus-sequence ::= parts
                | plus-sequence "+" parts

parts ::= alpha-parts
        | number-parts

alpha-parts ::= letter+
              | alpha-parts number-parts

number-parts ::= digit+
               | number-parts alpha-parts

digit ::= &lt;{@linkplain Character#isDigit(int) Unicode digits}&gt;

letter ::= &lt;{@linkplain Character#isLetter(int) Unicode letters}&gt;
    </code></pre>
     * <p>
     * Digit parts are sorted numerically (with leading zeros stripped). Alpha parts are sorted lexicographically and
     * case-sensitively. Trailing zero segments are trimmed. When comparing two sequences, the shorter sequence is
     * padded with virtual zero parts until the lengths match. When comparing parts, shorter parts come before longer
     * parts and are not zero-padded. Numeric parts sort after alphabetical parts.
     * <p>
     * This versioning scheme is generalized from <a href="semver.org">Semantic Versioning 2.0</a> and other similar
     * version schemes.
     */
    // @formatter:on
    VersionScheme BASIC = new BasicVersionScheme();

    /**
     * The <a href="https://maven.apache.org/">Apache Maven</a> versioning scheme.
     * <p>
     * This versioning scheme is a modified superset of semantic versioning which includes special treatment of certain
     * qualifier tokens and a few other quirks. For a more detailed (if somewhat incomplete) description of this
     * scheme, please refer to
     * <a href="https://maven.apache.org/pom.html#version-order-specification">the Maven documentation</a>.
     */
    VersionScheme MAVEN = new MavenVersionScheme();

    /**
     * The <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/module/package-summary.html">Java
     * platform module system</a> versioning scheme. <b>Note that the JPMS allows modules to be loaded with version numbers
     * that do not conform to this scheme's rules!</b>
     * <p>
     * This versioning scheme is based approximately on semantic versioning but with a few differences.
     */
    VersionScheme JPMS = new JpmsVersionScheme();

    private Predicate<String> parseRange(final String range, int start, final int end) {
        if (start == end) {
            return whenEquals("");
        }
        int cp = range.codePointAt(start);
        int cnt = Character.charCount(cp);
        switch (cp) {
            case '[': {
                return parseMinIncl(range, start + cnt, end);
            }
            case '(': {
                return parseMinExcl(range, start + cnt, end);
            }
            case ',': {
                return parseMore(whenEquals(""), range, start + cnt, end);
            }
            default: {
                return parseSingle(range, start + cnt, end);
            }
        }
    }

    private Predicate<String> parseSingle(String range, int start, int end) {
        int i = start;
        int cp, cnt;
        do {
            cp = range.codePointAt(i);
            cnt = Character.charCount(cp);
            switch (cp) {
                case ',': {
                    return parseMore(whenEquals(range.substring(start, i)), range, i + cnt, end);
                }
                case ']':
                case ')': {
                    throw Messages.msg.standaloneVersionCannotBeBound();
                }
            }
            i += cnt;
        } while (i < end);
        // just a single version
        return whenEquals(range.substring(start, end));
    }

    private Predicate<String> parseMinIncl(String range, int start, int end) {
        int i = start;
        int cp;
        do {
            cp = range.codePointAt(i);
            int cnt = Character.charCount(cp);
            switch (cp) {
                case ',': {
                    if (i == start) {
                        throw Messages.msg.inclusiveVersionCannotBeEmpty();
                    }
                    return parseRangeMax(whenGe(range.substring(start, i)), range, i + cnt, end);
                }
                case ']': {
                    return parseMore(whenEquals(range.substring(start, i)), range, i + cnt, end);
                }
                case ')': {
                    throw Messages.msg.singleVersionMustBeSurroundedByBrackets(range.substring(start, i + cnt));
                }
            }
            i += cnt;
        } while (i < end);
        // ended short, so treat it as open-ended
        return whenGe(range.substring(start, end));
    }

    private Predicate<String> parseMinExcl(String range, int start, int end) {
        int i = start;
        int cp;
        do {
            cp = range.codePointAt(i);
            int cnt = Character.charCount(cp);
            switch (cp) {
                case ',': {
                    if (i == start) {
                        // include all
                        return parseRangeMax(null, range, i + cnt, end);
                    } else {
                        return parseRangeMax(whenGt(range.substring(start, i)), range, i + cnt, end);
                    }
                }
                case ']':
                case ')': {
                    throw Messages.msg.singleVersionMustBeSurroundedByBrackets(range.substring(start, i + cnt));
                }
            }
            i += cnt;
        } while (i < end);
        // ended short, so treat it as open-ended
        return whenGt(range.substring(start, end));
    }

    private Predicate<String> parseRangeMax(Predicate<String> min, String range, int start, int end) {
        int i = start;
        int cp;
        do {
            cp = range.codePointAt(i);
            int cnt = Character.charCount(cp);
            switch (cp) {
                case ')': {
                    if (i == start) {
                        // empty upper range; only consider the minimum range
                        return parseMore(min, range, i + cnt, end);
                    }
                    // fall through
                }
                case ']': {
                    String high = range.substring(start, i);
                    if (min != null && ! min.test(high)) {
                        // low end must be higher than high end
                        throw Messages.msg.rangeDefiesVersionOrdering(range.substring(start, i + cnt));
                    }
                    Predicate<String> max = cp == ']' ? whenLe(high) : whenLt(high);
                    return parseMore(min == null ? max : min.and(max), range, i + cnt, end);
                }
                case ',': {
                    throw Messages.msg.rangeUnexpected(range.substring(start, i + cnt));
                }
            }
            i += cnt;
        } while (i < end);
        // ended short
        throw Messages.msg.unboundedRange(range.substring(start, end));
    }

    /**
     * Parse the end context (make sure there is no trailing garbage, combine subsequent predicates).
     *
     * @param predicate the predicate to return
     * @param range the range string
     * @param start the remaining start
     * @param end the end
     * @return the predicate
     */
    private Predicate<String> parseMore(Predicate<String> predicate, final String range, int start, int end) {
        if (start < end) {
            int cp = range.codePointAt(start);
            int cnt = Character.charCount(cp);
            if (cp == ',') {
                // composed version ranges
                Predicate<String> nextRange = parseRange(range, start + cnt, end);
                return predicate == null ? nextRange : predicate.or(nextRange);
            }
            throw Messages.msg.rangeUnexpected(range.substring(start, start + cnt));
        } else {
            return predicate;
        }
    }
}
