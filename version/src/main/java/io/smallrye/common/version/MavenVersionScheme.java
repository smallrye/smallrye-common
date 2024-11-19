package io.smallrye.common.version;

import io.smallrye.common.version.AbstractVersionIterator.TokenType;

final class MavenVersionScheme extends AbstractVersionScheme<MavenVersionIterator> {
    MavenVersionScheme() {
    }

    public MavenVersionIterator iterate(final String version) {
        return new MavenVersionIterator(version);
    }

    protected int compareNext(final MavenVersionIterator i1, final MavenVersionIterator i2) {
        if (!i1.hasNext()) {
            if (!i2.hasNext()) {
                // same length
                return 0;
            }
            i2.next();
            return -compareZero(i2);
        } else if (!i2.hasNext()) {
            i1.next();
            return compareZero(i1);
        }
        i1.next();
        i2.next();
        return compare(i1, i2);
    }

    // XXX this works, just need to check +/-

    protected int compareZero(MavenVersionIterator i) {
        if (!i.hasNext()) {
            return 0;
        }
        i.next();
        // pad separator
        if (i.isSeparator()) {
            i.insertEmptyAlpha();
        }
        int m = i.getMilestoneMagnitude();
        if (m != -1) {
            int res = Integer.compare(m, 5);
            if (res != 0) {
                return res;
            }
            // continue on
            if (i.hasNext()) {
                i.next();
                return compareZero(i);
            } else {
                // equal
                return 0;
            }
        } else {
            // greater than zero
            return 1;
        }
    }

    protected int compare(MavenVersionIterator i1, MavenVersionIterator i2) {
        // pad separators
        if (i1.isSeparator()) {
            i1.insertEmptyNumber();
        }
        if (i2.isSeparator()) {
            i2.insertEmptyNumber();
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

        // skip separators and proceed
        if (i1.hasNext()) {
            i1.next();
            if (i2.hasNext()) {
                i2.next();
                return compareNext(i1, i2);
            } else {
                // i2 is at end
                return compareZero(i1);
            }
        } else {
            if (i2.hasNext()) {
                i2.next();
                // i1 is at end
                return -compareZero(i2);
            } else {
                return 0;
            }
        }
    }
}
