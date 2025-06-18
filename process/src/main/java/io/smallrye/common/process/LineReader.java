package io.smallrye.common.process;

import java.io.IOException;
import java.io.Reader;

/**
 * A length-limited line reader.
 * Impl. note: this will perform best with a buffered reader.
 */
final class LineReader {
    private final StringBuilder sb;
    private final Reader r;
    private final int maxLineLength;
    private boolean gotCr;

    LineReader(final Reader r, final int maxLineLength) {
        this.r = r;
        this.maxLineLength = maxLineLength;
        sb = new StringBuilder(Math.min(192, maxLineLength));
    }

    public String readLine() throws IOException {
        int ch;
        for (;;) {
            ch = r.read();
            switch (ch) {
                case -1, '\n', '\r' -> {
                    // end of line
                    switch (ch) {
                        case '\r' -> gotCr = true;
                        case '\n' -> {
                            if (gotCr) {
                                gotCr = false;
                                continue;
                            }
                        }
                        case -1 -> {
                            if (sb.isEmpty()) {
                                return null;
                            }
                        }
                    }
                    try {
                        return sb.toString();
                    } finally {
                        sb.setLength(0);
                    }
                }
                default -> {
                    gotCr = false;
                    if (sb.length() < maxLineLength) {
                        sb.append((char) ch);
                    }
                }
            }
        }
    }
}
