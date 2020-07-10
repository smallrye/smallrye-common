package io.smallrye.common.version;

final class BasicVersionScheme extends AbstractVersionScheme<BasicVersionIterator> {
    BasicVersionScheme() {
    }

    public BasicVersionIterator iterate(final String version) {
        return new BasicVersionIterator(version);
    }

    protected int compare(final BasicVersionIterator i1, final BasicVersionIterator i2) {
        // alpha comes before numbers
        if (i1.isAlphaPart()) {
            if (i2.isAlphaPart()) {
                int res = i1.compareAlphaPart(i2, false);
                if (res != 0) {
                    return res;
                }
            } else {
                assert i2.isNumberPart();
                return -1;
            }
        } else {
            if (i2.isAlphaPart()) {
                return 1;
            } else {
                // both numbers
                int res = i1.compareNumberPart(i2);
                if (res != 0) {
                    return res;
                }
            }
        }

        if (!i1.hasNext()) {
            if (!i2.hasNext()) {
                // same length
                return 0;
            } else {
                // i2 is longer
                i2.next();
                if (i2.isEmptySeparator()) {
                    return -1;
                } else {
                    i1.insertEmptyNumber();
                }
                i2.next();
                return compare(i1, i2);
            }
        } else {
            if (!i2.hasNext()) {
                // i1 is longer
                i1.next();
                if (i1.isEmptySeparator()) {
                    return 1;
                } else {
                    i2.insertEmptyNumber();
                }
                i1.next();
                return compare(i1, i2);
            }
        }

        // separators

        i1.next();
        i2.next();

        int s1 = i1.getSeparatorMagnitude();
        int s2 = i2.getSeparatorMagnitude();

        if (s1 < s2) {
            // pad out s1
            i1.insertEmptyNumber();
            i2.next();
            return compare(i1, i2);
        } else if (s1 > s2) {
            // pad out s2
            i2.insertEmptyNumber();
            i1.next();
            return compare(i1, i2);
        } else {
            assert s1 == s2;
            return compareNext(i1, i2);
        }
    }
}
