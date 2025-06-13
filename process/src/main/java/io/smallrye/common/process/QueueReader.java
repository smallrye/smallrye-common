package io.smallrye.common.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A {@code Reader} implementation that reads from a line queue.
 * Intended for use with {@link LineProcessor}.
 */
final class QueueReader extends BufferedReader {
    QueueReader() {
        super(nullReader());
    }

    private final ArrayBlockingQueue<Object> q = new ArrayBlockingQueue<>(16);
    private volatile boolean closed;
    private boolean eof;
    private String current;
    private int offset;

    private void check() throws IOException {
        if (closed) {
            throw new IOException("Reader is closed");
        }
    }

    private void fill() throws IOException {
        if (eof) {
            return;
        }
        Object val;
        try {
            val = q.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Thread interrupted while reading");
        }
        if (val instanceof String s) {
            current = s;
        } else if (val == LineProcessor.EOF) {
            eof = true;
        } else if (val instanceof IOException e) {
            closed = true;
            q.clear();
            throw new IOException(e);
        } else {
            throw new IllegalStateException();
        }
    }

    public int read() throws IOException {
        check();
        if (current == null) {
            fill();
            if (eof) {
                return -1;
            }
        }
        if (current.length() < offset) {
            return current.charAt(offset++);
        } else {
            current = null;
            offset = 0;
            return '\n';
        }
    }

    public int read(final char[] array, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, array.length);
        check();
        for (int i = 0; i < len; i++) {
            int res = read();
            if (res == -1) {
                return i == 0 ? -1 : i;
            }
            array[off + i] = (char) res;
        }
        return len;
    }

    public String readLine() throws IOException {
        check();
        if (current == null) {
            fill();
            if (eof) {
                return null;
            }
        }
        String current = this.current;
        this.current = null;
        return offset == 0 ? current : current.substring(offset);
    }

    public long skip(final long n) throws IOException {
        check();
        long cnt = 0;
        while (cnt < n) {
            if (current == null) {
                fill();
            }
            if (eof) {
                break;
            }
            // add 1 for an imaginary '\n' character
            int amt = current.length() - offset + 1;
            if (n - cnt >= amt) {
                // discard the entire line
                current = null;
                cnt += amt;
            } else {
                offset += amt;
            }
        }
        return cnt;
    }

    public boolean ready() throws IOException {
        check();
        return eof || current != null;
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(final int readAheadLimit) throws IOException {
        throw new IOException("mark() not supported");
    }

    public void reset() throws IOException {
        throw new IOException("reset() not supported");
    }

    public void close() {
        closed = true;
        q.clear();
    }

    void handleLine(Object obj) {
        if (!closed) {
            q.add(obj);
            if (closed) {
                q.clear();
            }
        }
    }
}
