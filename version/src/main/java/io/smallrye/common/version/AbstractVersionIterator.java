package io.smallrye.common.version;

import static io.smallrye.common.version.Messages.msg;

import java.util.NoSuchElementException;

import io.smallrye.common.constraint.Assert;

/**
 * A version scheme iterator base class that provides useful utilities and mechanisms. The iterator does not support
 * version strings which are greater than 4095 characters in length.
 */
public abstract class AbstractVersionIterator implements VersionIterator {
    /**
     * The version string being iterated.
     */
    protected final String string;
    long cookie;

    /**
     * The current state of iteration.
     */
    protected enum TokenType {
        /**
         * The initial state.
         */
        INITIAL,
        /**
         * A part of the version string which is an alpha part.
         */
        PART_ALPHA,
        /**
         * A part of the version string which is a numeric part.
         */
        PART_NUMBER,
        /**
         * A separator character.
         */
        SEP_EMPTY,
        /**
         * A non-empty separator character.
         */
        SEP,
        /**
         * An invalid state.
         */
        INVALID;

        static TokenType[] values = values();
    }

    /**
     * Construct a new instance.
     *
     * @param string the version string (must not be {@code null})
     */
    protected AbstractVersionIterator(final String string) {
        this.string = Assert.checkNotNullParam("string", string);
    }

    static long makeCookie(final long start, final long end, final TokenType state, final long extraBits) {
        return start | (end << 12) | ((long) state.ordinal() << 24 | extraBits << 32);
    }

    static int cookieToStartIndex(long cookie) {
        return (int) (cookie & 0x3ff);
    }

    static int cookieToEndIndex(long cookie) {
        return (int) (cookie >> 12 & 0x3ff);
    }

    static TokenType cookieType(long cookie) {
        return TokenType.values[(int) (cookie >> 24 & 0xf)];
    }

    static int cookieToExtraBits(long cookie) {
        return (int) (cookie >> 32);
    }

    /**
     * Change the current token type.
     *
     * @param newType the new type (must not be {@code null})
     */
    protected void changeType(TokenType newType) {
        long cookie = this.cookie;
        this.cookie = makeCookie(cookieToStartIndex(cookie), cookieToEndIndex(cookie),
                Assert.checkNotNullParam("newState", newType), cookieToExtraBits(cookie));
    }

    /**
     * Insert an empty part into the current position.
     */
    protected void insertEmptyAlpha() {
        long cookie = this.cookie;
        setCurrentToken(TokenType.PART_ALPHA, cookieToStartIndex(cookie), cookieToStartIndex(cookie),
                cookieToExtraBits(cookie));
    }

    /**
     * Insert an empty separator into the current position.
     */
    protected void insertEmptySeparator() {
        long cookie = this.cookie;
        setCurrentToken(TokenType.SEP_EMPTY, cookieToStartIndex(cookie), cookieToStartIndex(cookie), cookieToExtraBits(cookie));
    }

    /**
     * Insert an empty (zero) number into the current position.
     */
    protected void insertEmptyNumber() {
        long cookie = this.cookie;
        setCurrentToken(TokenType.PART_NUMBER, cookieToStartIndex(cookie), cookieToStartIndex(cookie),
                cookieToExtraBits(cookie));
    }

    /**
     * Set the current token.
     *
     * @param tokenType the token type (must not be {@code null})
     * @param startIndex the start index
     * @param endIndex the end index
     * @param extraBits extra implementation-specific state bits
     */
    protected void setCurrentToken(TokenType tokenType, int startIndex, int endIndex, int extraBits) {
        this.cookie = makeCookie(startIndex, endIndex, Assert.checkNotNullParam("tokenType", tokenType), extraBits);
    }

    /**
     * Mark the current iteration position and state.
     *
     * @return the current iteration cookie
     */
    protected long mark() {
        return cookie;
    }

    /**
     * Restore the iteration position to a previous {@link #mark()} state.
     *
     * @param mark the mark to restore to
     */
    protected void reset(long mark) {
        cookie = mark;
    }

    /**
     * Get the current iteration token type.
     *
     * @return the iteration state
     */
    protected TokenType currentType() {
        return cookieType(cookie);
    }

    /**
     * Get the start index of the current token.
     *
     * @return the index into the version string
     */
    protected int getStartIndex() {
        return cookieToStartIndex(cookie);
    }

    /**
     * Get the end index (exclusive) of the current token. The length
     *
     * @return the index into the version string
     */
    protected int getEndIndex() {
        return cookieToEndIndex(cookie);
    }

