package io.smallrye.common.version;

import java.util.NoSuchElementException;

final class MavenVersionIterator extends AbstractVersionIterator {
    MavenVersionIterator(final String string) {
        super(string);
    }

    public boolean isEmptySeparator() {
        return false;
    }

    public boolean isNonEmptySeparator() {
        return super.isEmptySeparator() || super.isNonEmptySeparator();
    }

    public boolean isNumberPart() {
        return super.isNumberPart() || isReleaseString();
    }

    public boolean isAlphaPart() {
        return super.isAlphaPart() && !isReleaseString();
    }

    public int getSeparatorCodePoint() {
        return super.isEmptySeparator() ? '-' : super.getSeparatorCodePoint();
    }

    public StringBuilder appendPartTo(final StringBuilder target) {
        if (super.isEmptySeparator()) {
            return target.append('-');
        } else {
            return super.appendPartTo(target);
        }
    }

    public StringBuilder appendNumberPartTo(final StringBuilder target) throws IllegalStateException {
        if (isReleaseString()) {
            return target.append('0');
        } else {
            return super.appendNumberPartTo(target);
        }
    }

    public StringBuilder appendAlphaPartTo(final StringBuilder b) throws IllegalStateException {
        int m = getMilestoneMagnitude();
        if (m != -1) {
            return b.append(MILESTONES[m]);
        } else {
            // lowercased
            long cookie = this.cookie;
            final int start = cookieToStartIndex(cookie);
            final int length = cookieToEndIndex(cookie) - start;
            int cp;
            for (int i = 0; i < length; i += Character.charCount(cp)) {
                cp = string.charAt(start + i);
                b.appendCodePoint(Character.toLowerCase(cp));
            }
            return b;
        }
    }

    public boolean hasNext() {
        if (!super.hasNext()) {
            return false;
        }
        final long mark = mark();
        try {
            super.next();
            TokenType t = currentType();
            if (t == TokenType.PART_ALPHA && !isReleaseString()) {
                return true;
            }
            if (t == TokenType.PART_NUMBER && !super.numberPartEquals(0)) {
                return true;
            }
            // otherwise it depends on the next segment
            return hasNext();
        } finally {
            reset(mark);
        }
    }

    public void next() throws NoSuchElementException, VersionSyntaxException {
        super.next();
        // skip trailing 0. segments until next - or EOS
        if (isDotSeparator()) {
            final long mark = mark();
            try {
                skipTrailer(mark);
            } catch (Throwable t) {
                reset(mark);
                throw t;
            }
        }
        return;
    }

    ////

    protected boolean isSeparatorCodePoint(final int cp) {
        return cp == '.' || cp == '-' || cp == '_';
    }

    protected boolean isAlphaCodePoint(final int cp) {
        return !isSeparatorCodePoint(cp) && numericCodePointValue(cp) == -1;
    }

    ////

    boolean nextSeparatorIsEmpty() {
        if (!hasNext()) {
            return false;
        }
        final long mark = mark();
        next();
        try {
            return super.isEmptySeparator();
        } finally {
            reset(mark);
        }
    }

    boolean isReleaseString() {
        return alphaPartEquals("", true) || alphaPartEquals("ga", true) || alphaPartEquals("final", true)
                || alphaPartEquals("release", true);
    }

    boolean isZeroSegment() {
        return super.isNumberPart() && super.numberPartEquals(0) || super.isAlphaPart() && isReleaseString();
    }

    boolean isDashSeparator() {
        return isSeparator() && getSeparatorCodePoint() == '-';
    }

    boolean isDotSeparator() {
        return isSeparator() && getSeparatorCodePoint() == '.';
    }

    void skipTrailer(long mark) {
        assert isDotSeparator();
        assert hasNext();
        next();
        if (isZeroSegment()) {
            // could be more zeros
            assert hasNext();
            next();
            if (!isDashSeparator()) {
                assert isDotSeparator();
                skipTrailer(mark);
            }
            // return the dash separator
        } else {
            // can't skip
            reset(mark);
        }
        return;
    }

    // Magnitude (pop pop)

    int getMilestoneMagnitude() {
        if (super.isAlphaPart()) {
            final boolean nextSeparatorIsEmpty = nextSeparatorIsEmpty();

            if (alphaPartEquals("alpha", true) || alphaPartEquals("a", true) && nextSeparatorIsEmpty) {
                return 0;
            } else if (alphaPartEquals("beta", true)
                    || alphaPartEquals("b", true) && nextSeparatorIsEmpty) {
                return 1;
            } else if (alphaPartEquals("milestone", true)
                    || alphaPartEquals("m", true) && nextSeparatorIsEmpty) {
                return 2;
            } else if (alphaPartEquals("rc", true) || alphaPartEquals("cr", true)) {
                return 3;
            } else if (alphaPartEquals("snapshot", true)) {
                return 4;
            } else if (isReleaseString()) {
                return 5;
            } else if (alphaPartEquals("sp", true)) {
                return 6;
            }
        } else if (isZeroSegment()) {
            return 5;
        }
        return -1; // not a milestone
    }

    private static final String[] MILESTONES = {
            "alpha",
            "beta",
            "milestone",
            "rc",
            "snapshot",
            "", // this is really lame but it's how Maven does it
            "sp",
    };
}
