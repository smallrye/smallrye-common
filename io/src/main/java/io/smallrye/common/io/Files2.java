package io.smallrye.common.io;

import static io.smallrye.common.constraint.Assert.checkNotNullParam;
import static io.smallrye.common.io.Messages.log;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
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
     * If the target path or any nested path is a symbolic link, it will be removed and will not be
     * treated as a directory.
     * If the target path is absolute, then {@code sds} is ignored and the absolute path is removed.
     * The caller should ensure that the given path is sanitized if needed (see {@link Path#normalize()}
     * and {@link Path#isAbsolute()}).
     * <p>
     * The directory stream is not closed by this operation,
     * and its iterator is not consumed.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param sds the directory stream containing the file (must not be {@code null})
     * @param path the relative path of the file or directory to be removed (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     */
    public static void deleteRecursively(SecureDirectoryStream<Path> sds, Path path) throws IOException {
        log.tracef("Securely deleting %s", path);
        checkNotNullParam("sds", sds);
        checkNotNullParam("path", path);
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
     * Attempt to recursively delete the file or directory at the given path,
     * even if secure directory streams are not supported.
     * I/O errors will not be reported.
     * If the target path is a symbolic link, it will be removed.
     * <em>Warning:</em> this method can potentially delete files outside the intended path
     * if the target platform does not support secure directory iteration.
     *
     * @param path the path to delete (must not be {@code null})
     * @return the deletion statistics (not {@code null})
     *
     * @see #deleteRecursivelyQuietly(Path)
     */
    public static DeleteStats deleteRecursivelyQuietlyEvenIfInsecure(Path path) {
        checkNotNullParam("path", path);
        SecureDirectoryStream<Path> base = CWD_SDS;
        if (base != null) {
            // secure!
            return deleteRecursivelyQuietly(base, path);
        } else {
            long[] stats = new long[DeleteStats.STATS_SIZE];
            deleteRecursivelyQuietlyInsecurely(path, stats);
            return new DeleteStats(stats);
        }
    }

    /**
     * Attempt to recursively delete all of the files returned by the given directory stream,
     * even if secure directory streams are not supported.
     * I/O errors will not be reported.
     * If any of the target paths are symbolic links, they will be removed.
     * <em>Warning:</em> this method can potentially delete files outside the intended path
     * if the target platform does not support secure directory iteration.
     * <p>
     * The directory stream is not closed by this operation.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param ds the directory stream whose contents should be removed (must not be {@code null})
     * @return the deletion statistics (not {@code null})
     *
     * @see #deleteRecursivelyQuietly(SecureDirectoryStream)
     */
    public static DeleteStats deleteRecursivelyQuietlyEvenIfInsecure(DirectoryStream<Path> ds) {
        checkNotNullParam("ds", ds);
        if (ds instanceof SecureDirectoryStream<Path> sds) {
            return deleteRecursivelyQuietly(sds);
        } else {
            long[] stats = new long[DeleteStats.STATS_SIZE];
            deleteRecursivelyQuietlyInsecurely(ds, stats);
            return new DeleteStats(stats);
        }
    }

    /**
     * Attempt to recursively delete the file or directory at the given path.
     * If the target path is a symbolic link, it will be removed.
     * I/O errors will not be reported.
     *
     * @param path the file or directory to be removed (must not be {@code null})
     * @return the deletion statistics (not {@code null})
     * @throws UnsupportedOperationException if secure directory removal is unsupported
     */
    public static DeleteStats deleteRecursivelyQuietly(Path path) {
        checkNotNullParam("path", path);
        SecureDirectoryStream<Path> base = CWD_SDS;
        if (base == null) {
            throw Messages.log.secureDirectoryNotSupported(path.getFileSystem(), path);
        }
        return deleteRecursivelyQuietly(base, path);
    }

    /**
     * Attempt to recursively delete all of the files returned by the given directory stream.
     * If any of the target paths are symbolic links, they will be removed.
     * I/O errors will not be reported.
     * <p>
     * The directory stream is not closed by this operation,
     * but its iterator is consumed.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param sds the directory stream whose contents should be removed (must not be {@code null})
     * @return the deletion statistics (not {@code null})
     */
    public static DeleteStats deleteRecursivelyQuietly(SecureDirectoryStream<Path> sds) {
        checkNotNullParam("sds", sds);
        long[] stats = new long[DeleteStats.STATS_SIZE];
        deleteRecursivelyQuietly(sds, stats);
        return new DeleteStats(stats);
    }

    /**
     * Attempt to recursively delete the file or directory at the given directory and path.
     * If the target path or any nested path is a symbolic link, it will be removed and will not be
     * treated as a directory.
     * If the target path is absolute, then {@code sds} is ignored and the absolute path is removed.
     * I/O errors will not be reported.
     * The caller should ensure that the given path is sanitized if needed (see {@link Path#normalize()}
     * and {@link Path#isAbsolute()}).
     * <p>
     * The directory stream is not closed by this operation,
     * and its iterator is not consumed.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param sds the directory stream containing the file (must not be {@code null})
     * @param path the relative path of the file or directory to be removed (must not be {@code null})
     * @return the deletion statistics (not {@code null})
     */
    public static DeleteStats deleteRecursivelyQuietly(SecureDirectoryStream<Path> sds, Path path) {
        checkNotNullParam("sds", sds);
        checkNotNullParam("path", path);
        long[] stats = new long[DeleteStats.STATS_SIZE];
        deleteRecursivelyQuietly(sds, path, stats);
        return new DeleteStats(stats);
    }

    /**
     * Attempt to recursively clean the file or directory at the given path,
     * even if secure directory streams are not supported.
     * If the target path or any nested path is not a directory, it will be removed.
     * Directories will be preserved.
     * <em>Warning:</em> this method can potentially delete files outside the intended path
     * if the target platform does not support secure directory iteration.
     *
     * @param path the path to clean (must not be {@code null})
     * @throws IOException if one or more files fails to be cleaned or another I/O error occurs
     *
     * @see #cleanRecursively(Path)
     */
    public static void cleanRecursivelyEvenIfInsecure(Path path) throws IOException {
        checkNotNullParam("path", path);
        SecureDirectoryStream<Path> base = CWD_SDS;
        if (base != null) {
            // secure!
            cleanRecursively(base, path);
        } else {
            cleanRecursivelyInsecurely(path);
        }
    }

    /**
     * Attempt to recursively clean all of the files and directories from the given stream,
     * even if secure directory streams are not supported.
     * If any nested path is not a directory, it will be removed.
     * Directories will be preserved.
     * <em>Warning:</em> this method can potentially delete files outside the intended path
     * if the target platform does not support secure directory iteration.
     * <p>
     * The directory stream is not closed by this operation.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param ds the directory stream whose contents should be cleaned (must not be {@code null})
     * @throws IOException if one or more files fails to be cleaned or another I/O error occurs
     *
     * @see #cleanRecursively(Path)
     */
    public static void cleanRecursivelyEvenIfInsecure(DirectoryStream<Path> ds) throws IOException {
        checkNotNullParam("ds", ds);
        if (ds instanceof SecureDirectoryStream<Path> sds) {
            cleanRecursively(sds);
        } else {
            cleanRecursivelyInsecurely(ds);
        }
    }

    /**
     * Attempt to recursively clean the file or directory at the given path.
     * If the target path is not a directory, it will be removed.
     *
     * @param path the file or directory to be cleaned (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     * @throws UnsupportedOperationException if secure directory traversal is unsupported
     */
    public static void cleanRecursively(Path path) throws IOException {
        checkNotNullParam("path", path);
        SecureDirectoryStream<Path> base = CWD_SDS;
        if (base == null) {
            throw Messages.log.secureDirectoryNotSupported(path.getFileSystem(), path);
        }
        cleanRecursively(base, path);
    }

    /**
     * Attempt to recursively clean all of the files returned by the given directory stream.
     * If any of the target paths are not directories, they will be removed.
     * <p>
     * The directory stream is not closed by this operation,
     * but its iterator is consumed.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param sds the directory stream whose contents should be cleaned (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     */
    public static void cleanRecursively(SecureDirectoryStream<Path> sds) throws IOException {
        checkNotNullParam("sds", sds);
        for (Path abs : sds) {
            cleanRecursively(sds, abs.getFileName());
        }
    }

    /**
     * Attempt to recursively clean the file or directory at the given directory and path.
     * If the target path or any nested path is a symbolic link, it will be removed and will not be
     * treated as a directory.
     * If any of the target paths are not directories, they will be removed.
     * If the target path is absolute, then {@code sds} is ignored and the absolute path is cleaned.
     * The caller should ensure that the given path is sanitized if needed (see {@link Path#normalize()}
     * and {@link Path#isAbsolute()}).
     * <p>
     * The directory stream is not closed by this operation,
     * and its iterator is not consumed.
     * The caller should ensure that the stream is closed at the appropriate time.
     *
     * @param sds the directory stream containing the file (must not be {@code null})
     * @param path the relative path of the file or directory to be cleaned (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     */
    public static void cleanRecursively(SecureDirectoryStream<Path> sds, Path path) throws IOException {
        checkNotNullParam("sds", sds);
        checkNotNullParam("path", path);
        log.tracef("Cleaning %s", path);
        if (sds.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes()
                .isDirectory()) {
            try (SecureDirectoryStream<Path> subStream = sds.newDirectoryStream(path, LinkOption.NOFOLLOW_LINKS)) {
                log.tracef("Entering directory %s", path);
                cleanRecursively(subStream);
                log.tracef("Exiting directory %s", path);
            }
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
                deleteRecursivelyEvenIfInsecure(ds);
            }
        }
        Files.delete(path);
    }

    private static void deleteRecursivelyQuietly(final SecureDirectoryStream<Path> sds, final Path path, final long[] stats) {
        boolean isDirectory;
        try {
            isDirectory = sds.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
                    .readAttributes().isDirectory();
        } catch (NoSuchFileException ignored) {
            // not found at all, so don't count it
            return;
        } catch (IOException ignored) {
            // no idea what this thing was, just count it as a file
            isDirectory = false;
        }
        if (isDirectory) {
            stats[DeleteStats.DIR_FOUND]++;
            try (SecureDirectoryStream<Path> subStream = sds.newDirectoryStream(path, LinkOption.NOFOLLOW_LINKS)) {
                log.tracef("Entering directory %s", path);
                deleteRecursivelyQuietly(subStream, stats);
                log.tracef("Exiting directory %s", path);
            } catch (IOException ignored) {
            }
            try {
                sds.deleteDirectory(path);
                stats[DeleteStats.DIR_REMOVED]++;
            } catch (IOException ignored) {
            }
        } else {
            stats[DeleteStats.FILE_FOUND]++;
            try {
                sds.deleteFile(path);
                stats[DeleteStats.FILE_REMOVED]++;
            } catch (NoSuchFileException ignored) {
                // correct the count for non-existent file
                stats[DeleteStats.FILE_FOUND]--;
            } catch (IOException ignored) {
            }
        }
    }

    private static void deleteRecursivelyQuietly(final SecureDirectoryStream<Path> subStream, final long[] stats) {
        for (Path path : subStream) {
            deleteRecursivelyQuietly(subStream, path.getFileName(), stats);
        }
    }

    private static void deleteRecursivelyQuietlyInsecurely(final Path path, final long[] stats) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            stats[DeleteStats.DIR_FOUND]++;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                deleteRecursivelyQuietlyInsecurely(ds, stats);
            } catch (IOException ignored) {
            }
            try {
                Files.delete(path);
                stats[DeleteStats.DIR_REMOVED]++;
            } catch (IOException ignored) {
            }
        } else {
            stats[DeleteStats.FILE_FOUND]++;
            try {
                Files.delete(path);
                stats[DeleteStats.FILE_REMOVED]++;
            } catch (NoSuchFileException ignored) {
                // correct the count for non-existent file
                stats[DeleteStats.FILE_FOUND]--;
            } catch (IOException ignored) {
            }
        }
    }

    private static void deleteRecursivelyQuietlyInsecurely(final DirectoryStream<Path> ds, final long[] stats) {
        for (Path path : ds) {
            deleteRecursivelyQuietlyInsecurely(path, stats);
        }
    }

    private static void cleanRecursivelyInsecurely(final DirectoryStream<Path> ds) throws IOException {
        for (Path path : ds) {
            cleanRecursivelyEvenIfInsecure(path);
        }
    }

    private static void cleanRecursivelyInsecurely(final Path path) throws IOException {
        log.tracef("Insecurely cleaning path %s", path);
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                cleanRecursivelyEvenIfInsecure(ds);
            }
        } else {
            Files.delete(path);
        }
    }

}
