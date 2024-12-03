package io.smallrye.common.binarysearch;

/**
 * A utility class which provides multiple variations on binary searching.
 *
 * <h2>Ranges</h2>
 *
 * The binary search algorithm operates over a range of values.
 * The search will always return in logarithmic time with respect to the search interval.
 * This implementation supports multiple kinds of value ranges:
 * <ul>
 * <li>{@code int} via the {@link #intRange()} method</li>
 * <li>{@code long} via the {@link #longRange()} method</li>
 * <li>Object ranges via the {@link #objRange()} method</li>
 * </ul>
 *
 * Each of these methods returns an object with {@code find()} methods that perform variations on the binary search.
 *
 * <h3>Range predicates</h3>
 *
 * Ranges are searched by predicate.
 * The predicate is evaluated for the lowest value within the range
 * (that is, the value closest to the {@code from} argument) for which it returns {@code true}.
 * If no value satisfies the predicate, the highest value (that is, the value given for the {@code to} argument) is returned.
 * The value returned by each {@code find()} method is always within the range {@code [from, to]}.
 * <p>
 * In order to be well-defined, the predicate must be <em>continuous</em>, which is to say that
 * given some value {@code n} which is the first value in the interval which satisfies the predicate,
 * the predicate must return {@code false} for all {@code < n} and {@code true} for all {@code >= n}
 * over the interval of {@code [from, to)}.
 * If this constraint does not hold, then the results of the search will not be well-defined.
 * The behavior of the predicate outside of this range does not affect the well-definedness of the search operation.
 *
 * <h3>Inverted ranges</h3>
 *
 * It is possible to search over an inverted range, i.e.
 * a range for which {@code to} is numerically lower than {@code from}.
 * While such searches are well-defined, it should be noted that the {@code from} bound
 * remains inclusive while the {@code to} bound remains exclusive,
 * which might be surprising in some circumstances.
 */
public final class BinarySearch {
    private BinarySearch() {
    }

    /**
     * {@return an object which can perform binary searches over an integer range (not <code>null</code>)}
     * The returned object operates on a signed range by default.
     *
     * @see IntRange#unsigned()
     */
    public static IntRange intRange() {
        return IntRange.signed;
    }

    /**
     * {@return an object which can perform binary searches over a long integer range (not <code>null</code>)}
     * The returned object operates on a signed range by default.
     *
     * @see LongRange#unsigned()
     */
    public static LongRange longRange() {
        return LongRange.signed;
    }

    /**
     * {@return an object which can perform binary searches over an object range (not <code>null</code>)}
     */
    public static ObjRange objRange() {
        return ObjRange.instance;
    }
}
