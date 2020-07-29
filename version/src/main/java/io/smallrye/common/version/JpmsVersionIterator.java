package io.smallrye.common.version;

import static io.smallrye.common.version.Messages.msg;

import java.util.NoSuchElementException;

final class JpmsVersionIterator extends AbstractVersionIterator {
    static final int GOT_PRE_RELEASE = 1;
    static final int GOT_BUILD = 2;

    JpmsVersionIterator(final String string) {
        super(string);
    }

    public boolean hasNext() {
        if (!super.hasNext()) {
            return false;
        }
        final long mark = mark();
        try {
            super.next();
            TokenType t = currentType();
            if (t == TokenType.PART_ALPHA ||
                    t == TokenType.PART_NUMBER && !numberPartEquals(0) ||
                    cookieToExtraBits(mark) == 0 && t == TokenType.SEP && super.getSeparatorCodePoint() != '.') {
                return true;
            }
            // otherwise it depends on the next segment
            return hasNext();
        } finally {
            reset(mark);
        }
    }

    void skipTrailer(long mark) {
        // skip any .0 in the current section.
        assert hasNext();
        next();
        if (isNumberPart() && numberPartEquals(0)) {
            // could be more zeros
            next();
            if (isSeparator()) {
                int scp = getSeparatorCodePoint();
                if (scp == '-') {
                    // stop here always (pre-release version)
                    return;
                } else if (scp == '+') {
                    // try to skip the last part completely
                    skipTrailer(mark());
                    return;
                }
            }
            skipTrailer(mark);
        } else {
            // can't skip
            reset(mark);
        }
        return;
    }

    public void next() throws NoSuchElementException, VersionSyntaxException {
        // all . separators are replaced with SEP_EMPTY; all SEP promote to next section
        TokenType p = currentType();
        super.next();
        TokenType t = currentType();
        if ((p == TokenType.SEP || p == TokenType.SEP_EMPTY) && (t == TokenType.SEP || t == TokenType.SEP_EMPTY)) {
            int idx = cookieToStartIndex(cookie);
            throw msg.invalidCodePoint(new String(Character.toChars(string.codePointAt(idx))), idx, string);
        }
        if (p == TokenType.INITIAL && !isNumberPart()) {
            throw msg.expectedNumber();
        }
        int got = getExtraBits();
        if (isSeparator()) {
            if (super.isEmptySeparator()) {
                skipTrailer(mark());
            } else {
                int scp = super.getSeparatorCodePoint();
                if (scp == '.' || got >= GOT_PRE_RELEASE && scp == '-' || got >= GOT_BUILD && scp == '+') {
                    setCurrentToken(TokenType.SEP_EMPTY, getEndIndex(), getEndIndex(), got);
                    skipTrailer(mark());
                } else {
                    // change sections
                    setCurrentToken(currentType(), getStartIndex(), getEndIndex(), got + 1);
                    if (!super.hasNext()) {
                        if (got == 0) {
                            throw msg.emptyPreRelease();
                        } else if (got == GOT_PRE_RELEASE) {
                            throw msg.emptyBuild();
                        }
                    }
                }
            }
        }
    }

    protected boolean isSeparatorCodePoint(final int cp) {
        int got = getExtraBits();
        return cp == '.' || got < GOT_PRE_RELEASE && cp == '-' || got < GOT_BUILD && cp == '+';
    }

    protected int numericCodePointValue(final int cp) {
        int v = cp - '0';
        return v < 0 || v > 9 ? -1 : v;
    }

    protected boolean isAlphaCodePoint(final int cp) {
        return !isSeparatorCodePoint(cp) && numericCodePointValue(cp) == -1;
    }

    public boolean isEmptySeparator() {
        return false;
    }

    public int getSeparatorCodePoint() {
        if (super.isEmptySeparator()) {
            return '.';
        }
        int got = getExtraBits();
        if (got == GOT_PRE_RELEASE) {
            return '-';
        } else {
            assert got == GOT_BUILD;
            return '+';
        }
    }

    public boolean isNonEmptySeparator() {
        return super.isSeparator();
    }
}
