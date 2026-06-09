package io.smallrye.common.io.archive;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class BufferArchiveData extends ArchiveData {
    private final ByteBuffer buffer;

    BufferArchiveData(final ByteBuffer buffer) {
        this.buffer = buffer.slice().order(ByteOrder.LITTLE_ENDIAN);
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
        // no operation (needs MemorySegment)
    }
}
