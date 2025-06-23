package io.smallrye.common.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import io.smallrye.common.function.ExceptionConsumer;

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

    static void consumeToReader(InputStream is, ExceptionConsumer<BufferedReader, IOException> consumer, Charset charset)
            throws IOException {
        try (InputStreamReader isr = new InputStreamReader(is, charset)) {
            try (BufferedReader br = new BufferedReader(isr)) {
                consumer.accept(br);
            }
        }
    }
}
