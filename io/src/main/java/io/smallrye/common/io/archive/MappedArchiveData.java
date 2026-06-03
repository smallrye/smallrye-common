package io.smallrye.common.io.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

final class MappedArchiveData extends ArchiveData {
    private final FileChannel ch;
    private final MappedByteBuffer buffer;

    MappedArchiveData(FileChannel ch) throws IOException {
        this.ch = ch;
        MappedByteBuffer buf = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        this.buffer = buf;
    }

    protected byte s8(final long offset) {
        return buffer.get((int) offset);
    }

    protected short s16le(final long offset) {
        return buffer.getShort((int) offset);
    }

    protected int s32le(final long offset) {
        return buffer.getInt((int) offset);
    }

    protected long s64le(final long offset) {
        return buffer.getLong((int) offset);
    }

    protected ByteBuffer buffer(final long offset, final int size) {
        return buffer.slice((int) offset, size);
    }

    protected long size() {
        return buffer.capacity();
    }

    void get(final long base, final byte[] dest, final int off, final int len) {
        buffer.get((int) base, dest, off, len);
    }

    void release() {
    }

    void close() throws IOException {
        ch.close();
    }
}
