package io.smallrye.common.search;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import io.smallrye.common.function.ObjIntFunction;
import io.smallrye.common.function.ObjIntPredicate;
import io.smallrye.common.function.ObjLongFunction;
import io.smallrye.common.function.ObjLongPredicate;

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
 * given some value {@code n},
 * the predicate must return {@code false} for all {@code < n} and {@code true} for all {@code >= n}
 * when {@code from ≤ n ≤ to}.
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

    /**
     * A getter function which can satisfy {@link ObjIntFunction ObjIntFunction&lt;E[], E>} for accessing
     * values in an array.
     *
     * @param array the array (must not be {@code null})
     * @param index the array index
     * @return the array value at the index
     * @param <E> the array element type
     */
    public static <E> E getFromArray(E[] array, int index) {
        return array[index];
    }

    /**
     * Search operations which apply over an {@code int} range.
     */
    public static final class IntRange {
        private final IntBinaryOperator midpoint;
        private final InCollection inCollection;

        private IntRange(final IntBinaryOperator midpoint) {
            this.midpoint = midpoint;
            inCollection = new InCollection(midpoint);
        }

        private static final IntRange signed = new IntRange(BinarySearch::signedMidpoint);
        private static final IntRange unsigned = new IntRange(BinarySearch::unsignedMidpoint);

        /**
         * Get the lowest index within the given range that satisfies the given test.
         *
         * @param from the low end of the range (inclusive)
         * @param to the high end of the range (exclusive)
         * @param test the test (must not be {@code null})
         * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
         *         within the range
         */
        public int find(int from, int to, IntPredicate test) {
            return searchInt(from, to, test, null, null, null, midpoint, IntRange::find0);
        }

        private static boolean find0(int idx, IntPredicate test, Void ignored0, Void ignored1, Void ignored2) {
            return test.test(idx);
        }

        /**
         * {@return search operations over a signed-integer space}
         */
        public IntRange signed() {
            return signed;
        }

        /**
         * {@return search operations over an unsigned-integer space}
         */
        public IntRange unsigned() {
            return unsigned;
        }

        /**
         * {@return search operations over a space defined by a custom midpoint function}
         */
        public CustomMidpoint customMidpoint() {
            return CustomMidpoint.instance;
        }

        /**
         * {@return search operations over a collection using the current signedness rules for the midpoint function}
         */
        public InCollection inCollection() {
            return inCollection;
        }

        /**
         * Search operations which apply over an {@code int} range using a custom midpoint function.
         */
        public static final class CustomMidpoint {
            private static final CustomMidpoint instance = new CustomMidpoint();

            private CustomMidpoint() {
            }

            /**
             * Get the lowest index within the given range that satisfies the given test.
             *
             * @param from the low end of the range (inclusive)
             * @param to the high end of the range (exclusive)
             * @param midpoint the midpoint function (must not be {@code null})
             * @param test the test (must not be {@code null})
             * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
             *         within the range
             */
            public int find(int from, int to, IntBinaryOperator midpoint, IntPredicate test) {
                return searchInt(from, to, test, null, null, null, midpoint, CustomMidpoint::find0);
            }

            private static boolean find0(int idx, IntPredicate test, Void ignored0, Void ignored1, Void ignored2) {
                return test.test(idx);
            }

            /**
             * {@return search operations over a collection using a custom midpoint function}
             */
            public InCollection inCollection() {
                return InCollection.instance;
            }

            /**
             * Search operations within a collection using a custom midpoint function.
             */
            public static final class InCollection {
                private static final InCollection instance = new InCollection();

                private InCollection() {
                }

                /**
                 * Get the lowest index within the given range that satisfies the given test.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param midpoint the midpoint function (must not be {@code null})
                 * @param test the test (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <C> the collection type
                 */
                public <C> int find(C collection, int from, int to, IntBinaryOperator midpoint, ObjIntPredicate<C> test) {
                    return searchInt(from, to, collection, test, (Void) null, (Void) null, midpoint,
                            IntRange.InCollection::find0);
                }

                /**
                 * Get the lowest index within the given random-access list that satisfies the given test.
                 *
                 * @param list the list object (must not be {@code null})
                 * @param midpoint the midpoint function (must not be {@code null})
                 * @param test the test (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <L> the list type
                 */
                public <L extends List<?>> int find(L list, IntBinaryOperator midpoint, ObjIntPredicate<L> test) {
                    return find(list, 0, list.size(), midpoint, test);
                }

                /**
                 * {@return search operations which operate by an extracted key}
                 */
                public ByKey byKey() {
                    return ByKey.instance;
                }

                /**
                 * Search operations within a collection which operate on an extracted key using a custom midpoint function.
                 */
                public static final class ByKey {
                    private static final ByKey instance = new ByKey();

                    private ByKey() {
                    }

                    /**
                     * Get the lowest index within the given range that satisfies the given test.
                     *
                     * @param collection the collection object (must not be {@code null})
                     * @param from the low end of the range (inclusive)
                     * @param to the high end of the range (exclusive)
                     * @param midpoint the midpoint function (must not be {@code null})
                     * @param keyExtractor the key extraction function (must not be {@code null})
                     * @param keyTester the test for the key (must not be {@code null})
                     * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy
                     *         the test within the range
                     * @param <C> the collection type
                     * @param <K> the key type
                     */
                    public <C, K> int find(C collection, int from, int to, IntBinaryOperator midpoint,
                            ObjIntFunction<C, K> keyExtractor, Predicate<K> keyTester) {
                        return searchInt(from, to, collection, keyExtractor, keyTester, (Void) null, midpoint,
                                IntRange.InCollection.ByKey::find0);
                    }

                    /**
                     * Get the lowest index within the given range that satisfies the given test for the search value.
                     *
                     * @param collection the collection object (must not be {@code null})
                     * @param searchVal the value to search for
                     * @param from the low end of the range (inclusive)
                     * @param to the high end of the range (exclusive)
                     * @param midpoint the midpoint function (must not be {@code null})
                     * @param keyExtractor the key extraction function (must not be {@code null})
                     * @param keyTester the test for the key (must not be {@code null})
                     * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy
                     *         the test within the range
                     * @param <C> the collection type
                     * @param <K> the key type
                     * @param <V> the search value type
                     */
                    public <C, K, V> int find(C collection, V searchVal, int from, int to, IntBinaryOperator midpoint,
                            ObjIntFunction<C, K> keyExtractor,
                            BiPredicate<V, K> keyTester) {
                        return searchInt(from, to, collection, searchVal, keyExtractor, keyTester, midpoint,
                                IntRange.InCollection.ByKey::find1);
                    }

                    /**
                     * Get the lowest index within the given range whose key is equal to or greater than
                     * the search key, according to the {@linkplain Comparator#naturalOrder() natural order} of
                     * the search space.
                     *
                     * @param collection the collection object (must not be {@code null})
                     * @param searchKey the key to search for (must not be {@code null})
                     * @param from the low end of the range (inclusive)
                     * @param to the high end of the range (exclusive)
                     * @param midpoint the midpoint function (must not be {@code null})
                     * @param keyExtractor the key extraction function (must not be {@code null})
                     * @return the lowest index within the range which satisfies the condition, or {@code to} if no values
                     *         satisfy the condition within the range
                     * @param <C> the collection type
                     * @param <K> the key type
                     */
                    public <C, K extends Comparable<? super K>> int findFirst(C collection, K searchKey, int from, int to,
                            IntBinaryOperator midpoint, ObjIntFunction<C, K> keyExtractor) {
                        return findFirst(collection, searchKey, from, to, midpoint, keyExtractor, Comparator.naturalOrder());
                    }

                    /**
                     * Get the lowest index within the given range whose key is equal to or greater than
                     * the search key, according to the given comparator.
                     *
                     * @param collection the collection object (must not be {@code null})
                     * @param searchKey the key to search for (must not be {@code null})
                     * @param from the low end of the range (inclusive)
                     * @param to the high end of the range (exclusive)
                     * @param midpoint the midpoint function (must not be {@code null})
                     * @param keyExtractor the key extraction function (must not be {@code null})
                     * @param cmp the comparator (must not be {@code null})
                     * @return the lowest index within the range which satisfies the condition, or {@code to} if no values
                     *         satisfy the condition within the range
                     * @param <C> the collection type
                     * @param <K> the key type
                     */
                    public <C, K> int findFirst(C collection, K searchKey, int from, int to, IntBinaryOperator midpoint,
                            ObjIntFunction<C, K> keyExtractor,
                            Comparator<? super K> cmp) {
                        return searchInt(from, to, collection, searchKey, keyExtractor, cmp, midpoint,
                                IntRange.InCollection.ByKey::find2);
                    }
                }
            }
        }

        /**
         * Search operations within a collection.
         */
        public static final class InCollection {
            private final IntBinaryOperator midpoint;
            private final ByKey byKey;

            private InCollection(final IntBinaryOperator midpoint) {
                this.midpoint = midpoint;
                byKey = new ByKey(midpoint);
            }

            /**
             * Get the lowest index within the given range that satisfies the given test.
             *
             * @param collection the collection object (must not be {@code null})
             * @param from the low end of the range (inclusive)
             * @param to the high end of the range (exclusive)
             * @param test the test (must not be {@code null})
             * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
             *         within the range
             * @param <C> the collection type
             */
            public <C> int find(C collection, int from, int to, ObjIntPredicate<C> test) {
                return searchInt(from, to, collection, test, (Void) null, (Void) null, midpoint, InCollection::find0);
            }

            /**
             * Get the lowest index within the given random-access list that satisfies the given test.
             *
             * @param list the list object (must not be {@code null})
             * @param test the test (must not be {@code null})
             * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
             *         within the range
             * @param <L> the list type
             */
            public <L extends List<?>> int find(L list, ObjIntPredicate<L> test) {
                return find(list, 0, list.size(), test);
            }

            private static <C> boolean find0(int idx, C collection, ObjIntPredicate<C> test, Void ignored0, Void ignored1) {
                return test.test(collection, idx);
            }

            /**
             * {@return search operations which operate by an extracted key}
             */
            public ByKey byKey() {
                return byKey;
            }

            /**
             * Search operations within a collection which operate on an extracted key.
             */
            public static final class ByKey {
                private final IntBinaryOperator midpoint;

                private ByKey(IntBinaryOperator midpoint) {
                    this.midpoint = midpoint;
                }

                /**
                 * Get the lowest index within the given range that satisfies the given test.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param keyTester the test for the key (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <C, K> int find(C collection, int from, int to, ObjIntFunction<C, K> keyExtractor,
                        Predicate<K> keyTester) {
                    return searchInt(from, to, collection, keyExtractor, keyTester, (Void) null, midpoint, ByKey::find0);
                }

                private static <C, K> boolean find0(int idx, C collection, ObjIntFunction<C, K> keyExtractor,
                        Predicate<K> keyTester, Void ignored) {
                    return keyTester.test(keyExtractor.apply(collection, idx));
                }

                /**
                 * Get the lowest index within the given range that satisfies the given test for the search value.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchVal the value to search for
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param keyTester the test for the key (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <C> the collection type
                 * @param <K> the key type
                 * @param <V> the value type
                 */
                public <C, K, V> int find(C collection, V searchVal, int from, int to, ObjIntFunction<C, K> keyExtractor,
                        BiPredicate<V, K> keyTester) {
                    return searchInt(from, to, collection, searchVal, keyExtractor, keyTester, midpoint, ByKey::find1);
                }

                private static <C, K, V> boolean find1(int idx, C collection, V searchVal, ObjIntFunction<C, K> keyExtractor,
                        BiPredicate<V, K> keyTester) {
                    return keyTester.test(searchVal, keyExtractor.apply(collection, idx));
                }

                /**
                 * Get the lowest index within the given range whose key is equal to or greater than
                 * the search key, according to the {@linkplain Comparator#naturalOrder() natural order} of
                 * the search space.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchKey the key to search for (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @return the lowest index within the range which satisfies the condition, or {@code to} if no values satisfy
                 *         the condition within the range
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <C, K extends Comparable<? super K>> int findFirst(C collection, K searchKey, int from, int to,
                        ObjIntFunction<C, K> keyExtractor) {
                    return findFirst(collection, searchKey, from, to, keyExtractor, Comparator.naturalOrder());
                }

                /**
                 * Get the lowest index within the given range whose key is equal to or greater than
                 * the search key, according to the given comparator.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchKey the key to search for (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param cmp the comparator (must not be {@code null})
                 * @return the lowest index within the range which satisfies the condition, or {@code to} if no values satisfy
                 *         the condition within the range
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <C, K> int findFirst(C collection, K searchKey, int from, int to, ObjIntFunction<C, K> keyExtractor,
                        Comparator<? super K> cmp) {
                    return searchInt(from, to, collection, searchKey, keyExtractor, cmp, midpoint, ByKey::find2);
                }

                private static <C, K> boolean find2(int idx, C collection, K searchKey, ObjIntFunction<C, K> keyExtractor,
                        Comparator<? super K> cmp) {
                    return cmp.compare(searchKey, keyExtractor.apply(collection, idx)) >= 0;
                }
            }
        }
    }

    /**
     * Search operations which apply over an {@code long} range.
     */
    public static final class LongRange {
        private final LongBinaryOperator midpoint;
        private final InCollection inCollection;

        private LongRange(final LongBinaryOperator midpoint) {
            this.midpoint = midpoint;
            inCollection = new InCollection(midpoint);
        }

        private static final LongRange signed = new LongRange(BinarySearch::signedMidpoint);
        private static final LongRange unsigned = new LongRange(BinarySearch::unsignedMidpoint);

        /**
         * Get the lowest index within the given range that satisfies the given test.
         *
         * @param from the low end of the range (inclusive)
         * @param to the high end of the range (exclusive)
         * @param test the test (must not be {@code null})
         * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
         *         within the range
         */
        public long find(long from, long to, LongPredicate test) {
            return searchLong(from, to, test, null, null, null, midpoint, LongRange::find0);
        }

        private static boolean find0(long idx, LongPredicate test, Void ignored0, Void ignored1, Void ignored2) {
            return test.test(idx);
        }

        /**
         * {@return search operations over a signed-integer space}
         */
        public LongRange signed() {
            return signed;
        }

        /**
         * {@return search operations over an unsigned-integer space}
         */
        public LongRange unsigned() {
            return unsigned;
        }

        /**
         * {@return search operations over a space defined by a custom midpoint function}
         */
        public CustomMidpoint customMidpoint() {
            return CustomMidpoint.instance;
        }

        /**
         * {@return search operations over a collection using the current signedness rules for the midpoint function}
         */
        public InCollection inCollection() {
            return inCollection;
        }

        /**
         * Search operations which apply over an {@code long} range using a custom midpoint function.
         */
        public static final class CustomMidpoint {
            private static final CustomMidpoint instance = new CustomMidpoint();

            private CustomMidpoint() {
            }

            /**
             * Get the lowest index within the given range that satisfies the given test.
             *
             * @param from the low end of the range (inclusive)
             * @param to the high end of the range (exclusive)
             * @param midpoint the midpoint function (must not be {@code null})
             * @param test the test (must not be {@code null})
             * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
             *         within the range
             */
            public long find(long from, long to, LongBinaryOperator midpoint, LongPredicate test) {
                return searchLong(from, to, test, null, null, null, midpoint, CustomMidpoint::find0);
            }

            private static boolean find0(long idx, LongPredicate test, Void ignored0, Void ignored1, Void ignored2) {
                return test.test(idx);
            }

            /**
             * {@return search operations over a collection using a custom midpoint function}
             */
            public InCollection inCollection() {
                return InCollection.instance;
            }

            /**
             * Search operations within a collection using a custom midpoint function.
             */
            public static final class InCollection {
                private static final InCollection instance = new InCollection();

                private InCollection() {
                }

                /**
                 * Get the lowest index within the given range that satisfies the given test.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param midpoint the midpoint function (must not be {@code null})
                 * @param test the test (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <C> the collection type
                 */
                public <C> long find(C collection, long from, long to, LongBinaryOperator midpoint, ObjLongPredicate<C> test) {
                    return searchLong(from, to, collection, test, (Void) null, (Void) null, midpoint,
                            LongRange.InCollection::find0);
                }

                /**
                 * {@return search operations which operate by an extracted key}
                 */
                public ByKey byKey() {
                    return ByKey.instance;
                }

                /**
                 * Search operations within a collection which operate on an extracted key using a custom midpoint function.
                 */
                public static final class ByKey {
                    private static final ByKey instance = new ByKey();

                    private ByKey() {
                    }

                    /**
                     * Get the lowest index within the given range that satisfies the given test.
                     *
                     * @param collection the collection object (must not be {@code null})
                     * @param from the low end of the range (inclusive)
                     * @param to the high end of the range (exclusive)
                     * @param midpoint the midpoint function (must not be {@code null})
                     * @param keyExtractor the key extraction function (must not be {@code null})
                     * @param keyTester the test for the key (must not be {@code null})
                     * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy
                     *         the test within the range
                     * @param <C> the collection type
                     * @param <K> the key type
                     */
                    public <C, K> long find(C collection, long from, long to, LongBinaryOperator midpoint,
                            ObjLongFunction<C, K> keyExtractor, Predicate<K> keyTester) {
                        return searchLong(from, to, collection, keyExtractor, keyTester, (Void) null, midpoint,
                                LongRange.InCollection.ByKey::find0);
                    }

                    /**
                     * Get the lowest index within the given range that satisfies the given test for the search value.
                     *
                     * @param collection the collection object (must not be {@code null})
                     * @param searchVal the value to search for
                     * @param from the low end of the range (inclusive)
                     * @param to the high end of the range (exclusive)
                     * @param midpoint the midpoint function (must not be {@code null})
                     * @param keyExtractor the key extraction function (must not be {@code null})
                     * @param keyTester the test for the key (must not be {@code null})
                     * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy
                     *         the test within the range
                     * @param <C> the collection type
                     * @param <K> the key type
                     * @param <V> the value type
                     */
                    public <C, K, V> long find(C collection, V searchVal, long from, long to, LongBinaryOperator midpoint,
                            ObjLongFunction<C, K> keyExtractor, BiPredicate<V, K> keyTester) {
                        return searchLong(from, to, collection, searchVal, keyExtractor, keyTester, midpoint,
                                LongRange.InCollection.ByKey::find1);
                    }

                    /**
                     * Get the lowest index within the given range whose key is equal to or greater than
                     * the search key, according to the {@linkplain Comparator#naturalOrder() natural order} of
                     * the search space.
                     *
                     * @param collection the collection object (must not be {@code null})
                     * @param searchKey the key to search for (must not be {@code null})
                     * @param from the low end of the range (inclusive)
                     * @param to the high end of the range (exclusive)
                     * @param midpoint the midpoint function (must not be {@code null})
                     * @param keyExtractor the key extraction function (must not be {@code null})
                     * @return the lowest index within the range which satisfies the condition, or {@code to} if no values
                     *         satisfy the condition within the range
                     * @param <C> the collection type
                     * @param <K> the key type
                     */
                    public <C, K extends Comparable<? super K>> long findFirst(C collection, K searchKey, long from, long to,
                            LongBinaryOperator midpoint, ObjLongFunction<C, K> keyExtractor) {
                        return findFirst(collection, searchKey, from, to, midpoint, keyExtractor, Comparator.naturalOrder());
                    }

                    /**
                     * Get the lowest index within the given range whose key is equal to or greater than
                     * the search key, according to the given comparator.
                     *
                     * @param collection the collection object (must not be {@code null})
                     * @param searchKey the key to search for (must not be {@code null})
                     * @param from the low end of the range (inclusive)
                     * @param to the high end of the range (exclusive)
                     * @param midpoint the midpoint function (must not be {@code null})
                     * @param keyExtractor the key extraction function (must not be {@code null})
                     * @param cmp the comparator (must not be {@code null})
                     * @return the lowest index within the range which satisfies the condition, or {@code to} if no values
                     *         satisfy the condition within the range
                     * @param <C> the collection type
                     * @param <K> the key type
                     */
                    public <C, K> long findFirst(C collection, K searchKey, long from, long to, LongBinaryOperator midpoint,
                            ObjLongFunction<C, K> keyExtractor, Comparator<? super K> cmp) {
                        return searchLong(from, to, collection, searchKey, keyExtractor, cmp, midpoint,
                                LongRange.InCollection.ByKey::find2);
                    }
                }
            }
        }

        /**
         * Search operations within a collection.
         */
        public static final class InCollection {
            private final LongBinaryOperator midpoint;
            private final ByKey byKey;

            private InCollection(final LongBinaryOperator midpoint) {
                this.midpoint = midpoint;
                byKey = new ByKey(midpoint);
            }

            /**
             * Get the lowest index within the given range that satisfies the given test.
             *
             * @param collection the collection object (must not be {@code null})
             * @param from the low end of the range (inclusive)
             * @param to the high end of the range (exclusive)
             * @param test the test (must not be {@code null})
             * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
             *         within the range
             * @param <C> the collection type
             */
            public <C> long find(C collection, long from, long to, ObjLongPredicate<C> test) {
                return searchLong(from, to, collection, test, (Void) null, (Void) null, midpoint, InCollection::find0);
            }

            private static <C> boolean find0(long idx, C collection, ObjLongPredicate<C> test, Void ignored0, Void ignored1) {
                return test.test(collection, idx);
            }

            /**
             * {@return search operations which operate by an extracted key}
             */
            public ByKey byKey() {
                return byKey;
            }

            /**
             * Search operations within a collection which operate on an extracted key.
             */
            public static final class ByKey {
                private final LongBinaryOperator midpoint;

                private ByKey(LongBinaryOperator midpoint) {
                    this.midpoint = midpoint;
                }

                /**
                 * Get the lowest index within the given range that satisfies the given test.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param keyTester the test for the key (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <C, K> long find(C collection, long from, long to, ObjLongFunction<C, K> keyExtractor,
                        Predicate<K> keyTester) {
                    return searchLong(from, to, collection, keyExtractor, keyTester, (Void) null, midpoint, ByKey::find0);
                }

                private static <C, K> boolean find0(long idx, C collection, ObjLongFunction<C, K> keyExtractor,
                        Predicate<K> keyTester, Void ignored) {
                    return keyTester.test(keyExtractor.apply(collection, idx));
                }

                /**
                 * Get the lowest index within the given range that satisfies the given test for the search value.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchVal the value to search for
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param keyTester the test for the key (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <C> the collection type
                 * @param <K> the key type
                 * @param <V> the value type
                 */
                public <C, K, V> long find(C collection, V searchVal, long from, long to, ObjLongFunction<C, K> keyExtractor,
                        BiPredicate<V, K> keyTester) {
                    return searchLong(from, to, collection, searchVal, keyExtractor, keyTester, midpoint, ByKey::find1);
                }

                private static <C, K, V> boolean find1(long idx, C collection, V searchVal, ObjLongFunction<C, K> keyExtractor,
                        BiPredicate<V, K> keyTester) {
                    return keyTester.test(searchVal, keyExtractor.apply(collection, idx));
                }

                /**
                 * Get the lowest index within the given range whose key is equal to or greater than
                 * the search key, according to the {@linkplain Comparator#naturalOrder() natural order} of
                 * the search space.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchKey the key to search for (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @return the lowest index within the range which satisfies the condition, or {@code to} if no values satisfy
                 *         the condition within the range
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <C, K extends Comparable<? super K>> long findFirst(C collection, K searchKey, long from, long to,
                        ObjLongFunction<C, K> keyExtractor) {
                    return findFirst(collection, searchKey, from, to, keyExtractor, Comparator.naturalOrder());
                }

                /**
                 * Get the lowest index within the given range whose key is equal to or greater than
                 * the search key, according to the given comparator.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchKey the key to search for (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param cmp the comparator (must not be {@code null})
                 * @return the lowest index within the range which satisfies the condition, or {@code to} if no values satisfy
                 *         the condition within the range
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <C, K> long findFirst(C collection, K searchKey, long from, long to, ObjLongFunction<C, K> keyExtractor,
                        Comparator<? super K> cmp) {
                    return searchLong(from, to, collection, searchKey, keyExtractor, cmp, midpoint, ByKey::find2);
                }

                private static <C, K> boolean find2(long idx, C collection, K searchKey, ObjLongFunction<C, K> keyExtractor,
                        Comparator<? super K> cmp) {
                    return cmp.compare(searchKey, keyExtractor.apply(collection, idx)) >= 0;
                }
            }
        }
    }

    /**
     * Binary searches over a range of objects.
     */
    public static final class ObjRange {
        private static final ObjRange instance = new ObjRange();

        private ObjRange() {
        }

        /**
         * Get the lowest index within the given range that satisfies the given test.
         *
         * @param from the low end of the range (inclusive)
         * @param to the high end of the range (exclusive)
         * @param midpoint the midpoint function (must not be {@code null})
         * @param test the test (must not be {@code null})
         * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
         *         within the range
         * @param <T> the index type
         */
        public <T> T find(T from, T to, BinaryOperator<T> midpoint, Predicate<T> test) {
            return searchObject(from, to, test, (Void) null, (Void) null, (Void) null, midpoint, ObjRange::find0);
        }

        private static <T> boolean find0(T idx, Predicate<T> test, Void ignored0, Void ignored1, Void ignored2) {
            return test.test(idx);
        }

        /**
         * {@return search operations over a collection}
         */
        public static InCollection inCollection() {
            return InCollection.instance;
        }

        /**
         * Search operations within a collection.
         */
        public static final class InCollection {
            private static final InCollection instance = new InCollection();

            private InCollection() {
            }

            /**
             * Get the lowest index within the given range that satisfies the given test.
             *
             * @param collection the collection object (must not be {@code null})
             * @param from the low end of the range (inclusive)
             * @param to the high end of the range (exclusive)
             * @param midpoint the midpoint function (must not be {@code null})
             * @param test the test (must not be {@code null})
             * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the test
             *         within the range
             * @param <C> the collection type
             * @param <T> the index type
             */
            public <T, C> T find(C collection, T from, T to, BinaryOperator<T> midpoint, BiPredicate<C, T> test) {
                return searchObject(from, to, collection, test, (Void) null, (Void) null, midpoint, InCollection::find0);
            }

            private static <T, C> boolean find0(T idx, C collection, BiPredicate<C, T> test, Void ignored0, Void ignored1) {
                return test.test(collection, idx);
            }

            /**
             * {@return search operations which operate by an extracted key}
             */
            public ByKey byKey() {
                return ByKey.instance;
            }

            /**
             * Search operations within a collection which operate on an extracted key using a custom midpoint function.
             */
            public static final class ByKey {
                private static final ByKey instance = new ByKey();

                private ByKey() {
                }

                /**
                 * Get the lowest index within the given range that satisfies the given test.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param midpoint the midpoint function (must not be {@code null})
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param test the test for the key (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <T> the index type
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <T, C, K> T find(C collection, T from, T to, BinaryOperator<T> midpoint,
                        BiFunction<C, T, K> keyExtractor,
                        Predicate<K> test) {
                    return searchObject(from, to, collection, keyExtractor, test, (Void) null, midpoint, ByKey::find0);
                }

                private static <T, C, K> boolean find0(T idx, C collection, BiFunction<C, T, K> keyExtractor, Predicate<K> test,
                        Void ignored) {
                    return test.test(keyExtractor.apply(collection, idx));
                }

                /**
                 * Get the lowest index within the given range that satisfies the given test for the search value.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchVal the value to search for
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param midpoint the midpoint function (must not be {@code null})
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param keyTester the test for the key (must not be {@code null})
                 * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the
                 *         test within the range
                 * @param <T> the index type
                 * @param <C> the collection type
                 * @param <K> the key type
                 * @param <V> the value type
                 */
                public <T, C, K, V> T find(C collection, V searchVal, T from, T to, BinaryOperator<T> midpoint,
                        BiFunction<C, T, K> keyExtractor,
                        BiPredicate<V, K> keyTester) {
                    return searchObject(from, to, collection, searchVal, keyExtractor, keyTester, midpoint, ByKey::find1);
                }

                private static <T, C, K, V> boolean find1(T idx, C collection, V searchVal, BiFunction<C, T, K> keyExtractor,
                        BiPredicate<V, K> test) {
                    return test.test(searchVal, keyExtractor.apply(collection, idx));
                }

                /**
                 * Get the lowest index within the given range whose key is equal to or greater than
                 * the search key, according to the {@linkplain Comparator#naturalOrder() natural order} of
                 * the search space.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchKey the key to search for (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param midpoint the midpoint function (must not be {@code null})
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @return the lowest index within the range which satisfies the condition, or {@code to} if no values satisfy
                 *         the condition within the range
                 * @param <T> the index type
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <T, C, K extends Comparable<? super K>> T findFirst(C collection, K searchKey, T from, T to,
                        BinaryOperator<T> midpoint, BiFunction<C, T, K> keyExtractor) {
                    return findFirst(collection, searchKey, from, to, midpoint, keyExtractor, Comparator.naturalOrder());
                }

                /**
                 * Get the lowest index within the given range whose key is equal to or greater than
                 * the search key, according to the given comparator.
                 *
                 * @param collection the collection object (must not be {@code null})
                 * @param searchKey the key to search for (must not be {@code null})
                 * @param from the low end of the range (inclusive)
                 * @param to the high end of the range (exclusive)
                 * @param midpoint the midpoint function (must not be {@code null})
                 * @param keyExtractor the key extraction function (must not be {@code null})
                 * @param cmp the comparator (must not be {@code null})
                 * @return the lowest index within the range which satisfies the condition, or {@code to} if no values satisfy
                 *         the condition within the range
                 * @param <T> the index type
                 * @param <C> the collection type
                 * @param <K> the key type
                 */
                public <T, C, K> T findFirst(C collection, K searchKey, T from, T to, BinaryOperator<T> midpoint,
                        BiFunction<C, T, K> keyExtractor,
                        Comparator<? super K> cmp) {
                    return searchObject(from, to, collection, searchKey, keyExtractor, cmp, midpoint, ByKey::find2);
                }

                private static <T, C, K> boolean find2(T idx, C collection, K searchKey, BiFunction<C, T, K> keyExtractor,
                        Comparator<? super K> cmp) {
                    return cmp.compare(searchKey, keyExtractor.apply(collection, idx)) >= 0;
                }
            }
        }
    }

    // midpoint functions

    private static int signedMidpoint(int from, int to) {
        return from + (to - from >> 1);
    }

    private static int unsignedMidpoint(int from, int to) {
        return from + (to - from >>> 1);
    }

    private static long signedMidpoint(long from, long to) {
        return from + (to - from >> 1);
    }

    private static long unsignedMidpoint(long from, long to) {
        return from + (to - from >>> 1);
    }

    // Theoretically, we could use only the object variation and rely on box types, compiler magic and
    // future value type support for performance. However, this is not yet a reality so we will have
    // three versions for now: one that operates on object ranges, and two that operate on integer ranges (int/long).

    private interface ObjIdxTestFunction<T, A, B, C, D> {
        boolean test(T idx, A a, B b, C c, D d);
    }

    private static <T, A, B, C, D> T searchObject(T from, T to, A a, B b, C c, D d, BinaryOperator<T> midpoint,
            ObjIdxTestFunction<T, A, B, C, D> test) {
        T mid = midpoint.apply(from, to);
        T newMid;
        for (;;) {
            if (test.test(mid, a, b, c, d)) {
                to = mid;
                newMid = midpoint.apply(from, to);
                if (Objects.equals(mid, newMid)) {
                    return from;
                }
            } else {
                from = mid;
                newMid = midpoint.apply(from, to);
                if (Objects.equals(mid, newMid)) {
                    return to;
                }
            }
            mid = newMid;
        }
    }

    private interface LongIdxTestFunction<A, B, C, D> {
        boolean test(long idx, A a, B b, C c, D d);
    }

    private static <A, B, C, D> long searchLong(long from, long to, A a, B b, C c, D d, LongBinaryOperator midpoint,
            LongIdxTestFunction<A, B, C, D> test) {
        long mid = midpoint.applyAsLong(from, to);
        long newMid;
        for (;;) {
            if (test.test(mid, a, b, c, d)) {
                to = mid;
                newMid = midpoint.applyAsLong(from, to);
                if (mid == newMid) {
                    return from;
                }
            } else {
                from = mid;
                newMid = midpoint.applyAsLong(from, to);
                if (mid == newMid) {
                    return to;
                }
            }
            mid = newMid;
        }
    }

    private interface IntIdxTestFunction<A, B, C, D> {
        boolean test(int idx, A a, B b, C c, D d);
    }

    private static <A, B, C, D> int searchInt(int from, int to, A a, B b, C c, D d, IntBinaryOperator midpoint,
            IntIdxTestFunction<A, B, C, D> test) {
        int mid = midpoint.applyAsInt(from, to);
        int newMid;
        for (;;) {
            if (test.test(mid, a, b, c, d)) {
                to = mid;
                newMid = midpoint.applyAsInt(from, to);
                if (mid == newMid) {
                    return from;
                }
            } else {
                from = mid;
                newMid = midpoint.applyAsInt(from, to);
                if (mid == newMid) {
                    return to;
                }
            }
            mid = newMid;
        }
    }
}
