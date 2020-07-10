package io.smallrye.common.version;

import java.math.BigInteger;
import java.util.NoSuchElementException;

/**
 * An iterator over the parts of a version. The iterator starts off in the "initial" position. Use the
 * {@link #hasNext()} and {@link #next()} methods to move the cursor to each segment of the version string in turn.
 */
public interface VersionIterator {
    /**
     * Determine whether a subsequent token exists in this version.
     *
     * @return {@code true} if more tokens remain, {@code false} otherwise
     */
    boolean hasNext();

    /**
     * Move to the next token.
     *
     * @throws NoSuchElementException if there are no more tokens to iterate
     */
    void next() throws NoSuchElementException, VersionSyntaxException;

    /**
     * Get the length of the current token. If there is no current token, zero is returned.
     *
     * @return the length of the current token
     */
    int length();

    /**
     * Determine if the current token is some kind of separator (a character or a zero-length alphabetical-to-numeric
     * or numeric-to-alphabetical transition).
     *
     * @return {@code true} if the token is a separator, {@code false} otherwise
     */
    default boolean isSeparator() {
        return isEmptySeparator() || isNonEmptySeparator();
    }

    /**
     * Determine if the current token is some kind of part (alphabetical or numeric).
     *
     * @return {@code true} if the token is a part, {@code false} otherwise
     */
    default boolean isPart() {
        return isAlphaPart() || isNumberPart();
    }

    /**
     * Determine if the current token is an empty (or zero-length alphabetical-to-numeric
     * or numeric-to-alphabetical) separator. Note that some version schemes do not have
     * empty separators.
     *
     * @return {@code true} if the token is an empty separator, {@code false} otherwise
     */
    boolean isEmptySeparator();

    /**
     * Determine if the current token is a non-empty separator.
     *
     * @return {@code true} if the token is a non-empty separator, {@code false} otherwise
     */
    boolean isNonEmptySeparator();

    /**
     * Get the code point of the current separator. If the iterator is not positioned on a non-empty separator
     * (i.e. {@link #isNonEmptySeparator()} returns {@code false}), then an exception is thrown.
     *
     * @return the code point of the current separator
     * @throws IllegalStateException if the current token is not a non-empty separator
     */
    int getSeparatorCodePoint();

    /**
     * Determine if the current token is an alphabetical part.
     *
     * @return {@code true} if the token is an alphabetical part, {@code false} otherwise
     */
    boolean isAlphaPart();

    /**
     * Determine if the current token is a numeric part.
     *
     * @return {@code true} if the token is a numeric part, {@code false} otherwise
     */
    boolean isNumberPart();

    /**
     * Get the current alphabetical part. If the iterator is not positioned on an alphabetical part (i.e.
     * {@link #isAlphaPart()} returns {@code false}), then an exception is thrown.
     *
     * @return the current alphabetical part
     * @throws IllegalStateException if the current token is not an alphabetical part
     */
    String getAlphaPart() throws IllegalStateException;

    /**
     * Append the current alphabetical part to the given string builder. If the iterator is not positioned on an
     * alphabetical part (i.e. {@link #isAlphaPart()} returns {@code false}), then an exception is thrown.
     *
     * @return the current alphabetical part
     * @throws IllegalStateException if the current token is not an alphabetical part
     */
    StringBuilder appendAlphaPartTo(StringBuilder target) throws IllegalStateException;

    /**
     * Determine whether the current alphabetical part is equal to the given test string. If the iterator is not
     * positioned on an alphabetical part (i.e. {@link #isAlphaPart()} returns {@code false}), then an exception is
     * thrown.
     *
     * @param str the string to compare (must not be {@code null})
     * @param ignoreCase {@code true} to perform a case-insensitive comparison, or {@code false} to perform a
     *        case-sensitive comparison
     * @return {@code true} if the segment matches the given string; {@code false} otherwise
     * @throws IllegalStateException if the current token is not an alphabetical part
     */
    default boolean alphaPartEquals(String str, boolean ignoreCase) throws IllegalStateException {
        return alphaPartEquals(str, 0, str.length(), ignoreCase);
    }

