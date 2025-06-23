package io.smallrye.common.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

public class TeeTests {
    static final String testString;

    static {
        // large enough to force multiple input frames
        byte[] bytes = new byte[128 * 1024];
        ThreadLocalRandom.current().nextBytes(bytes);
        testString = Base64.getEncoder().encodeToString(bytes);
        assert !testString.contains("\n");
    }

    @Test
    public void testTee() {
        ByteArrayInputStream is = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
        Tee tee = new Tee(4);

        try (var ignored = AutoThread.start(doIt(tee.outputs().get(0)))) {
            try (var ignored1 = AutoThread.start(doIt(tee.outputs().get(1)))) {
                try (var ignored2 = AutoThread.start(doIt(tee.outputs().get(2)))) {
                    try (var ignored3 = AutoThread.start(doIt(tee.outputs().get(3)))) {
                        tee.run(is);
                    }
                }
            }
        }
    }

    private Runnable doIt(Tee.TeeInputStream tis) {
        return () -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new BumpyInputStream(tis)), 97)) {
                assertEquals(testString, br.readLine());
                assertNull(br.readLine());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static class AutoThread extends Thread implements AutoCloseable {
        Throwable t;

        AutoThread(final Runnable task) {
            super(task);
        }

        static AutoThread start(Runnable task) {
            AutoThread at = new AutoThread(task);
            at.start();
            return at;
        }

        public void run() {
            try {
                super.run();
            } catch (RuntimeException | Error t) {
                this.t = t;
            }
        }

        public void close() {
            for (;;) {
                try {
                    join();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
            if (t != null) {
                try {
                    throw t;
                } catch (Error | RuntimeException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new UndeclaredThrowableException(e);
                } finally {
                    t = null;
                }
            }
        }
    }
}
