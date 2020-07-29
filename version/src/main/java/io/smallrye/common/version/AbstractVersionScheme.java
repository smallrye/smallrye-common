package io.smallrye.common.version;

/**
 * A version scheme base class that provides basic comparison functionality.
 */
public abstract class AbstractVersionScheme<I extends AbstractVersionIterator> implements VersionScheme {
    public int compare(final String v1, final String v2) {
        return compareNext(iterate(v1), iterate(v2));
    }

    public abstract I iterate(final String version);

    public StringBuilder appendCanonicalized(final StringBuilder target, final String original) {
        final VersionIterator iterator = iterate(original);
        while (iterator.hasNext()) {
            iterator.next();
            iterator.appendPartTo(target);
        }
        return target;
    }

    /**
     * Compare the next elements of the given iterators. If one iterator is at the end of iteration (its iterator
     * reports no next element), the corresponding version is considered to come before the other version. If
     * both iterators are at the end of iteration, the versions are considered equal. Otherwise,
     * {@link #compare(AbstractVersionIterator, AbstractVersionIterator) compare(I, I)} is called to compare the
     * iterators' current tokens.
     *
     * @param i1 the iterator for the first version (must not be {@code null})
     * @param i2 the iterator for the second version (must not be {@code null})
     * @return {@code -1}, {@code 0}, or {@code 1} if the first version is less than, equal to, or greater than the
     *         second version
     */
    protected int compareNext(I i1, I i2) {
        if (!i1.hasNext()) {
            // same length
            // i2 is longer
            return !i2.hasNext() ? 0 : -1;
        } else if (!i2.hasNext()) {
            // i1 is longer
            return 1;
        }
        i1.next();
        i2.next();
        return compare(i1, i2);
    }

    /**
     * Compare the current tokens of the two given iterators. If the tokens are equal, then this method should
     * recurse to {@link #compareNext(AbstractVersionIterator, AbstractVersionIterator) compareNext(I, I)} (or
     * equivalent) to compare the next tokens.
     *
     * @param i1 the iterator for the first version (must not be {@code null})
     * @param i2 the iterator for the second version (must not be {@code null})
     * @return {@code -1}, {@code 0}, or {@code 1} if the first version is less than, equal to, or greater than the
     *         second version
     */
    protected abstract int compare(I i1, I i2);
}
