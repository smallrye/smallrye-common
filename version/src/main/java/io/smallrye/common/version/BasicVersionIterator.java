package io.smallrye.common.version;

import java.util.NoSuchElementException;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class BasicVersionIterator extends AbstractVersionIterator {
    BasicVersionIterator(final String string) {
        super(string);
    }

    int getSeparatorMagnitude() {
        if (isEmptySeparator()) {
            return 0;
        }
        int scp = getSeparatorCodePoint();
        switch (scp) {
            case '+':
                return 1;
            case '-':
                return 2;
            case '_':
                return 3;
            case '.':
                return 4;
            default:
                throw Assert.impossibleSwitchCase(scp);
        }
    }

    void skipTrailer(long mark) {
        int sm = getSeparatorMagnitude();
        next();
        if (isNumberPart() && numberPartEquals(0)) {
            // could be more zeros
            assert hasNext();
            next();
            if (getSeparatorMagnitude() > sm) {
                // no more nonzero segments at current magnitude
                skipTrailer(mark());
                return;
            }
            // otherwise skip it
        } else {
            // can't skip
            reset(mark);
        }
        return;
    }

    public boolean hasNext() {
        TokenType pt = currentType();
        if (!super.hasNext()) {
            if (pt == TokenType.SEP || pt == TokenType.SEP_EMPTY) {
                throw Messages.msg.unexpectedEnd();
            }
            return false;
        }
        final long mark = mark();
        try {
            super.next();
            TokenType ct = currentType();
            if ((pt == TokenType.INITIAL || pt == TokenType.SEP || pt == TokenType.SEP_EMPTY)
                    && (ct == TokenType.SEP || ct == TokenType.SEP_EMPTY)) {
                throw Messages.msg.invalidCodePoint(new String(Character.toChars(getSeparatorCodePoint())), getStartIndex(),
                        string);
            }
            if (ct == TokenType.PART_ALPHA) {
                return true;
            }
            if (ct == TokenType.PART_NUMBER && !numberPartEquals(0)) {
                return true;
            }
            // otherwise it depends on the next segment
            return hasNext();
        } finally {
            reset(mark);
        }
    }

    public void next() throws NoSuchElementException, VersionSyntaxException {
        TokenType pt = currentType();
        super.next();
        TokenType ct = currentType();
        if ((pt == TokenType.INITIAL || pt == TokenType.SEP || pt == TokenType.SEP_EMPTY)
                && (ct == TokenType.SEP || ct == TokenType.SEP_EMPTY)) {
            throw Messages.msg.invalidCodePoint(new String(Character.toChars(getSeparatorCodePoint())), getStartIndex(),
                    string);
        }
        if (isSeparator()) {
            // skip trailing zero segments of all same- or lower-order separators
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
}
