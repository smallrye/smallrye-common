package io.smallrye.common.io;

import static io.smallrye.common.constraint.Assert.checkNotNullArrayParam;
import static io.smallrye.common.constraint.Assert.checkNotNullParam;
import static io.smallrye.common.constraint.Assert.impossibleSwitchCase;
import static io.smallrye.common.io.Messages.log;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
     * Copy from one channel to another in the most efficient manner supported by the JDK.
     * Neither channel is closed.
     *
     * @param in the input channel (must not be {@code null})
     * @param out the output channel (must not be {@code null})
     * @return the number of bytes copied
     * @throws IOException if copying failed for some reason
     */
    public static long copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
        checkNotNullParam("in", in);
        checkNotNullParam("out", out);
        if (in instanceof FileChannel fc) {
            // zero-copy from source
            long size = fc.size();
            long cnt = 0;
            while (cnt < size) {
                long res = fc.transferTo(cnt, size - cnt, out);
                if (res == 0) {
                    throw log.partialCopy(fc, out, size, cnt);
                }
                cnt += res;
            }
            return cnt;
        } else if (in instanceof SeekableByteChannel sc && out instanceof FileChannel fc) {
            // zero-copy to dest
            long size = sc.size();
            long cnt = 0;
            while (cnt < size) {
                long res = fc.transferFrom(sc, cnt, size - cnt);
                if (res == 0) {
                    throw log.partialCopy(fc, out, size, cnt);
                }
                cnt += res;
            }
            return cnt;
        } else {
            // no zero-copy channel API, so try the zero-copy stream API instead
            return Channels.newInputStream(in).transferTo(Channels.newOutputStream(out));
        }
    }

    /**
     * Equivalent to calling {@link #copy(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)} with no
     * options.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during copy
     *
     * @see #copy(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void copy(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile) throws IOException {
        copyOrMove(sourceDir, srcFile, destDir, destFile, 0);
    }

    /**
     * Equivalent to calling {@link #copy(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)} with one
     * option.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param option1 the copy option to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during copy
     *
     * @see #copy(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void copy(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption option1) throws IOException {
        checkNotNullParam("option1", option1);
        copyOrMove(sourceDir, srcFile, destDir, destFile, parseCopyOption(option1));
    }

    /**
     * Equivalent to calling {@link #copy(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)} with two
     * options.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param option1 the first copy option to use for the operation (must not be {@code null})
     * @param option2 the second copy option to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during copy
     *
     * @see #copy(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void copy(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption option1, CopyOption option2) throws IOException {
        checkNotNullParam("option1", option1);
        checkNotNullParam("option2", option2);
        copyOrMove(sourceDir, srcFile, destDir, destFile, parseCopyOption(option1) | parseCopyOption(option2));
    }

    /**
     * Copy a file as securely as possible from one directory and path to another.
     * If both {@code srcFile} and {@code destFile} are {@linkplain Path#isAbsolute() absolute paths},
     * then this method behaves exactly the same as {@link Files#copy(Path, Path, CopyOption...)}.
     * If {@code srcFile} and {@code destFile} are found to refer to the same file, then no action is taken
     * and this method will return directly.
     * <p>
     * Otherwise, an attempt is made to:
     * <ul>
     * <li>Copy symbolic links as symbolic links (when {@link LinkOption#NOFOLLOW_LINKS} is given)</li>
     * <li>Copy directories as directories (non-recursively)</li>
     * <li>Preserve file attributes (when {@link StandardCopyOption#COPY_ATTRIBUTES} is given)</li>
     * <li>Copy regular files using the most efficient JDK mechanism available</li>
     * </ul>
     * The operation may fail with a {@link IOException} if some part of the operation fails; in this case,
     * the file may be partially copied.
     * <p>
     * If some aspect of the operation is not supported, {@link UnsupportedOperationException} will be thrown.
     * Currently, the currently unsupported operations include:
     * <ul>
     * <li>Copying a symbolic link from or to a relative path (unsupported by the JDK as of JDK 25)</li>
     * <li>Copying a directory into a relative path (unsupported by the JDK as of JDK 25)</li>
     * </ul>
     * The following copy options are allowed:
     * <ul>
     * <li>{@link LinkOption#NOFOLLOW_LINKS}</li>
     * <li>{@link StandardCopyOption#COPY_ATTRIBUTES}</li>
     * <li>{@link StandardCopyOption#REPLACE_EXISTING}</li>
     * </ul>
     * Giving any other option will cause a {@link IllegalArgumentException} to be thrown.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param options the copy options to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during copy
     *
     * @see SecureDirectoryStream#move(Object, SecureDirectoryStream, Object)
     */
    public static void copy(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption... options) throws IOException {
        copyOrMove(sourceDir, srcFile, destDir, destFile, parseCopyOptions(options));
    }

    /**
     * Equivalent to calling {@link #copyRecursively(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)}
     * with no options.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during copy
     *
     * @see #copyRecursively(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void copyRecursively(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile) throws IOException {
        copyOrMove(sourceDir, srcFile, destDir, destFile, OPT_RECURSIVE);
    }

    /**
     * Equivalent to calling {@link #copyRecursively(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)}
     * with one option.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param option1 the copy option to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during copy
     *
     * @see #copyRecursively(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void copyRecursively(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption option1) throws IOException {
        checkNotNullParam("option1", option1);
        copyOrMove(sourceDir, srcFile, destDir, destFile, parseCopyOption(option1) | OPT_RECURSIVE);
    }

    /**
     * Equivalent to calling {@link #copyRecursively(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)}
     * with two options.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param option1 the first copy option to use for the operation (must not be {@code null})
     * @param option2 the second copy option to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during copy
     *
     * @see #copyRecursively(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void copyRecursively(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption option1, CopyOption option2) throws IOException {
        checkNotNullParam("option1", option1);
        checkNotNullParam("option2", option2);
        copyOrMove(sourceDir, srcFile, destDir, destFile, parseCopyOption(option1) | parseCopyOption(option2) | OPT_RECURSIVE);
    }

    /**
     * Recursively copy a file or directory as securely as possible from one directory and path to another.
     * If {@code srcFile} and {@code destFile} are found to refer to the same file, then no action is taken
     * and this method will return directly.
     * <p>
     * Otherwise, an attempt is made to:
     * <ul>
     * <li>Copy symbolic links as symbolic links (when {@link LinkOption#NOFOLLOW_LINKS} is given)</li>
     * <li>Copy directories as directories (recursively)</li>
     * <li>Preserve file attributes (when {@link StandardCopyOption#COPY_ATTRIBUTES} is given)</li>
     * <li>Copy regular files using the most efficient JDK mechanism available</li>
     * </ul>
     * The operation may fail with a {@link IOException} if some part of the operation fails; in this case,
     * the file or directory may be partially copied.
     * <p>
     * If some aspect of the operation is not supported, {@link UnsupportedOperationException} will be thrown.
     * Currently, the currently unsupported operations include:
     * <ul>
     * <li>Copying a symbolic link from or to a relative path (unsupported by the JDK as of JDK 25)</li>
     * <li>Copying a directory into a relative path (unsupported by the JDK as of JDK 25)</li>
     * </ul>
     * The following copy options are allowed:
     * <ul>
     * <li>{@link LinkOption#NOFOLLOW_LINKS}</li>
     * <li>{@link StandardCopyOption#COPY_ATTRIBUTES}</li>
     * <li>{@link StandardCopyOption#REPLACE_EXISTING}</li>
     * </ul>
     * Giving any other option will cause a {@link IllegalArgumentException} to be thrown.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param options the copy options to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during copy
     */
    public static void copyRecursively(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption... options) throws IOException {
        copyOrMove(sourceDir, srcFile, destDir, destFile, parseCopyOptions(options) | OPT_RECURSIVE);
    }

    /**
     * Equivalent to calling {@link #move(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)} with no
     * options.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during move
     *
     * @see #move(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void move(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile) throws IOException {
        copyOrMove(sourceDir, srcFile, destDir, destFile, OPT_MOVE | OPT_RECURSIVE);
    }

    /**
     * Equivalent to calling {@link #move(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)} with one
     * option.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param option1 the move option to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during move
     *
     * @see #move(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void move(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption option1) throws IOException {
        checkNotNullParam("option1", option1);
        copyOrMove(sourceDir, srcFile, destDir, destFile, parseCopyOption(option1) | OPT_MOVE | OPT_RECURSIVE);
    }

    /**
     * Equivalent to calling {@link #move(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)} with two
     * options.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param option1 the first move option to use for the operation (must not be {@code null})
     * @param option2 the second move option to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during move
     *
     * @see #move(SecureDirectoryStream, Path, SecureDirectoryStream, Path, CopyOption...)
     */
    public static void move(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption option1, CopyOption option2) throws IOException {
        checkNotNullParam("option1", option1);
        checkNotNullParam("option2", option2);
        copyOrMove(sourceDir, srcFile, destDir, destFile,
                parseCopyOption(option1) | parseCopyOption(option2) | OPT_MOVE | OPT_RECURSIVE);
    }

    /**
     * Move a file as securely as possible from one directory and path to another.
     * If both {@code srcFile} and {@code destFile} are {@linkplain Path#isAbsolute() absolute paths},
     * then this method behaves exactly the same as {@link Files#move(Path, Path, CopyOption...)}.
     * If {@code srcFile} and {@code destFile} are found to refer to the same file, then no action is taken
     * and this method will return directly.
     * <p>
     * Otherwise, an attempt is made to:
     * <ul>
     * <li>Move symbolic links as symbolic links (when {@link LinkOption#NOFOLLOW_LINKS} is given)</li>
     * <li>Move directories as directories (including their contents)</li>
     * <li>Preserve file attributes (when {@link StandardCopyOption#COPY_ATTRIBUTES} is given)</li>
     * <li>Move regular files using the most efficient JDK mechanism available</li>
     * </ul>
     * The operation may fail with a {@link IOException} if some part of the operation fails; in this case,
     * the file may be partially copied.
     * <p>
     * If some aspect of the operation is not supported, {@link UnsupportedOperationException} will be thrown.
     * Currently, the currently unsupported operations include:
     * <ul>
     * <li>Copying a symbolic link from or to a relative path (unsupported by the JDK as of JDK 25)</li>
     * <li>Copying a directory into a relative path (unsupported by the JDK as of JDK 25)</li>
     * </ul>
     * The following move options are allowed:
     * <ul>
     * <li>{@link LinkOption#NOFOLLOW_LINKS}</li>
     * <li>{@link StandardCopyOption#COPY_ATTRIBUTES}</li>
     * <li>{@link StandardCopyOption#REPLACE_EXISTING}</li>
     * <li>{@link StandardCopyOption#ATOMIC_MOVE}</li>
     * </ul>
     * Giving any other option will cause a {@link IllegalArgumentException} to be thrown.
     *
     * @param sourceDir the directory stream of the source directory (must not be {@code null})
     * @param srcFile the source path name (must not be {@code null})
     * @param destDir the directory stream of the destination directory (must not be {@code null})
     * @param destFile the destination path name (must not be {@code null})
     * @param options the move options to use for the operation (must not be {@code null})
     * @throws UnsupportedOperationException if the combination of options is unsupported by the source or destination
     *         filesystem or the JDK itself
     * @throws IOException if an I/O error occurs during move
     *
     * @see SecureDirectoryStream#move(Object, SecureDirectoryStream, Object)
     */
    public static void move(SecureDirectoryStream<Path> sourceDir, Path srcFile, SecureDirectoryStream<Path> destDir,
            Path destFile, CopyOption... options) throws IOException {
        copyOrMove(sourceDir, srcFile, destDir, destFile, parseCopyOptions(options) | OPT_MOVE | OPT_RECURSIVE);
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

    private static void copyOrMove(final SecureDirectoryStream<Path> srcDir, final Path srcFile,
            final SecureDirectoryStream<Path> destDir, final Path destFile, final int opts) throws IOException {
        if (has(opts, C_OPT_ATOMIC_MOVE) && !has(opts, OPT_MOVE)) {
            throw log.unsupportedForOperation(StandardCopyOption.ATOMIC_MOVE);
        }
        if (srcFile.isAbsolute() && destFile.isAbsolute() && !has(opts, OPT_RECURSIVE)) {
            // use the JDK methods
            if (has(opts, OPT_MOVE)) {
                Files.move(srcFile, destFile, ALL_COPY_OPTIONS[opts]);
            } else {
                Files.copy(srcFile, destFile, ALL_COPY_OPTIONS[opts]);
            }
            return;
        }
        // first, get attributes to ensure that the source file exists (throws here if not found)
        PosixFileAttributeView srcPosixView = srcDir.getFileAttributeView(srcFile, PosixFileAttributeView.class,
                ALL_LINK_OPTIONS[opts & OPT_NO_FOLLOW]);
        PosixFileAttributes srcPosix = srcPosixView == null ? null : srcPosixView.readAttributes();
        DosFileAttributeView srcDosView = srcPosix != null ? null
                : srcDir.getFileAttributeView(srcFile, DosFileAttributeView.class, ALL_LINK_OPTIONS[opts & OPT_NO_FOLLOW]);
        DosFileAttributes srcDos = srcDosView == null ? null : srcDosView.readAttributes();
        BasicFileAttributes srcBasic = srcPosix != null ? srcPosix
                : srcDos != null ? srcDos
                        : srcDir.getFileAttributeView(srcFile, BasicFileAttributeView.class,
                                ALL_LINK_OPTIONS[opts & OPT_NO_FOLLOW]).readAttributes();
        // source file exists; continue
        FileAttribute<?>[] attrs = has(opts, C_OPT_COPY_ATTRS) && srcPosix != null ? new FileAttribute[] {
                PosixFilePermissions.asFileAttribute(srcPosix.permissions())
        } : NO_FILE_ATTRS;
        BasicFileAttributes oldDestBasic = null;
        try {
            // always use nofollow-links on the destination so we know what to do about it
            oldDestBasic = destDir.getFileAttributeView(destFile, BasicFileAttributeView.class, ALL_LINK_OPTIONS[OPT_NO_FOLLOW])
                    .readAttributes();
        } catch (NoSuchFileException ignored) {
        }
        if (oldDestBasic != null) {
            // try to determine if they are the same file
            Object srcKey = srcBasic.fileKey();
            if (srcKey != null && srcKey.equals(oldDestBasic.fileKey())) {
                // same file; nothing to do
                return;
            }
            // different file, or don't know
            if (!has(opts, C_OPT_REPLACE)) {
                throw log.copyFileExists(destFile);
            }
            // remove the dest. path
            if (oldDestBasic.isDirectory()) {
                // non-recursive removal; fails if directory is not empty
                destDir.deleteDirectory(destFile);
            } else {
                destDir.deleteFile(destFile);
            }
        }
        if (has(opts, OPT_MOVE)) {
            try {
                srcDir.move(srcFile, destDir, destFile);
                return;
            } catch (AtomicMoveNotSupportedException e) {
                if (has(opts, C_OPT_ATOMIC_MOVE)) {
                    throw e;
                }
                // else do a copy+delete instead
            }
        }
        // now check the source type
        boolean isDirectory = srcBasic.isDirectory();
        if (isDirectory) {
            JDKSpecificDirectoryActions.createDirectory(destDir, destFile, attrs);
            if (has(opts, OPT_RECURSIVE)) {
                try (SecureDirectoryStream<Path> srcFileSds = srcDir.newDirectoryStream(srcFile,
                        ALL_LINK_OPTIONS[opts & OPT_NO_FOLLOW])) {
                    try (SecureDirectoryStream<Path> destFileSds = destDir.newDirectoryStream(destFile,
                            ALL_LINK_OPTIONS[OPT_NO_FOLLOW])) {
                        for (Path subPath : srcFileSds) {
                            Path fileName = subPath.getFileName();
                            copyOrMove(srcFileSds, fileName, destFileSds, fileName, opts);
                        }
                    }
                }
            }
        } else if (srcBasic.isSymbolicLink()) {
            Path target = JDKSpecificDirectoryActions.readLink(srcDir, srcFile);
            JDKSpecificDirectoryActions.createSymlink(destDir, destFile, target, attrs);
        } else {
            // regular file
            Set<? extends OpenOption> destOpenOpts = ALL_OPEN_OPTIONS_FOR_COPY_WRITE.get(opts & OPT_NO_FOLLOW);
            try (SeekableByteChannel destCh = destDir.newByteChannel(destFile, destOpenOpts, attrs)) {
                // now open source channel
                Set<? extends OpenOption> srcOpenOpts = ALL_OPEN_OPTIONS_FOR_COPY_READ.get(opts & OPT_NO_FOLLOW);
                try (SeekableByteChannel srcCh = srcDir.newByteChannel(srcFile, srcOpenOpts)) {
                    // do the copy
                    copy(srcCh, destCh);
                }
            }
        }
        // copy attributes
        if (has(opts, C_OPT_COPY_ATTRS)) {
            PosixFileAttributeView newDestPosixView = destDir.getFileAttributeView(destFile, PosixFileAttributeView.class,
                    ALL_LINK_OPTIONS[OPT_NO_FOLLOW]);
            DosFileAttributeView newDestDosView = newDestPosixView != null ? null
                    : destDir.getFileAttributeView(destFile, DosFileAttributeView.class, ALL_LINK_OPTIONS[OPT_NO_FOLLOW]);
            BasicFileAttributeView newDestBasicView = newDestPosixView != null ? newDestPosixView
                    : newDestDosView != null ? newDestDosView
                            : destDir.getFileAttributeView(destFile, BasicFileAttributeView.class,
                                    ALL_LINK_OPTIONS[OPT_NO_FOLLOW]);
            if (srcPosix != null && newDestPosixView != null) {
                // copy owner/group
                newDestPosixView.setOwner(srcPosix.owner());
                newDestPosixView.setGroup(srcPosix.group());
            } else if (srcDos != null && newDestDosView != null) {
                // copy RASH
                newDestDosView.setReadOnly(srcDos.isReadOnly());
                newDestDosView.setArchive(srcDos.isArchive());
                newDestDosView.setSystem(srcDos.isSystem());
                newDestDosView.setHidden(srcDos.isHidden());
            }
            // copy times
            newDestBasicView.setTimes(srcBasic.lastModifiedTime(), srcBasic.lastAccessTime(), srcBasic.creationTime());
        }
        // remove old, if moving
        if (has(opts, OPT_MOVE)) {
            if (isDirectory) {
                srcDir.deleteDirectory(srcFile);
            } else {
                srcDir.deleteFile(srcFile);
            }
        }
    }

    private static final int OPT_NO_FOLLOW = 1 << 0;

    private static final int C_OPT_REPLACE = 1 << 1;
    private static final int C_OPT_COPY_ATTRS = 1 << 2;
    private static final int C_OPT_ATOMIC_MOVE = 1 << 3;

    private static final int OPT_MOVE = 1 << 4;
    private static final int OPT_RECURSIVE = 1 << 5;

    private static int parseCopyOptions(final CopyOption... options) {
        checkNotNullParam("options", options);
        int opts = 0;
        for (int i = 0; i < options.length; i++) {
            final CopyOption option = options[i];
            checkNotNullArrayParam("options", i, option);
            opts |= parseCopyOption(option);
        }
        return opts;
    }

    private static int parseCopyOption(final CopyOption option) {
        if (option instanceof StandardCopyOption o) {
            return switch (o) {
                case REPLACE_EXISTING -> C_OPT_REPLACE;
                case COPY_ATTRIBUTES -> C_OPT_COPY_ATTRS;
                case ATOMIC_MOVE -> C_OPT_ATOMIC_MOVE;
                //noinspection UnnecessaryDefault
                default -> throw log.unknownOption(o);
            };
        } else if (option instanceof LinkOption o) {
            if (o == LinkOption.NOFOLLOW_LINKS) {
                return OPT_NO_FOLLOW;
            } else {
                throw log.unknownOption(o);
            }
        } else {
            throw log.unknownOption(option);
        }
    }

    private static boolean has(int opts, int option) {
        return (opts & option) != 0;
    }

    private static final LinkOption[][] ALL_LINK_OPTIONS = {
            {},
            { LinkOption.NOFOLLOW_LINKS }
    };
    private static final CopyOption[][] ALL_COPY_OPTIONS;
    // order is significant
    private static final List<Set<? extends OpenOption>> ALL_OPEN_OPTIONS_FOR_COPY_WRITE = List.of(
            EnumSet.of(StandardOpenOption.CREATE_NEW),
            Set.of(LinkOption.NOFOLLOW_LINKS, StandardOpenOption.CREATE_NEW));
    private static final List<Set<? extends OpenOption>> ALL_OPEN_OPTIONS_FOR_COPY_READ = List.of(
            EnumSet.noneOf(StandardOpenOption.class),
            EnumSet.of(LinkOption.NOFOLLOW_LINKS));
    private static final FileAttribute<?>[] NO_FILE_ATTRS = new FileAttribute[0];

    static {
        CopyOption[][] outer = new CopyOption[16][];
        for (int i = 0; i < 16; i++) {
            CopyOption[] inner = new CopyOption[Integer.bitCount(i)];
            int bits = i, idx = 0;
            while (bits != 0) {
                inner[idx++] = switch (Integer.numberOfTrailingZeros(bits)) {
                    case 0 -> LinkOption.NOFOLLOW_LINKS;
                    case 1 -> StandardCopyOption.REPLACE_EXISTING;
                    case 2 -> StandardCopyOption.COPY_ATTRIBUTES;
                    case 3 -> StandardCopyOption.ATOMIC_MOVE;
                    default -> throw impossibleSwitchCase(bits);
                };
                bits &= ~Integer.lowestOneBit(bits);
            }
            outer[i] = inner;
        }
        ALL_COPY_OPTIONS = outer;
    }

}