    /**
     * Get the extra implementation-specific state bits.
     *
     * @return the extra bits
     */
    protected int getExtraBits() {
        return cookieToExtraBits(cookie);
    }

    public int compareAlphaPart(final String str, final int offs, final int len, final boolean ignoreCase)
            throws IllegalStateException, StringIndexOutOfBoundsException {
        long cookie = this.cookie;
        int myOffs = cookieToStartIndex(cookie);
        int myLen = cookieToEndIndex(cookie) - myOffs;
        int cp;
        int ocp;
        for (int i = 0, j = 0; i < myLen && j < len; i += Character.charCount(cp), j += Character.charCount(ocp)) {
            cp = string.codePointAt(i + myOffs);
            ocp = str.codePointAt(j + offs);
            int res;
            if (ignoreCase) {
                res = Integer.compare(Character.toLowerCase(cp), Character.toLowerCase(ocp));
                if (res == 0) {
                    res = Integer.compare(Character.toUpperCase(cp), Character.toUpperCase(ocp));
                }
            } else {
                res = Integer.compare(cp, ocp);
            }
            if (res != 0) {
                return res;
            }
        }
        // identical prefix; fall back to length comparison
        return Integer.compare(len, myLen);
    }

    public int compareAlphaPart(final VersionIterator other, final boolean ignoreCase) throws IllegalStateException {
        long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_ALPHA) {
            throw msg.expectedAlpha();
        }
        int start = cookieToStartIndex(cookie);
        return -Integer.signum(other.compareAlphaPart(string, start, cookieToEndIndex(cookie) - start, ignoreCase));
    }

    public int length() {
        long cookie = this.cookie;
        return cookieToEndIndex(cookie) - cookieToStartIndex(cookie);
    }

    public String getNumberPartAsString() throws IllegalStateException {
        long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_NUMBER) {
            throw msg.expectedNumber();
        }
        return string.substring(cookieToStartIndex(cookie), cookieToEndIndex(cookie));
    }

    public StringBuilder appendNumberPartTo(final StringBuilder target) throws IllegalStateException {
        final long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_NUMBER) {
            throw msg.expectedNumber();
        }
        final int start = cookieToStartIndex(cookie);
        final int end = cookieToEndIndex(cookie);
        int cp;
        int v;
        boolean nz = false;
        for (int i = start; i < end; i += Character.charCount(cp)) {
            cp = string.codePointAt(i);
            v = numericCodePointValue(cp);
            assert v >= 0;
            if (v != 0 || nz) {
                target.appendCodePoint('0' + v);
                nz = true;
            }
        }
        return target;
    }

    public int getNumberPartAsInt() {
        return (int) getNumberPartAsLong();
    }

    public long getNumberPartAsLong() {
        final long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_NUMBER) {
            throw msg.expectedNumber();
        }
        long total = 0;
        final int start = cookieToStartIndex(cookie);
        final int end = cookieToEndIndex(cookie);
        int cp;
        int v;
        for (int i = start; i < end; i += Character.charCount(cp)) {
            cp = string.codePointAt(i);
            v = numericCodePointValue(cp);
            assert v >= 0;
            total = total * 10 + v;
        }
        return total;
    }

    public int compareNumberPart(final int value) throws IllegalStateException {
        final long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_NUMBER) {
            throw msg.expectedNumber();
        }
        long total = 0;
        final int start = cookieToStartIndex(cookie);
        final int end = cookieToEndIndex(cookie);
        final long ul = Integer.toUnsignedLong(value);
        int cp;
        int v;
        for (int i = start; i < end; i += Character.charCount(cp)) {
            cp = string.codePointAt(i);
            v = numericCodePointValue(cp);
            assert v >= 0;
            total = total * 10 + v;
            if (total > ul) {
                return 1;
            }
        }
        return total == value ? 0 : -1;
    }

    public int compareNumberPart(final long value) throws IllegalStateException {
        final long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_NUMBER) {
            throw msg.expectedNumber();
        }
        long total = 0;
        final int start = cookieToStartIndex(cookie);
        final int end = cookieToEndIndex(cookie);
        int cp;
        int v;
        try {
            for (int i = start; i < end; i += Character.charCount(cp)) {
                cp = string.codePointAt(i);
                v = numericCodePointValue(cp);
                assert v >= 0;
                total = Math.addExact(Math.multiplyExact(total, 10L), v);
                if (Long.compareUnsigned(total, value) > 0) {
                    return 1;
                }
            }
        } catch (ArithmeticException ignored) {
            // current segment is too large to represent in an unsigned long
            return 1;
        }
        return total == value ? 0 : -1;
    }

    public int compareNumberPart(final String value, int offs, int len) throws IllegalStateException {
        final long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_NUMBER) {
            throw msg.expectedNumber();
        }
        int start = cookieToStartIndex(cookie);
        int myLen = cookieToEndIndex(cookie) - start;
        int cp, ocp, v, ov;
        int res = 0;
        // lengths (in code points)
        int j = 0;
        int i = 0;
        // skip leading zeros in both strings
        while (i < myLen) {
            cp = string.codePointAt(i + start);
            v = numericCodePointValue(cp);
            if (v != 0) {
                break;
            }
            int cnt = Character.charCount(cp);
            start += cnt;
            myLen -= cnt;
        }
        while (j < len) {
            ocp = value.codePointAt(j + offs);
            ov = numericCodePointValue(ocp);
            if (ov != 0) {
                break;
            }
            int cnt = Character.charCount(ocp);
            offs += cnt;
            len -= cnt;
        }
        // now compare the possible most-significant result
        // - we can't compare lengths first, because the length in code points might differ
        // - we could compare the code point length first but then we'd have to iterate both strings twice
        while (i < myLen && j < len) {
            cp = string.codePointAt(i + start);
            v = numericCodePointValue(cp);
            if (v == -1) {
                throw msg.invalidCodePoint(new String(Character.toChars(cp)), start + i, string);
            }
            ocp = value.codePointAt(j + offs);
            ov = numericCodePointValue(ocp);
            if (ov == -1) {
                throw msg.nonNumeric(new String(Character.toChars(cp)), offs + j, value);
            }
            if (res == 0) {
                // find the most significant difference
                res = Integer.compare(v, ov);
            }
            i += Character.charCount(cp);
            j += Character.charCount(ocp);
        }
        // longer numbers are bigger, otherwise fall back to most significant result
        return i < myLen ? 1 : j < len ? -1 : res;
    }

    public int compareNumberPart(final VersionIterator other) {
        final long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_NUMBER) {
            throw msg.expectedNumber();
        }
        final int start = cookieToStartIndex(cookie);
        final int len = cookieToEndIndex(cookie) - start;
        return -Integer.signum(other.compareNumberPart(string, start, len));
    }

    public boolean isPart() {
        final TokenType tokenType = cookieType(cookie);
        return tokenType == TokenType.PART_ALPHA || tokenType == TokenType.PART_NUMBER;
    }

    public boolean isSeparator() {
        final TokenType tokenType = cookieType(cookie);
        return tokenType == TokenType.SEP || tokenType == TokenType.SEP_EMPTY;
    }

    public boolean isEmptySeparator() {
        return cookieType(cookie) == TokenType.SEP_EMPTY;
    }

    public boolean isNonEmptySeparator() {
        return cookieType(cookie) == TokenType.SEP;
    }

    public int getSeparatorCodePoint() {
        final long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.SEP) {
            throw msg.expectedSep();
        }
        return string.codePointAt(cookieToStartIndex(cookie));
    }

    public boolean isAlphaPart() {
        return cookieType(cookie) == TokenType.PART_ALPHA;
    }

    public boolean isNumberPart() {
        return cookieType(cookie) == TokenType.PART_NUMBER;
    }

    public String getAlphaPart() throws IllegalStateException {
        long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_ALPHA) {
            throw msg.expectedAlpha();
        }
        return string.substring(cookieToStartIndex(cookie), cookieToEndIndex(cookie));
    }

    public StringBuilder appendAlphaPartTo(final StringBuilder target) throws IllegalStateException {
        long cookie = this.cookie;
        if (cookieType(cookie) != TokenType.PART_ALPHA) {
            throw msg.expectedAlpha();
        }
        return target.append(string, cookieToStartIndex(cookie), cookieToEndIndex(cookie));
    }

    public StringBuilder appendPartTo(final StringBuilder target) {
        Assert.checkNotNullParam("target", target);
        final TokenType tokenType = cookieType(cookie);
        if (tokenType == TokenType.PART_NUMBER) {
            return appendNumberPartTo(target);
        } else if (tokenType == TokenType.PART_ALPHA) {
            return appendAlphaPartTo(target);
        } else if (tokenType == TokenType.SEP) {
            return target.appendCodePoint(getSeparatorCodePoint());
        } else {
            // nothing to append
            return target;
        }
    }

    /**
     * A basic implementation of {@link VersionIterator#hasNext()} which returns {@code true} if there are more
     * characters after the current token. Subclasses should generally override this basic behavior but may
     * delegate to this implementation for convenience.
     *
     * @return {@code true} if there are more characters after the current token, {@code false} otherwise
     */
    public boolean hasNext() {
        return thisHasNext();
    }

    private boolean thisHasNext() {
        return cookieToEndIndex(cookie) < string.length();
    }

    /**
     * Determine whether the given code point is a valid separator according to the current scheme. This basic
     * implementation returns {@code true} for {@code '.'}, {@code '-'}, {@code '+'}, and {@code '_'}.
     * Subclasses may override this method to recognize a different set of separators.
     *
     * @param cp the code point to test
     * @return {@code true} if the code point is a separator, or {@code false} otherwise
     */
    protected boolean isSeparatorCodePoint(int cp) {
        return cp == '.' || cp == '-' || cp == '+' || cp == '_';
    }

    /**
     * Determine whether the given code point is a valid numeric code point according to the current scheme, and if
     * so, what the digit's numerical value is. This basic implementation delegates to {@link Character#digit(int, int)}
     * with a {@code radix} value of 10.
     * Subclasses may override this method to recognize a different set of numeric code points.
     *
     * @param cp the code point to test
     * @return the numerical value of the given code point, or {@code -1} if the code point is not a valid numerical digit
     *         for this version scheme
     */
    protected int numericCodePointValue(int cp) {
        return Character.digit(cp, 10);
    }

    /**
     * Determine whether the given code point is a valid alpha code point according to the current scheme. This
     * basic implementation returns {@code true} for any character whose general category is in the set recognized by
     * {@link Character#isLetter(int)}.
     * Subclasses may override this method to recognize a different set of alpha code points.
     *
     * @param cp the code point to test
     * @return {@code true} if the code point is a valid letter, or {@code false} otherwise
     */
    protected boolean isAlphaCodePoint(int cp) {
        return Character.isLetter(cp);
    }

    /**
     * A basic implementation of {@link VersionIterator#next()} which uses the methods on this class to establish the
     * next iteration token. This implementation consumes leading zeros from numeric parts but otherwise does not
     * perform any transformations.
     *
     * @throws NoSuchElementException if there are no tokens left to iterate
     * @throws VersionSyntaxException if the version string is unparseable due to syntax error
     */
    public void next() throws NoSuchElementException, VersionSyntaxException {
        if (!thisHasNext()) {
            throw msg.iterationPastEnd();
        }
        TokenType token;
        // get old end value
        final long cookie = this.cookie;
        int end = cookieToEndIndex(cookie);
        int bits = cookieToExtraBits(cookie);
        final String string = this.string;
        final int length = string.length();
        // hasNext() should have been called first on this cookie value
        assert end < length;
        int start = end;
        int cp = string.codePointAt(start);
        // examine the previous token
        token = cookieType(cookie);
        if ((token == TokenType.PART_NUMBER || token == TokenType.PART_ALPHA)
                && (isAlphaCodePoint(cp) || numericCodePointValue(cp) != -1)) {
            token = TokenType.SEP_EMPTY;
            end = start;
        } else if (isAlphaCodePoint(cp)) {
            token = TokenType.PART_ALPHA;
            end = length;
            // find end
            int i = start + Character.charCount(cp);
            while (i < length) {
                cp = string.codePointAt(i);
                if (!isAlphaCodePoint(cp)) {
                    end = i;
                    break;
                }
                i += Character.charCount(cp);
            }
        } else if (numericCodePointValue(cp) != -1) {
            token = TokenType.PART_NUMBER;
            end = length;
            // find end if it's before length
            int d;
            int i = 0;
            while ((start + i) < length) {
                cp = string.codePointAt(i + start);
                d = numericCodePointValue(cp);
                if (d == -1) {
                    end = start + i;
                    break;
                } else if (d == 0 && i == 0) {
                    // skip leading zero
                    start += Character.charCount(cp);
                } else {
                    i += Character.charCount(cp);
                }
            }
        } else if (isSeparatorCodePoint(cp)) {
            token = TokenType.SEP;
            end = start + Character.charCount(cp);
        } else {
            throw msg.invalidCodePoint(new String(Character.toChars(cp)), start, string);
        }
        if (end >= 1 << 12) {
            throw msg.tooLong();
        }
        assert end >= start;
        this.cookie = makeCookie(start, end, token, bits);
    }
}
