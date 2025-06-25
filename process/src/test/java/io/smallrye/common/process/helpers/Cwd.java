package io.smallrye.common.process.helpers;

import java.nio.file.Path;

/**
 * Get and return the current working directory file name.
 */
public final class Cwd {
    public static void main(String[] args) {
        System.out.print(Path.of("").toAbsolutePath().getFileName());
    }
}
