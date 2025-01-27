package io.smallrye.common.resource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Iterator;
import java.util.function.Function;

import io.smallrye.common.constraint.Assert;

/**
 * A directory stream to map one kind of entry to another.
 *
 * @param <T> the input entry type
 * @param <R> the output entry type
 */
public final class MappedDirectoryStream<T, R> implements DirectoryStream<R> {
    private final DirectoryStream<T> delegate;
    private final Function<T, R> mappingFunction;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate stream (must not be {@code null})
     * @param mappingFunction the mapping function (must not be {@code null})
     */
    public MappedDirectoryStream(final DirectoryStream<T> delegate, final Function<T, R> mappingFunction) {
        this.delegate = Assert.checkNotNullParam("delegate", delegate);
        this.mappingFunction = Assert.checkNotNullParam("mappingFunction", mappingFunction);
    }

    public Iterator<R> iterator() {
        Iterator<T> itr = delegate.iterator();
        return new Iterator<R>() {
            public boolean hasNext() {
                return itr.hasNext();
            }

            public R next() {
                return mappingFunction.apply(itr.next());
            }
        };
    }

    public void close() throws IOException {
        delegate.close();
    }
}
