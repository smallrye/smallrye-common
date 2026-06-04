package io.smallrye.common.io.archive;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

final class MappedArchiveData extends ArchiveData {
    private static final VarHandle S64_LE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN).varHandle();
    private static final VarHandle S32_LE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN).varHandle();
    private static final VarHandle S16_LE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN).varHandle();
    private static final VarHandle S8 = ValueLayout.JAVA_BYTE.varHandle();

    private final MemorySegment buffer;

    MappedArchiveData(FileChannel ch) throws IOException {
        this.buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size(), Arena.ofAuto());
    }

    protected byte s8(final long offset) {
        return (byte) S8.get(buffer, offset);
    }

    protected short s16le(final long offset) {
        return (short) S16_LE.get(buffer, offset);
    }

    protected int s32le(final long offset) {
        return (int) S32_LE.get(buffer, offset);
    }

    protected long s64le(final long offset) {
        return (long) S64_LE.get(buffer, offset);
    }

    protected ByteBuffer buffer(final long offset, final int size) {
        return buffer.asSlice(offset, size).asByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }

    protected long size() {
        return buffer.byteSize();
    }

    void get(final long base, final byte[] dest, final int off, final int len) {
        MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, base, dest, off, len);
    }

    void release() {
        buffer.unload();
    }
}
