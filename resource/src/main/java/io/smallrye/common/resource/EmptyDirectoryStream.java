package io.smallrye.common.resource;

import java.nio.file.DirectoryStream;
import java.util.Collections;
import java.util.Iterator;

/**
 * An empty directory stream.
 *
 * @param <T> the entry type
 */
public final class EmptyDirectoryStream<T> implements DirectoryStream<T> {
    private static final EmptyDirectoryStream<Object> INSTANCE = new EmptyDirectoryStream<>();

    private EmptyDirectoryStream() {
    }

    /**
     * {@return the singleton instance}
     *
     * @param <T> the entry type
     */
    @SuppressWarnings("unchecked")
    public static <T> EmptyDirectoryStream<T> instance() {
        return (EmptyDirectoryStream<T>) INSTANCE;
    }

    public Iterator<T> iterator() {
        return Collections.emptyIterator();
    }

    public void close() {
    }
}
