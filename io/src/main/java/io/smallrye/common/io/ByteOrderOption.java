package io.smallrye.common.io;

import java.nio.ByteOrder;
import java.nio.file.OpenOption;

import io.smallrye.common.constraint.Assert;

/**
 * An {@link OpenOption} that specifies the default byte order to use when opening a {@link BufferedFile}.
 * <p>
 * If no {@code ByteOrderOption} is specified, the {@linkplain ByteOrder#nativeOrder() native byte order} is used.
 * Specifying both {@link #BIG_ENDIAN} and {@link #LITTLE_ENDIAN} in the same set of open options
 * will generally cause an {@link IllegalArgumentException} to be thrown.
 */
public enum ByteOrderOption implements OpenOption {
    /**
     * Use {@linkplain ByteOrder#BIG_ENDIAN big-endian} byte order.
     */
    BIG_ENDIAN(ByteOrder.BIG_ENDIAN),
    /**
     * Use {@linkplain ByteOrder#LITTLE_ENDIAN little-endian} byte order.
     */
    LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN),
    ;

    private final ByteOrder byteOrder;

    ByteOrderOption(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    /**
     * {@return the byte order corresponding to this option}
     */
    public ByteOrder byteOrder() {
        return byteOrder;
    }

    /**
     * {@return the byte order option corresponding to the given byte order}
     *
     * @param byteOrder the byte order (must not be {@code null})
     */
    public static ByteOrderOption of(ByteOrder byteOrder) {
        Assert.checkNotNullParam("byteOrder", byteOrder);
        return byteOrder == ByteOrder.BIG_ENDIAN ? BIG_ENDIAN : LITTLE_ENDIAN;
    }

    /**
     * {@return the byte order option corresponding to the native byte order of the current JVM instance}
     */
    public static ByteOrderOption ofNative() {
        return of(ByteOrder.nativeOrder());
    }
}