    /**
     * Determine whether the current alphabetical part is equal to the given test string. If the iterator is not
     * positioned on an alphabetical part (i.e. {@link #isAlphaPart()} returns {@code false}), then an exception is
     * thrown.
     *
     * @param str the string to compare (must not be {@code null})
     * @param offs the offset into the string to compare
     * @param len the length to compare
     * @param ignoreCase {@code true} to perform a case-insensitive comparison, or {@code false} to perform a
     *        case-sensitive comparison
     * @return {@code true} if the segment matches the given string; {@code false} otherwise
     * @throws IllegalStateException if the current token is not an alphabetical part
     * @throws StringIndexOutOfBoundsException if the given offset or length fall outside of the string
     */
    default boolean alphaPartEquals(String str, int offs, int len, boolean ignoreCase) throws IllegalStateException,
            StringIndexOutOfBoundsException {
        return compareAlphaPart(str, offs, len, ignoreCase) == 0;
    }

    /**
     * Compare two alphabetical parts lexicographically. This iterator must be positioned at an alphabetical part or an
     * exception is thrown.
     *
     * @param str the string to compare against (must not be {@code null})
     * @param ignoreCase {@code true} to perform a case-insensitive comparison, or {@code false} to perform a
     *        case-sensitive comparison
     * @return {@code -1}, {@code 0}, or {@code 1} if this segment is less than, equal to, or greater than the given
     *         string
     * @throws IllegalStateException if this iterator is not positioned on an alphabetical part
     */
    default int compareAlphaPart(String str, boolean ignoreCase) throws IllegalStateException {
        return compareAlphaPart(str, 0, str.length(), ignoreCase);
    }

    /**
     * Compare two alphabetical parts lexicographically. This iterator must be positioned at an
     * alphabetical part or an exception is thrown.
     *
     * @param str the string to compare against (must not be {@code null})
     * @param offs the offset into the string to compare
     * @param len the length to compare
     * @param ignoreCase {@code true} to perform a case-insensitive comparison, or {@code false} to perform a
     *        case-sensitive comparison
     * @return {@code -1}, {@code 0}, or {@code 1} if this segment is less than, equal to, or greater than the given
     *         string
     * @throws IllegalStateException if this iterator is not positioned on an alphabetical part
     * @throws StringIndexOutOfBoundsException if the given offset or length fall outside of the string
     */
    int compareAlphaPart(String str, int offs, int len, boolean ignoreCase) throws IllegalStateException,
            StringIndexOutOfBoundsException;

    /**
     * Compare two alphabetical parts lexicographically. Both this and the other iterator must be positioned at
     * alphabetical parts or an exception is thrown.
     *
     * @param other the other iterator (must not be {@code null})
     * @return {@code -1}, {@code 0}, or {@code 1} if this segment is less than, equal to, or greater than the other
     *         iterator's part
     * @throws IllegalStateException if this or the other iterator are not positioned on an alphabetical part
     */
    int compareAlphaPart(VersionIterator other, boolean ignoreCase) throws IllegalStateException;

    /**
     * Get the current numeric part, as a {@code String}. If the iterator is not positioned on a numeric part (i.e.
     * {@link #isNumberPart()} returns {@code false}), then an exception is thrown. Any redundant leading zeros are
     * removed.
     *
     * @return the current numeric part as a {@code String}
     * @throws IllegalStateException if the current token is not a numeric part
     */
    default String getNumberPartAsString() throws IllegalStateException {
        return appendNumberPartTo(new StringBuilder()).toString();
    }

    /**
     * Get the current numeric part, appending it to the given builder. If the iterator is not positioned on a numeric
     * part (i.e. {@link #isNumberPart()} returns {@code false}), then an exception is thrown. Any redundant
     * leading zeros are removed.
     *
     * @return the current numeric part as a {@code String}
     * @throws IllegalStateException if the current token is not a numeric part
     */
    StringBuilder appendNumberPartTo(StringBuilder target) throws IllegalStateException;

    /**
     * Get the current numeric part, as a {@code long}. If the iterator is not positioned on a numeric
     * part (i.e. {@link #isNumberPart()} returns {@code false}), then an exception is thrown. If the value
     * overflows the maximum value for a {@code long}, then only the low-order 64 bits of the version number
     * value are returned.
     *
     * @return the current numeric part as a {@code long}
     * @throws IllegalStateException if the current token is not a numeric part
     */
    long getNumberPartAsLong() throws IllegalStateException;

    /**
     * Get the current numeric part, as an {@code int}. If the iterator is not positioned on a numeric
     * part (i.e. {@link #isNumberPart()} returns {@code false}), then an exception is thrown. If the value
     * overflows the maximum value for an {@code int}, then only the low-order 32 bits of the version number
     * value are returned.
     *
     * @return the current numeric part as an {@code int}
     * @throws IllegalStateException if the current token is not a numeric part
     */
    int getNumberPartAsInt() throws IllegalStateException;

