package io.smallrye.common.io;

import static io.smallrye.common.io.Messages.log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.FileAttribute;

final class JDKSpecificDirectoryActions {
    private JDKSpecificDirectoryActions() {
    }

    static Path readLink(SecureDirectoryStream<Path> sds, Path path) throws IOException {
        if (path.isAbsolute()) {
            return Files.readSymbolicLink(path);
        } else {
            throw log.secureReadlinkNotSupported();
        }
    }

    static void createSymlink(SecureDirectoryStream<Path> sds, Path path, Path target, FileAttribute<?>... attrs)
            throws IOException {
        if (path.isAbsolute()) {
            Files.createSymbolicLink(path, target, attrs);
        } else {
            throw log.secureSymlinkNotSupported();
        }
    }

    static void createDirectory(SecureDirectoryStream<Path> sds, Path path, FileAttribute<?>... attrs) throws IOException {
        if (path.isAbsolute()) {
            Files.createDirectory(path, attrs);
        } else {
            throw log.secureMkdirNotSupported();
        }
    }
}
