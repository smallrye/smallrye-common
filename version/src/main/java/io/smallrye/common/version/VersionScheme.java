package io.smallrye.common.version;

import java.util.Comparator;

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
}
