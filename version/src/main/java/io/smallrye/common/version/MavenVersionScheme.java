package io.smallrye.common.version;

import io.smallrye.common.version.AbstractVersionIterator.TokenType;

final class MavenVersionScheme extends AbstractVersionScheme<MavenVersionIterator> {
    MavenVersionScheme() {
    }

    public MavenVersionIterator iterate(final String version) {
        return new MavenVersionIterator(version);
    }

    protected int compare(MavenVersionIterator i1, MavenVersionIterator i2) {
        // pad separators
        if (i1.isSeparator()) {
            i1.insertEmptyAlpha();
        }
        if (i2.isSeparator()) {
            i2.insertEmptyAlpha();
        }

        // there is a current element
        int m1 = i1.getMilestoneMagnitude();
        int m2 = i2.getMilestoneMagnitude();
        if (m1 != -1) {
            if (m2 != -1) {
                int res = Integer.compare(m1, m2);
                if (res != 0) {
                    return res;
                }
            } else {
                // milestones always come first
                return -1;
            }
        } else {
            if (m2 != -1) {
                // milestones always come first
                return 1;
            }
        }
        TokenType p1 = i1.currentType();
        TokenType p2 = i2.currentType();
        // alpha < numbers
        if (p1 == TokenType.PART_ALPHA) {
            if (p2 == TokenType.PART_ALPHA) {
                int res = i1.compareAlphaPart(i2, true);
                if (res != 0) {
                    return res;
                }
            } else {
                assert p2 == TokenType.PART_NUMBER;
                return -1;
            }
        } else if (p2 == TokenType.PART_ALPHA) {
            assert p1 == TokenType.PART_NUMBER;
            // alpha comes before number
            return 1;
        } else {
            assert p1 == TokenType.PART_NUMBER && p2 == TokenType.PART_NUMBER;
            int res = i1.compareNumberPart(i2);
            if (res != 0) {
                return res;
            }
        }

        assert p2 == p1;

        if (!i1.hasNext()) {
            if (!i2.hasNext()) {
                // same length
                return 0;
            } else {
                // i2 is longer
                return -1;
            }
        } else {
            if (!i2.hasNext()) {
                // i1 is longer
                return 1;
            }
        }

        boolean z1 = i1.isZeroSegment();
        boolean z2 = i2.isZeroSegment();

        // next segment is a separator
        i1.next();
        i2.next();

        int s1 = i1.getSeparatorCodePoint();
        int s2 = i2.getSeparatorCodePoint();

        assert s1 == '-' || s1 == '.';
        assert s2 == '-' || s2 == '.';

        // If we just matched a number, Maven seems to compare by length.  Otherwise we zero-pad.

        if (s1 == s2) {
            // then alpha comes before number (normal rules)
            return compareNext(i1, i2);
        }
        if (s1 == '-') {
            assert s2 == '.';
            // no more segments in v1
            i2.next();
            if (p2 == TokenType.PART_ALPHA || !z2) {
                // pad with a "real" zero for next compare
                i1.insertEmptyNumber();
            } else {
                // i1 is shorter
                return -1;
            }
        } else {
            assert s1 == '.';
            assert s2 == '-';
            // no more segments in v2
            i1.next();
            if (p1 == TokenType.PART_ALPHA || !z1) {
                // pad with zero for next compare
                i2.insertEmptyNumber();
            } else {
                // i2 is shorter
                return 1;
            }
        }
        return compare(i1, i2);
    }
}
