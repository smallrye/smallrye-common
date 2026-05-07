package io.smallrye.common.io;

import java.nio.file.OpenOption;

import io.smallrye.common.constraint.Assert;

/**
 * An {@link OpenOption} that specifies the buffer size to use when opening something which supports buffering.
 * <p>
 * Instances are obtained via the {@link #of(int)} or {@link #ofDefault()} factory methods.
 * The default buffer size is {@link #DEFAULT_BUFFER_SIZE} bytes.
 */
public final class BufferSizeOption implements OpenOption {

    /**
     * The smallest allowed buffer size.
     */
    public static final int MIN_BUFFER_SIZE = minBufferSize();

    /**
     * {@return the minimum buffer size (as a method to prevent javac inlining of the value)}
     */
    private static int minBufferSize() {
        return 8;
    }

    /**
     * The default buffer size.
     */
    public static final int DEFAULT_BUFFER_SIZE = defaultBufferSize();

    /**
     * {@return the default buffer size (as a method to prevent javac inlining of the value)}
     */
    private static int defaultBufferSize() {
        return 8192;
    }

    private static final BufferSizeOption DEFAULT = new BufferSizeOption(DEFAULT_BUFFER_SIZE);

    private final int bufferSize;

    private BufferSizeOption(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * {@return an option specifying the given buffer size}
     * If the given buffer size is less than {@linkplain #MIN_BUFFER_SIZE the minimum size},
     * it will be rounded up to that size.
     *
     * @param bufferSize the buffer size in bytes (must be at least 1)
     */
    public static BufferSizeOption of(int bufferSize) {
        Assert.checkMinimumParameter("bufferSize", 1, bufferSize);
        if (bufferSize < MIN_BUFFER_SIZE) {
            bufferSize = MIN_BUFFER_SIZE;
        }
        return bufferSize == DEFAULT_BUFFER_SIZE ? DEFAULT : new BufferSizeOption(bufferSize);
    }

    /**
     * {@return an option specifying the default buffer size}
     */
    public static BufferSizeOption ofDefault() {
        return DEFAULT;
    }

    /**
     * {@return the buffer size in bytes}
     */
    public int bufferSize() {
        return bufferSize;
    }

    /**
     * {@return {@code true} if the given object is a {@code BufferSizeOption} with the same buffer size}
     *
     * @param obj the object to compare
     */
    public boolean equals(final Object obj) {
        return obj instanceof BufferSizeOption bso && equals(bso);
    }

    /**
     * {@return {@code true} if the given option has the same buffer size as this one}
     *
     * @param obj the option to compare
     */
    public boolean equals(final BufferSizeOption obj) {
        return obj == this || obj != null && bufferSize == obj.bufferSize;
    }

    public int hashCode() {
        return Integer.hashCode(bufferSize);
    }

    public String toString() {
        return "BufferSizeOption[" + bufferSize + "]";
    }
}
