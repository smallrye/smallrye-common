package io.smallrye.common.serial.impl;

import java.util.function.Function;

import io.smallrye.common.constraint.Assert;

/**
 * A context-scoped analog of {@link ClassValue}.
 * Each {@code ClassLocal} instance represents a per-class computed value that is cached
 * on the serial context rather than on the {@link Class} object itself.
 * This avoids class loader leaks that can occur with {@link ClassValue} when
 * the cached value strongly references a class from a child class loader.
 * <p>
 * Instances of this class are intended to be stored in {@code static final} fields.
 * The computation function is invoked at most once per class per context.
 *
 * @param <T> the type of the cached value
 */
public final class ClassLocal<T> {
    private final Function<Class<?>, T> compute;

    /**
     * Construct a new instance.
     *
     * @param compute the function to compute the value for a given class (must not be {@code null})
     */
    public ClassLocal(final Function<Class<?>, T> compute) {
        this.compute = Assert.checkNotNullParam("compute", compute);
    }

    /**
     * {@return the computation function (not {@code null})}
     */
    public Function<Class<?>, T> compute() {
        return compute;
    }
}
