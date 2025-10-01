package io.smallrye.common.io;

import static io.smallrye.common.io.Messages.log;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

final class JDKSpecificDirectoryActions {
    private JDKSpecificDirectoryActions() {
    }

    static Path readLink(SecureDirectoryStream<Path> sds, Path path) throws IOException {
        if (path.isAbsolute()) {
            return Files.readSymbolicLink(path);
        } else {
            // try to find the absolute path of the symlink (slow)
            if (path.getNameCount() > 1) {
                try (SecureDirectoryStream<Path> subSds = sds.newDirectoryStream(path.getName(0))) {
                    return readLink(subSds, path.subpath(1, path.getNameCount()));
                }
            }
            // try to find it...
            try (SecureDirectoryStream<Path> sdsClone = sds.newDirectoryStream(Path.of("."))) {
                for (Path subPath : sdsClone) {
                    if (subPath.getFileName().toString().equals(path.getFileName().toString())) {
                        // found it!
                        BasicFileAttributes rbfa = sdsClone
                                .getFileAttributeView(subPath.getFileName(), BasicFileAttributeView.class,
                                        LinkOption.NOFOLLOW_LINKS)
                                .readAttributes();
                        BasicFileAttributes abfa = Files
                                .getFileAttributeView(subPath, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
                                .readAttributes();
                        if (rbfa.fileKey().equals(abfa.fileKey())) {
                            return Files.readSymbolicLink(subPath);
                        } else {
                            // just fail
                            break;
                        }
                    }
                }
            }
            throw log.secureReadlinkNotSupported();
        }
    }

    static void createSymlink(SecureDirectoryStream<Path> sds, Path path, Path target, FileAttribute<?>... attrs)
            throws IOException {
        if (path.isAbsolute()) {
            Files.createSymbolicLink(path, target, attrs);
        } else {
            // Create the symlink under what we assume is the proper path,
            // then atomically move it into the target directory stream at the desired path.
            Path tempName = tempName(path);
            // first, create a probe file
            Path tempFullPath;
            try (SeekableByteChannel ignored = sds.newByteChannel(tempName,
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
                try (SecureDirectoryStream<Path> sdsClone = sds.newDirectoryStream(Path.of("."))) {
                    Iterator<Path> itr = sdsClone.iterator();
                    Path sdsAbsPath = itr.next().getParent();
                    if (sdsAbsPath == null) {
                        throw log.secureMkdirNotSupported();
                    }
                    tempFullPath = sdsAbsPath.resolve(tempName);
                }
            } catch (Throwable t) {
                try {
                    sds.deleteFile(tempName);
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
            // now that we have the full path, delete the probe file and try to create a directory there.
            sds.deleteFile(tempName);
            Files.createSymbolicLink(tempFullPath, target, attrs);
            try {
                sds.move(tempFullPath, sds, path);
            } catch (Throwable t) {
                try {
                    Files.delete(tempFullPath);
                } catch (IOException t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
        }
    }

    private static Path tempName(final Path path) {
        return Path
                .of(path.getFileName().toString() + ".temp" + Long.toHexString(ThreadLocalRandom.current().nextLong()));
    }

    static void createDirectory(SecureDirectoryStream<Path> sds, Path path, FileAttribute<?>... attrs) throws IOException {
        if (path.isAbsolute()) {
            Files.createDirectory(path, attrs);
        } else {
            // Create the directory under what we assume is the proper path,
            // then atomically move it into the target directory stream at the desired path.
            Path tempName = tempName(path);
            // first, create a probe file
            Path tempFullPath;
            try (SeekableByteChannel ignored = sds.newByteChannel(tempName,
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
                try (SecureDirectoryStream<Path> sdsClone = sds.newDirectoryStream(Path.of("."))) {
                    Iterator<Path> itr = sdsClone.iterator();
                    Path sdsAbsPath = itr.next().getParent();
                    if (sdsAbsPath == null) {
                        throw log.secureMkdirNotSupported();
                    }
                    tempFullPath = sdsAbsPath.resolve(tempName);
                }
            } catch (Throwable t) {
                try {
                    sds.deleteFile(tempName);
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
            // now that we have the full path, delete the probe file and try to create a directory there.
            sds.deleteFile(tempName);
            Files.createDirectory(tempFullPath, attrs);
            try {
                sds.move(tempFullPath, sds, path);
            } catch (Throwable t) {
                try {
                    Files.delete(tempFullPath);
                } catch (IOException t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
        }
    }
}
