package io.smallrye.common.ref;

import java.util.EnumSet;

/**
 * An enhanced reference type with a type-safe attachment.
 *
 * @param <T> the reference value type
 * @param <A> the attachment type
 *
 * @see java.lang.ref.Reference
 */
public interface Reference<T, A> {

    /**
     * Get the value, or {@code null} if the reference has been cleared.
     *
     * @return the value
     */
    T get();

    /**
     * Get the attachment, if any.
     *
     * @return the attachment
     */
    A getAttachment();

    /**
     * Clear the reference.
     */
    void clear();

    /**
     * Get the type of the reference.
     *
     * @return the type
     */
    Type getType();

    /**
     * A reference type.
     *
     * @apiviz.exclude
     */
    enum Type {

        /**
         * A strong reference.
         */
        STRONG,
        /**
         * A weak reference.
         */
        WEAK,
        /**
         * A phantom reference.
         */
        PHANTOM,
        /**
         * A soft reference.
         */
        SOFT,
        /**
         * A {@code null} reference.
         */
        NULL,
        ;

        private static final int fullSize = values().length;

        /**
         * Determine whether the given set is fully populated (or "full"), meaning it contains all possible values.
         *
         * @param set the set
         *
         * @return {@code true} if the set is full, {@code false} otherwise
         */
        public static boolean isFull(final EnumSet<Type> set) {
            return set != null && set.size() == fullSize;
        }

        /**
         * Determine whether this instance is equal to one of the given instances.
         *
         * @param v1 the first instance
         *
         * @return {@code true} if one of the instances matches this one, {@code false} otherwise
         */
        public boolean in(final Type v1) {
            return this == v1;
        }

        /**
         * Determine whether this instance is equal to one of the given instances.
         *
         * @param v1 the first instance
         * @param v2 the second instance
         *
         * @return {@code true} if one of the instances matches this one, {@code false} otherwise
         */
        public boolean in(final Type v1, final Type v2) {
            return this == v1 || this == v2;
        }

        /**
         * Determine whether this instance is equal to one of the given instances.
         *
         * @param v1 the first instance
         * @param v2 the second instance
         * @param v3 the third instance
         *
         * @return {@code true} if one of the instances matches this one, {@code false} otherwise
         */
        public boolean in(final Type v1, final Type v2, final Type v3) {
            return this == v1 || this == v2 || this == v3;
        }

        /**
         * Determine whether this instance is equal to one of the given instances.
         *
         * @param values the possible values
         *
         * @return {@code true} if one of the instances matches this one, {@code false} otherwise
         */
        public boolean in(final Type... values) {
            if (values != null)
                for (Type value : values) {
                    if (this == value)
                        return true;
                }
            return false;
        }
    }
}
