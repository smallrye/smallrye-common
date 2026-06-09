package io.smallrye.common.io.archive;

import java.io.IOException;
import java.io.InputStream;

final class ArchiveDataInputStream extends InputStream {
    private final ArchiveData archiveData;
    private final long offset;
    private final long size;
    private long position;
    private long mark = -1;

    ArchiveDataInputStream(final ArchiveData archiveData, final long offset, final long size) {
        this.archiveData = archiveData;
        this.offset = offset;
        this.size = size;
    }

    public int read() {
        if (position == size) {
            return -1;
        }
        return archiveData.u8(offset + position++);
    }

    public int read(final byte[] b) {
        return read(b, 0, b.length);
    }

    public int read(final byte[] b, final int off, final int len) {
        if (position == size) {
            return -1;
        }
        int cnt = (int) Math.min(size - position, len);
        archiveData.get(offset, b, off, cnt);
        position += cnt;
        return cnt;
    }

    public byte[] readNBytes(final int len) {
        int cnt = (int) Math.min(size - position, len);
        byte[] b = new byte[cnt];
        archiveData.get(offset, b, 0, cnt);
        position += cnt;
        return b;
    }

    public long skip(final long n) {
        long cnt = Math.max(0, Math.min(size - position, n));
        position += cnt;
        return cnt;
    }

    public void mark(final int limit) {
        mark = position;
    }

    public int available() {
        return (int) Math.min(size - position, Integer.MAX_VALUE);
    }

    public void reset() throws IOException {
        if (mark == -1) {
            throw new IOException("Mark not set");
        }
        position = mark;
    }

    public boolean markSupported() {
        return true;
    }
}
