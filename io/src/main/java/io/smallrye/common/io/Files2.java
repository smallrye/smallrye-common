package io.smallrye.common.io;

import static io.smallrye.common.constraint.Assert.checkNotNullParam;
import static io.smallrye.common.io.Messages.log;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.BasicFileAttributeView;

/**
 * Extra utilities for dealing with the filesystem which are missing from {@link Files}.
 */
public final class Files2 {
    private Files2() {
    }

    /**
     * Open a new secure directory stream at the given path.
     * If the link option {@link LinkOption#NOFOLLOW_LINKS} is given, the open will fail with an exception
     * if {@code path} is a symbolic link.
     *
     * @param path the path of the directory to open (must not be {@code null})
     * @param linkOptions the link options (must not be {@code null})
     *
     * @return a secure directory stream for the given path (not {@code null})
     *
     * @throws NotDirectoryException if the target {@code path} is not a directory
     * @throws IOException if another I/O error occurs
     * @throws UnsupportedOperationException if this platform does not support secure directory streams
     *
     * @see SecureDirectoryStream#newDirectoryStream(Object, LinkOption...)
     */
    public static SecureDirectoryStream<Path> newSecureDirectoryStream(Path path, LinkOption... linkOptions)
            throws IOException {
        checkNotNullParam("path", path);
        checkNotNullParam("linkOptions", linkOptions);
        SecureDirectoryStream<Path> base = CWD_SDS;
        if (base == null) {
            throw Messages.log.secureDirectoryNotSupported(path.getFileSystem(), path);
        }
        // this is a back-door way of having NOFOLLOW_LINKS be supported even for absolute paths.
        return base.newDirectoryStream(path, linkOptions);
    }

    /**
     * Attempt to recursively delete the file or directory at the given path,
     * even if secure directory streams are not supported.
     * If the target path is a symbolic link, it will be removed.
     * <em>Warning:</em> this method can potentially delete files outside the intended path
     * if the target platform does not support secure directory iteration.
     *
     * @param path the path to delete (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     *
     * @see #deleteRecursively(Path)
     */
    public static void deleteRecursivelyEvenIfInsecure(Path path) throws IOException {
        checkNotNullParam("path", path);
        SecureDirectoryStream<Path> base = CWD_SDS;
        if (base != null) {
            // secure!
            deleteRecursively(base, path);
        } else {
            deleteRecursivelyInsecurely(path);
        }
    }

    /**
     * Attempt to recursively delete all of the files returned by the given directory stream,
     * even if secure directory streams are not supported.
     * If any of the target paths are symbolic links, they will be removed.
     * <em>Warning:</em> this method can potentially delete files outside the intended path
     * if the target platform does not support secure directory iteration.
     * <p>
     * The directory stream is not closed by this operation.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param ds the directory stream whose contents should be removed (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     *
     * @see #deleteRecursively(SecureDirectoryStream)
     */
    public static void deleteRecursivelyEvenIfInsecure(DirectoryStream<Path> ds) throws IOException {
        checkNotNullParam("ds", ds);
        if (ds instanceof SecureDirectoryStream<Path> sds) {
            deleteRecursively(sds);
        } else {
            deleteRecursivelyInsecurely(ds);
        }
    }

    /**
     * Attempt to recursively delete the file or directory at the given path.
     * If the target path is a symbolic link, it will be removed.
     *
     * @param path the file or directory to be removed (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     * @throws UnsupportedOperationException if secure directory removal is unsupported
     */
    public static void deleteRecursively(Path path) throws IOException {
        checkNotNullParam("path", path);
        SecureDirectoryStream<Path> base = CWD_SDS;
        if (base == null) {
            throw Messages.log.secureDirectoryNotSupported(path.getFileSystem(), path);
        }
        deleteRecursively(base, path);
    }

    /**
     * Attempt to recursively delete all of the files returned by the given directory stream.
     * If any of the target paths are symbolic links, they will be removed.
     * <p>
     * The directory stream is not closed by this operation,
     * but its iterator is consumed.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param sds the directory stream whose contents should be removed (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     */
    public static void deleteRecursively(SecureDirectoryStream<Path> sds) throws IOException {
        checkNotNullParam("sds", sds);
        for (Path abs : sds) {
            deleteRecursively(sds, abs.getFileName());
        }
    }

    /**
     * Attempt to recursively delete the file or directory at the given directory and path.
     * If the target path is a symbolic link, it will be removed.
     * The target {@code path} must be a relative path.
     * The caller should ensure that the given path is sanitized if needed (see {@link Path#normalize()}).
     * <p>
     * The directory stream is not closed by this operation,
     * and its iterator is not consumed.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param sds the directory stream containing the file (must not be {@code null})
     * @param path the relative path of the file or directory to be removed (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     * @throws IllegalArgumentException if the given path is absolute
     */
    public static void deleteRecursively(SecureDirectoryStream<Path> sds, Path path) throws IOException {
        log.tracef("Securely deleting %s", path);
        checkNotNullParam("sds", sds);
        checkNotNullParam("path", path);
        if (path.isAbsolute()) {
            throw log.notRelative(path);
        }
        if (sds.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes()
                .isDirectory()) {
            try (SecureDirectoryStream<Path> subStream = sds.newDirectoryStream(path, LinkOption.NOFOLLOW_LINKS)) {
                log.tracef("Entering directory %s", path);
                deleteRecursively(subStream);
                log.tracef("Exiting directory %s", path);
            }
            sds.deleteDirectory(path);
        } else {
            sds.deleteFile(path);
        }
    }

    /**
     * {@return the parent of the given path, even if it is relative, or {@code null} if the path has no parent}
     * Relative paths are resolved relative to the {@linkplain #currentDirectory() current directory}.
     *
     * @param path the path to examine (must not be {@code null})
     * @see Path#getParent()
     * @see #currentDirectory()
     */
    public static Path getParent(Path path) {
        checkNotNullParam("path", path);
        if (!path.isAbsolute()) {
            path = currentDirectory().resolve(path);
        }
        return path.normalize().getParent();
    }

    /**
     * {@return the current working directory path at the time that this program was started (not {@code null})}
     * This path comes from the {@code user.dir} system property.
     */
    public static Path currentDirectory() {
        return CWD;
    }

    /**
     * {@return {@code true} if this platform has secure directories, or {@code false} if it does not}
     * Some operating systems or JVM versions do not support secure directories.
     */
    public static boolean hasSecureDirectories() {
        return CWD_SDS != null;
    }

    // -- private --

    private static final Path CWD = Path.of(System.getProperty("user.dir", ".")).normalize().toAbsolutePath();
    private static final SecureDirectoryStream<Path> CWD_SDS;

    static {
        SecureDirectoryStream<Path> cwdSds;
        try {
            DirectoryStream<Path> ds = Files.newDirectoryStream(CWD);
            if (ds instanceof SecureDirectoryStream<Path> sds) {
                cwdSds = sds;
            } else {
                try {
                    ds.close();
                } catch (IOException ignored) {
                }
                cwdSds = null;
            }
        } catch (IOException ignored) {
            cwdSds = null;
        }
        CWD_SDS = cwdSds;
    }

    private static void deleteRecursivelyInsecurely(final DirectoryStream<Path> ds) throws IOException {
        for (Path path : ds) {
            deleteRecursivelyEvenIfInsecure(path);
        }
    }

    private static void deleteRecursivelyInsecurely(final Path path) throws IOException {
        log.tracef("Insecurely deleting path %s", path);
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                deleteRecursivelyInsecurely(ds);
            }
        }
        Files.delete(path);
    }

}
