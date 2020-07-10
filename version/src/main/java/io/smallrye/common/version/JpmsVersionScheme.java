package io.smallrye.common.version;

import io.smallrye.common.version.AbstractVersionIterator.TokenType;

final class JpmsVersionScheme extends AbstractVersionScheme<JpmsVersionIterator> {
    JpmsVersionScheme() {
    }

    public JpmsVersionIterator iterate(final String version) {
        return new JpmsVersionIterator(version);
    }

    protected int compare(JpmsVersionIterator i1, JpmsVersionIterator i2) {
        TokenType p1 = i1.currentType();
        TokenType p2 = i2.currentType();

        if (p1 == TokenType.PART_ALPHA) {
            if (p2 == TokenType.PART_ALPHA) {
                int res = i1.compareAlphaPart(i2, false);
                if (res != 0) {
                    return res;
                }
            } else {
                assert p2 == TokenType.PART_NUMBER;
                // numbers come before letters lexicographically
                return 1;
            }
        } else {
            assert p1 == TokenType.PART_NUMBER;
            if (p2 == TokenType.PART_ALPHA) {
                // numbers come before letters lexicographically
                return -1;
            } else {
                assert p2 == TokenType.PART_NUMBER;
                int res = i1.compareNumberPart(i2);
                if (res != 0) {
                    return res;
                }
            }
        }

        assert p2 == p1;

        if (!i1.hasNext()) {
            if (!i2.hasNext()) {
                // same length
                return 0;
            } else {
                i2.next();
                if (i2.isSeparator() && i2.getSeparatorCodePoint() == '-') {
                    // pre-release before non-pre-release
                    return 1;
                } else {
                    // otherwise, i2 is longer
                    return -1;
                }
            }
        } else {
            if (!i2.hasNext()) {
                i1.next();
                if (i1.isSeparator() && i1.getSeparatorCodePoint() == '-') {
                    // pre-release before non-pre-release
                    return -1;
                } else {
                    // otherwise, i1 is longer
                    return 1;
                }
            }
        }

        // next segment is a separator
        i1.next();
        i2.next();

        if (i1.getSeparatorCodePoint() == '.') {
            if (i2.getSeparatorCodePoint() == '.') {
                return compareNext(i1, i2);
            } else {
                // i1's segment is longer
                return 1;
            }
        } else if (i2.getSeparatorCodePoint() == '.') {
            // i2's segment is longer
            return -1;
        }

        return compareNext(i1, i2);
    }
}