    /**
     * Get the current numeric part, as a {@code BigInteger}. If the iterator is not positioned on a numeric
     * part (i.e. {@link #isNumberPart()} returns {@code false}), then an exception is thrown.
     *
     * @return the current numeric part as a {@code BigInteger}
     * @throws IllegalStateException if the current token is not a numeric part
     */
    default BigInteger getNumberPartAsBigInteger() throws IllegalStateException {
        return new BigInteger(getNumberPartAsString());
    }

    /**
     * Determine whether the current numeric part equals the given value (which is treated as unsigned).
     *
     * @param value the unsigned value
     * @return {@code true} if the values are equal, or {@code false} otherwise
     * @throws IllegalStateException if the current token is not a numeric part
     */
    default boolean numberPartEquals(int value) throws IllegalStateException {
        return compareNumberPart(value) == 0;
    }

    /**
     * Determine whether the current numeric part equals the given value (which is treated as unsigned).
     *
     * @param value the unsigned value
     * @return {@code true} if the values are equal, or {@code false} otherwise
     * @throws IllegalStateException if the current token is not a numeric part
     */
    default boolean numberPartEquals(long value) throws IllegalStateException {
        return compareNumberPart(value) == 0;
    }

    /**
     * Compare two numerical parts (using an unsigned comparison). This iterator must be positioned at a
     * numerical part or an exception is thrown.
     *
     * @param value the number to compare against
     * @return {@code -1}, {@code 0}, or {@code 1} if this segment is less than, equal to, or greater than the given
     *         value
     * @throws IllegalStateException if this iterator is not positioned on a numeric part
     */
    int compareNumberPart(int value) throws IllegalStateException;

    /**
     * Compare two numerical parts (using an unsigned comparison). This iterator must be positioned at a
     * numerical part or an exception is thrown.
     *
     * @param value the number to compare against
     * @return {@code -1}, {@code 0}, or {@code 1} if this segment is less than, equal to, or greater than the given
     *         value
     * @throws IllegalStateException if this iterator is not positioned on a numeric part
     */
    int compareNumberPart(long value) throws IllegalStateException;

    /**
     * Compare two numerical parts (using an unsigned comparison). This iterator must be positioned at a numerical part
     * or an exception is thrown. The given string must be numeric according to the rules of this iterator or an
     * exception is thrown.
     *
     * @param value the number to compare against
     * @return {@code -1}, {@code 0}, or {@code 1} if this segment is less than, equal to, or greater than the given
     *         value
     * @throws IllegalStateException if this iterator is not positioned on a numeric part
     * @throws IllegalArgumentException if the given string is not numeric according to the rules of this iterator
     */
    default int compareNumberPart(String value) throws IllegalStateException {
        return compareNumberPart(value, 0, value.length());
    }

    /**
     * Compare two numerical parts (using an unsigned comparison). This iterator must be positioned at a
     * numerical part or an exception is thrown. The given string must be numeric according to
     * the rules of this iterator or an exception is thrown.
     *
     * @param value the number to compare against
     * @param offs the offset into the string to compare
     * @param len the length to compare
     * @return {@code -1}, {@code 0}, or {@code 1} if this segment is less than, equal to, or greater than the given
     *         value
     * @throws IllegalStateException if this iterator is not positioned on a numeric part
     * @throws IllegalArgumentException if the given string is not numeric according to the rules of this iterator
     */
    int compareNumberPart(String value, int offs, int len) throws IllegalStateException;

    /**
     * Compare two numerical parts (using an unsigned comparison). Both iterators must be positioned at a
     * numerical part or an exception is thrown.
     *
     * @param other the other iterator (must not be {@code null})
     * @return {@code -1}, {@code 0}, or {@code 1} if this segment is less than, equal to, or greater than the other
     *         iterator's part
     * @throws IllegalStateException if this or the other iterator are not positioned on an numerical part
     */
    int compareNumberPart(VersionIterator other);

    /**
     * Append this version part to the given string builder. This is used to produce a canonical representation
     * of an input string.
     *
     * @param b the string builder (must not be {@code null})
     * @return the same string builder (not {@code null})
     */
    StringBuilder appendPartTo(StringBuilder b);
}
