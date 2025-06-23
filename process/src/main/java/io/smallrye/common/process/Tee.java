package io.smallrye.common.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Stream;

/**
 * A tee which splits an input stream into many streams without extra copying.
 */
final class Tee {
    public static final int SIZE = 512;
    private final List<TeeInputStream> outputs;
    private final ArrayBlockingQueue<Buf> free;

    Tee(final int outputCnt) {
        this.outputs = Stream.generate(TeeInputStream::new).limit(outputCnt).toList();
        this.free = new ArrayBlockingQueue<>(8);
        Stream.generate(Buf::new).limit(8).forEach(free::add);
    }

    List<TeeInputStream> outputs() {
        return outputs;
    }

    void run(InputStream input) {
        Buf buf;
        int res;
        for (;;) {
            for (;;) {
                try {
                    buf = free.take();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
            try {
                res = input.read(buf.data());
            } catch (IOException e) {
                for (TeeInputStream output : outputs) {
                    output.offer(e);
                }
                return;
            }
            if (res == -1) {
                for (TeeInputStream output : outputs) {
                    output.offerEof();
                }
                return;
            } else {
                buf.limit(res);
                buf.initRefCnt(1);
                for (TeeInputStream output : outputs) {
                    // acquires
                    output.offer(buf);
                }
                // our initial ref count
                buf.release();
            }
        }
    }

    final class Buf {
        private static final VarHandle refCntHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "refCnt",
                VarHandle.class, Buf.class, int.class);

        private final byte[] data = new byte[SIZE];
        private int limit;
        @SuppressWarnings("unused") // handle
        private volatile int refCnt;

        byte[] data() {
            return data;
        }

        int limit() {
            return limit;
        }

        void limit(int limit) {
            this.limit = limit;
        }

        void acquire() {
            refCntHandle.getAndAdd(this, 1);
        }

        void release() {
            if ((int) refCntHandle.getAndAdd(this, -1) == 1) {
                free.add(this);
            }
        }

        void initRefCnt(int cnt) {
            refCntHandle.set(this, cnt);
        }

        int get(int offset, byte[] data, int off, int len) {
            int cnt = Math.min(len, limit - offset);
            if (cnt > 0) {
                System.arraycopy(this.data, offset, data, off, cnt);
            }
            return cnt;
        }

        int get(final int offset) {
            return Byte.toUnsignedInt(data[offset]);
        }
    }

    class TeeInputStream extends InputStream {
        private final ArrayDeque<Buf> q = new ArrayDeque<>(8);
        private Buf buf;
        private int offset;
        private IOException error;
        private boolean eof;
        private boolean closed;

        TeeInputStream() {
        }

        private void check() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        void offerEof() {
            synchronized (this) {
                if (!eof) {
                    eof = true;
                    notify();
                }
            }
        }

        void offer(IOException e) {
            synchronized (this) {
                error = e;
                notify();
            }
        }

        boolean offer(Buf buf) {
            synchronized (this) {
                if (closed || eof) {
                    return false;
                }
                buf.acquire();
                q.add(buf);
                notify();
                return true;
            }
        }

        private boolean fill() throws IOException {
            check();
            assert Thread.holdsLock(this);
            if (buf != null) {
                return true;
            }
            for (;;) {
                IOException error = this.error;
                if (error != null) {
                    this.error = null;
                    eof = true;
                    throw new IOException(error);
                }
                buf = q.poll();
                if (buf != null) {
                    offset = 0;
                    return true;
                }
                if (eof) {
                    return false;
                }
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
            }
        }

        public int read() throws IOException {
            synchronized (this) {
                if (!fill()) {
                    return -1;
                }
                int res = buf.get(offset++);
                if (offset == buf.limit()) {
                    buf.release();
                    buf = null;
                }
                return res;
            }
        }

        public int read(final byte[] b, final int off, final int len) throws IOException {
            synchronized (this) {
                while (fill()) {
                    int cnt = buf.get(offset, b, off, len);
                    offset += cnt;
                    if (offset == buf.limit()) {
                        buf.release();
                        buf = null;
                    }
                    if (cnt == 0) {
                        continue;
                    }
                    return cnt;
                }
                return -1;
            }
        }

        public long skip(final long n) throws IOException {
            long cnt = 0;
            synchronized (this) {
                while (cnt < n) {
                    if (!fill()) {
                        return cnt;
                    }
                    int rem = buf.limit() - offset;
                    if (cnt + rem <= n) {
                        cnt += rem;
                        buf.release();
                        buf = null;
                    } else {
                        offset += (int) (n - cnt);
                        break;
                    }
                }
                return n;
            }
        }

        public int available() throws IOException {
            synchronized (this) {
                check();
                return buf == null ? 0 : buf.limit() - offset;
            }
        }

        public void close() {
            synchronized (this) {
                q.clear();
                eof = true;
                closed = true;
            }
        }

        public long transferTo(final OutputStream out) throws IOException {
            long cnt = 0;
            synchronized (this) {
                for (;;) {
                    if (fill()) {
                        int bytes = buf.limit() - offset;
                        out.write(buf.data(), offset, bytes);
                        offset += bytes;
                        cnt += bytes;
                        if (offset == buf.limit()) {
                            buf.release();
                            buf = null;
                        }
                    } else {
                        return cnt;
                    }
                }
            }
        }

    }
}
