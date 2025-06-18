package io.smallrye.common.process;

import java.io.IOException;
import java.io.Reader;

/**
 * Useful I/O utilities for internal usage.
 */
final class IOUtil {
    private IOUtil() {
    }

    static void drain(Reader reader) throws IOException {
        int ch;
        for (;;) {
            long res = reader.skip(Integer.MAX_VALUE);
            if (res == 0) {
                ch = reader.read();
                if (ch == -1) {
                    return;
                }
            }
        }
    }
}
