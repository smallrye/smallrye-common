package io.smallrye.common.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

/**
 * A processor of lines for error handling.
 * Line endings are cleaned up and lines are restricted to a maximum length.
 */
final class LineProcessor {
    private final BufferedReader reader;
    private final int lengthLimit;
    private final Consumer<Object> consumer;

    static final Object EOF = new Object();

    public LineProcessor(final Reader reader, final int lengthLimit, final Consumer<Object> consumer) {
        this.reader = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        this.lengthLimit = lengthLimit;
        this.consumer = consumer;
    }

    public void run() throws IOException {
        final StringBuilder sb = new StringBuilder(Math.min(lengthLimit, 128));
        int charCnt = 0;
        boolean skipping = false;
        boolean gotCr = false;
        for (;;) {
            int c;
            try {
                c = reader.read();
            } catch (IOException e) {
                sb.setLength(0);
                consumer.accept(e);
                throw e;
            }
            switch (c) {
                case -1 -> {
                    if (!sb.isEmpty()) {
                        consumer.accept(sb.toString());
                        sb.setLength(0);
                    }
                    consumer.accept(EOF);
                    return;
                }
                case '\r' -> {
                    gotCr = true;
                    consumer.accept(sb.toString());
                    sb.setLength(0);
                    skipping = false;
                    charCnt = 0;
                }
                case '\n' -> {
                    if (gotCr) {
                        gotCr = false;
                        continue;
                    }
                    consumer.accept(sb.toString());
                    sb.setLength(0);
                    skipping = false;
                    charCnt = 0;
                }
                default -> {
                    gotCr = false;
                    if (!skipping) {
                        if (charCnt == lengthLimit) {
                            skipping = true;
                            sb.append('…');
                        } else {
                            charCnt++;
                            sb.append((char) c);
                        }
                    }
                }
            }
        }
    }
}
