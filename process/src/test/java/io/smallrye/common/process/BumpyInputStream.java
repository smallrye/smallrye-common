package io.smallrye.common.process;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class BumpyInputStream extends FilterInputStream {
    BumpyInputStream(final InputStream in) {
        super(in);
    }

    public int read() throws IOException {
        bump();
        return super.read();
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        bump();
        return super.read(b, off, len);
    }

    public long skip(final long n) throws IOException {
        bump();
        return super.skip(n);
    }

    private static void bump() {
        try {
            Thread.sleep(2L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
