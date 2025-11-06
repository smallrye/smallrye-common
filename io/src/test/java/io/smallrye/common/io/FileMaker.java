package io.smallrye.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * A simple test file maker.
 */
public class FileMaker {
    private final Path directory;

    private FileMaker(Path directory) {
        this.directory = directory;
    }

    public FileMaker dir(String name, Consumer<FileMaker> action) {
        FileMaker.of(directory.resolve(name), action);
        return this;
    }

    public FileMaker file(String name, int size) {
        byte[] bytes = new byte[Math.min(size, 1024)];
        int cnt = 0;
        try (OutputStream outputStream = Files.newOutputStream(directory.resolve(name))) {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            while (cnt < size) {
                tlr.nextBytes(bytes);
                outputStream.write(bytes, 0, Math.min(size - cnt, bytes.length));
                cnt += bytes.length;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public FileMaker symlink(String name, String target) {
        try {
            Files.createSymbolicLink(directory.resolve(name), Path.of(target));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public static void of(Path path, Consumer<FileMaker> action) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        action.accept(new FileMaker(path));
    }
}
