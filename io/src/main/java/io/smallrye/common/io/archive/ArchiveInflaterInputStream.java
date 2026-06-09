package io.smallrye.common.io.archive;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class ArchiveInflaterInputStream extends InputStream {
    private final ArchiveData archiveData;
    private final Inflater inflater;
    // start of compressed data
    private final long offset;
    // compressed size
    private final long size;
    private final long uncompressedSize;
    // compressed position
    private long position;
    // uncompressed byte count
    private long readTotal;

    ArchiveInflaterInputStream(final ArchiveData archiveData, final Inflater inflater, final long offset, final long size,
            final long uncompressedSize) {
        this.archiveData = archiveData;
        this.inflater = inflater;
        this.offset = offset;
        this.size = size;
        this.uncompressedSize = uncompressedSize;
    }

    public int read() throws IOException {
        byte[] buf = new byte[1];
        int res = read(buf);
        if (res == -1) {
            return -1;
        } else {
            return Byte.toUnsignedInt(buf[0]);
        }
    }

    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (inflater.finished()) {
            return -1;
        }
        if (inflater.needsInput()) {
            if (position == size) {
                throw new EOFException("Truncated stream");
            }
            // give up to 1GB at a time
            long cnt = Math.min(size - position, 0x4000_0000);
            ByteBuffer slice = archiveData.buffer(offset + position, (int) cnt);
            inflater.setInput(slice);
            position += cnt;
        }
        if (inflater.needsDictionary()) {
            throw new IOException("Unexpected dictionary requirement");
        }
        int cnt;
        try {
            cnt = inflater.inflate(b, off, len);
        } catch (DataFormatException e) {
            throw new IOException(e);
        }
        readTotal += cnt;
        return cnt;
    }

    public int available() {
        return (int) Math.min(uncompressedSize - readTotal, Integer.MAX_VALUE);
    }

    public void close() throws IOException {
        inflater.end();
        super.close();
    }
}
