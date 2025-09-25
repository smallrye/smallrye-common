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
        path = path.normalize();
        boolean noFollow = false;
        for (LinkOption linkOption : linkOptions) {
            switch (linkOption) {
                case NOFOLLOW_LINKS -> noFollow = true;
                default -> throw Messages.log.unknownOption(linkOption);
            }
        }
        return noFollow ? newSecureDirectoryStreamNoFollow(path) : newSecureDirectoryStreamFollow(path);
    }

    /**
     * Open a new secure directory stream at the given path.
     * If the link option {@link LinkOption#NOFOLLOW_LINKS} is given, the open will fail with an exception
     * if {@code path} is a symbolic link.
     *
     * @param path the path of the directory to open (must not be {@code null})
     * @param linkOption the link option (must not be {@code null})
     *
     * @return a secure directory stream for the given path (not {@code null})
     *
     * @throws NotDirectoryException if the target {@code path} is not a directory
     * @throws IOException if another I/O error occurs
     * @throws UnsupportedOperationException if this platform does not support secure directory streams
     *
     * @see SecureDirectoryStream#newDirectoryStream(Object, LinkOption...)
     */
    // this method exists to prevent a spurious array creation when giving an option
    public static SecureDirectoryStream<Path> newSecureDirectoryStream(Path path, LinkOption linkOption)
            throws IOException {
        checkNotNullParam("path", path);
        checkNotNullParam("linkOption", linkOption);
        if (linkOption != LinkOption.NOFOLLOW_LINKS) {
            throw log.unknownOption(linkOption);
        }
        return newSecureDirectoryStreamNoFollow(path.normalize());
    }

    /**
     * Open a new secure directory stream at the given path, following symbolic links.
     *
     * @param path the path of the directory to open (must not be {@code null})
     *
     * @return a secure directory stream for the given path (not {@code null})
     *
     * @throws NotDirectoryException if the target {@code path} is not a directory
     * @throws IOException if another I/O error occurs
     * @throws UnsupportedOperationException if this platform does not support secure directory streams
     *
     * @see SecureDirectoryStream#newDirectoryStream(Object, LinkOption...)
     */
    // this method exists to prevent a spurious array creation when giving no option
    public static SecureDirectoryStream<Path> newSecureDirectoryStream(Path path)
            throws IOException {
        checkNotNullParam("path", path);
        return newSecureDirectoryStreamFollow(path.normalize());
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
        path = path.normalize();
        if (!Files.exists(path)) {
            return;
        }
        Path parent = path.getParent();
        if (parent == null) {
            throw log.cannotRecursivelyDeleteRoot();
        }
        try (DirectoryStream<Path> pds = Files.newDirectoryStream(parent)) {
            if (pds instanceof SecureDirectoryStream<Path> sds) {
                deleteRecursively(sds, path.getFileName());
            } else {
                deleteRecursivelyInsecurely(path);
            }
        }
    }

    /**
     * Attempt to recursively delete all of the files returned by the given directory stream,
     * even if secure directory streams are not supported.
     * If any of the target paths are symbolic links, they will be removed.
     * <em>Warning:</em> this method can potentially delete files outside the intended path
     * if the target platform does not support secure directory iteration.
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
        path = path.normalize();
        Path parent = path.getParent();
        if (parent == null) {
            throw log.cannotRecursivelyDeleteRoot();
        }
        try (SecureDirectoryStream<Path> sds = newSecureDirectoryStreamFollow(parent.normalize())) {
            deleteRecursively(sds, path.getFileName());
        }
    }

    /**
     * Attempt to recursively delete all of the files returned by the given directory stream.
     * If any of the target paths are symbolic links, they will be removed.
     *
     * @param sds the directory stream whose contents should be removed (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     */
    public static void deleteRecursively(SecureDirectoryStream<Path> sds) throws IOException {
        checkNotNullParam("sds", sds);
        for (Path abs : sds) {
            log.tracef("Securely deleting %s", abs);
            deleteRecursively(sds, abs.getFileName());
        }
    }

    /**
     * Attempt to recursively delete the file or directory at the given directory and path.
     * If the target path is a symbolic link, it will be removed.
     * The target {@code path} must be a relative path, and will be normalized to prevent
     * "escaping" via rogue {@code ..} elements.
     *
     * @param sds the directory stream containing the file (must not be {@code null})
     * @param path the relative path of the file or directory to be removed (must not be {@code null})
     * @throws IOException if one or more files fails to be deleted or another I/O error occurs
     * @throws IllegalArgumentException if the given path is absolute
     */
    public static void deleteRecursively(SecureDirectoryStream<Path> sds, Path path) throws IOException {
        checkNotNullParam("sds", sds);
        checkNotNullParam("path", path);
        if (path.isAbsolute()) {
            throw log.notRelative(path);
        }
        // prevent escaping
        path = path.normalize();
        if (sds.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes()
                .isDirectory()) {
            try (SecureDirectoryStream<Path> subStream = sds.newDirectoryStream(path, LinkOption.NOFOLLOW_LINKS)) {
                deleteRecursively(subStream);
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

    // -- private --

    private static final Path CWD = Path.of(System.getProperty("user.dir", ".")).normalize().toAbsolutePath();

    private static SecureDirectoryStream<Path> newSecureDirectoryStreamFollow(Path path) throws IOException {
        DirectoryStream<Path> ds = Files.newDirectoryStream(path);
        // not t-w-r because we only close on error, not on success
        try {
            if (ds instanceof SecureDirectoryStream<Path> sds) {
                return sds;
            }
            throw log.secureDirectoryNotSupported(path.getFileSystem(), path);
        } catch (Throwable t) {
            try {
                ds.close();
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }
    }

    private static SecureDirectoryStream<Path> newSecureDirectoryStreamNoFollow(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent == null) {
            // the root cannot be a symbolic link by definition
            return newSecureDirectoryStreamFollow(path);
        }
        // open the parent directory to safely open subdirectory with NOFOLLOW_LINKS
        try (SecureDirectoryStream<Path> sds = newSecureDirectoryStream(parent)) {
            return sds.newDirectoryStream(path.getFileName(), LinkOption.NOFOLLOW_LINKS);
        }
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
