package io.smallrye.common.io;

import static io.smallrye.common.constraint.Assert.checkNotEmptyAfterTrimParam;
import static io.smallrye.common.constraint.Assert.checkNotEmptyParam;
import static io.smallrye.common.constraint.Assert.checkNotNullParam;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.NotLinkException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import io.smallrye.common.constraint.Assert;

/**
 * Utilities for {@link java.nio.file.Files}. Fluent style methods, more input
 * types and unchecked exceptions make these more convenient.
 * <p>
 * Design decisions and assumptions:
 * </p>
 * <p>
 * If you create a {@code Path} element from a {@code String} a
 * {@link io.smallrye.common.constraint.Assert#checkNotEmptyAfterTrimParam(String, String)}
 * will be done first. Both <a href=
 * "https://docs.microsoft.com/en-us/troubleshoot/windows-client/shell-experience/file-folder-name-whitespace-characters">Windows</a>
 * and <a href=
 * "https://dwheeler.com/essays/fixing-unix-linux-filenames.html">Unixoid
 * systems</a> suffer from trailing whitespace, even if individually supported
 * or not. Several bugs are filed for this issue for different JDK versions.
 * Therefore default is to trim, you have to construct manually special cases.
 * Same for empty String "": If you want to express the present working
 * directory, use a portable dot (".") to express it at the beginning of a
 * {@code Path} element.
 * </p>
 * <p>
 * The PathUtils are based on {@link java.io.UncheckedIOException} to give users
 * the chance to avoid (or remove) boilerplate code handling IOException.
 * Existing code often has utility methods simply declaring a throws IOException
 * (but not handling it). A central method catches the IOException and rethrows
 * it as a RuntimeException, most times a Exception class not intended for IO
 * but not a generic error of the logic. In PathUtils in every case you still
 * get the best possible message what is really wrong, keeping one of the great
 * advantages to use NIO.
 * </p>
 * <p>
 * Every method checks arguments required to be not null in the same way with
 * {@link io.smallrye.common.constraint.Assert#checkNotNullParam(String, Object)}
 * and avoids anonymous, subsequent NullPointerExceptions, due to
 * io.smallrye.common.io uses IllegalArgumentException to express explicit,
 * named NotNull constraints. It's taken care to avoid double checks.
 * </p>
 * <p>
 * The usage of {@link java.nio.file.LinkOption#NOFOLLOW_LINKS} has been expressed
 * with methods. Different to the implicit usage in classic java.io this
 * shall be visible, even in subsequent stacktraces for users of PathUtils. This
 * has not been done for {@link java.nio.file.StandardCopyOption} or similar, due
 * to the large amount of possible combinations and use cases.
 * </p>
 *
 * @author Boris Unckel
 *
 */
public final class PathUtils {

    public static final LinkOption[] NO_FOLLOW_LINK_OPTION_ARRAY = { LinkOption.NOFOLLOW_LINKS };

    private static final String[] EMPTY_STRING_ARRAY = new String[] {};

    /**
     * A wrapper around {@link java.nio.file.Paths#get(String, String...)} with
     * explicit null check.
     *
     * @param first part of the path, must be not null, not empty and a valid path
     * @param more optional parts of the path
     * @return the path constructed out of first and more
     * @throws IllegalArgumentException if first is null or empty
     */
    public static Path get(final String first, final String... more) {
        return Paths.get(checkNotEmptyAfterTrimParam("first", first), checkElementsNotEmpty(more));
    }

    static String[] checkElementsNotEmpty(final String... more) {
        if (more == null || more.length == 0) {
            return EMPTY_STRING_ARRAY;
        }
        String[] checkedArray = new String[more.length];
        for (int i = 0; i < more.length; i++) {
            checkedArray[i] = checkNotEmptyAfterTrimParam("more", more[i]);
        }
        return checkedArray;
    }

    /**
     * A wrapper around {@link java.nio.file.Paths#get(URI)} with explicit null
     * check.
     *
     * @param uri for the path, must be not null and a valid path
     * @return the path constructed out of the uri
     * @throws IllegalArgumentException if uri is null
     */
    public static Path get(final URI uri) {
        return Paths.get(checkNotNullParam("uri", uri));
    }

    /**
     * A wrapper around {@link java.nio.file.Files#newDirectoryStream(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null.
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final Path dir) throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        return doNewDirectoryStream(dir);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#newDirectoryStream(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null, not empty, and a valid
     *        path.
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final String dir) throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotEmptyAfterTrimParam("dir", dir));
        return doNewDirectoryStream(aDir);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#newDirectoryStream(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null and a valid path.
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final URI dir) throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotNullParam("dir", dir));
        return doNewDirectoryStream(aDir);
    }

    static DirectoryStream<Path> doNewDirectoryStream(final Path dir) throws UncheckedIOException {
        try {
            return Files.newDirectoryStream(dir);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around {@link java.nio.file.Files#newDirectoryStream(Path, String)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null
     * @param glob the pattern, must be not null and not empty
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the dir or glob is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final Path dir, final String glob)
            throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        return doNewDirectoryStream(dir, checkNotEmptyParam("glob", glob));
    }

    /**
     * A wrapper around {@link java.nio.file.Files#newDirectoryStream(Path, String)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null, not empty, and a
     *        valid path
     * @param glob the pattern, must be not null and not empty
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the dir or glob is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final String dir, final String glob)
            throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotEmptyAfterTrimParam("dir", dir));
        return doNewDirectoryStream(aDir, checkNotEmptyParam("glob", glob));
    }

    /**
     * A wrapper around {@link java.nio.file.Files#newDirectoryStream(Path, String)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null and a valid path
     * @param glob the pattern, must be not null and not empty
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the dir or glob is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final URI dir, final String glob)
            throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotNullParam("dir", dir));
        return doNewDirectoryStream(aDir, checkNotEmptyParam("glob", glob));
    }

    static DirectoryStream<Path> doNewDirectoryStream(final Path dir, final String glob) throws UncheckedIOException {
        try {
            return Files.newDirectoryStream(dir, glob);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#newDirectoryStream(Path, java.nio.file.DirectoryStream.Filter)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null
     * @param filter for the stream, must be not null
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the dir or filter is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        checkNotNullParam("filter", filter);
        return doNewDirectoryStream(dir, filter);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#newDirectoryStream(Path, java.nio.file.DirectoryStream.Filter)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null, not empty, and a
     *        valid path
     * @param filter for the stream, must be not null
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the dir or filter is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final String dir,
            final DirectoryStream.Filter<? super Path> filter) throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotEmptyAfterTrimParam("dir", dir));
        checkNotNullParam("filter", filter);
        return doNewDirectoryStream(aDir, filter);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#newDirectoryStream(Path, java.nio.file.DirectoryStream.Filter)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to the directory to list, must be not null and a valid path
     * @param filter for the stream, must be not null
     * @return the elements in the directory as DirectoryStream
     * @throws IllegalArgumentException if the dir or filter is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static DirectoryStream<Path> newDirectoryStream(final URI dir,
            final DirectoryStream.Filter<? super Path> filter) throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotNullParam("dir", dir));
        checkNotNullParam("filter", filter);
        return doNewDirectoryStream(aDir, filter);
    }

    static DirectoryStream<Path> doNewDirectoryStream(final Path dir, final DirectoryStream.Filter<? super Path> filter)
            throws UncheckedIOException {
        try {
            return Files.newDirectoryStream(dir, filter);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createFile(Path, FileAttribute...)} with explicit
     * null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path of the file to create, must be not null
     * @param attrs optional attributes of the file to create
     * @return the created file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createFile(final Path path, final FileAttribute<?>... attrs) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCreateFile(path, attrs);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createFile(Path, FileAttribute...)} with explicit
     * null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path of the file to create, must be not null, not empty, and a valid
     *        path.
     * @param attrs optional attributes of the file to create
     * @return the created file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createFile(final String path, final FileAttribute<?>... attrs) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCreateFile(aPath, attrs);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createFile(Path, FileAttribute...)} with explicit
     * null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path of the file to create, must be not null and a valid path
     * @param attrs optional attributes of the file to create
     * @return the created file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createFile(final URI path, final FileAttribute<?>... attrs) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCreateFile(aPath, attrs);
    }

    static Path doCreateFile(final Path path, final FileAttribute<?>... attrs) throws UncheckedIOException {
        try {
            return Files.createFile(path, attrs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createDirectory(Path, FileAttribute...)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to create, must be not null
     * @param attrs optional attributes of the directory to create
     * @return the created directory
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createDirectory(final Path dir, final FileAttribute<?>... attrs) throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        return doCreateDirectory(dir, attrs);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createDirectory(Path, FileAttribute...)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to create, must be not null, not empty, and a valid path
     * @param attrs optional attributes of the directory to create
     * @return the created directory
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createDirectory(final String dir, final FileAttribute<?>... attrs) throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotEmptyAfterTrimParam("dir", dir));
        return doCreateDirectory(aDir, attrs);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createDirectory(Path, FileAttribute...)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to create, must be not null and a valid path
     * @param attrs optional attributes of the directory to create
     * @return the created directory
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createDirectory(final URI dir, final FileAttribute<?>... attrs) throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotNullParam("dir", dir));
        return doCreateDirectory(aDir, attrs);
    }

    static Path doCreateDirectory(final Path dir, final FileAttribute<?>... attrs) throws UncheckedIOException {
        try {
            return Files.createDirectory(dir, attrs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createDirectories(Path, FileAttribute...)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to create, must be not null
     * @param attrs optional attributes of the directory to create
     * @return the created directory
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createDirectories(final Path dir, final FileAttribute<?>... attrs) throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        return doCreateDirectories(dir, attrs);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createDirectories(Path, FileAttribute...)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to create, must be not null, not empty, and a valid path
     * @param attrs optional attributes of the directory to create
     * @return the created directory
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createDirectories(final String dir, final FileAttribute<?>... attrs)
            throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotEmptyAfterTrimParam("dir", dir));
        return doCreateDirectories(aDir, attrs);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createDirectories(Path, FileAttribute...)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param dir to create, must be not null and a valid path
     * @param attrs optional attributes of the directory to create
     * @return the created directory
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createDirectories(final URI dir, final FileAttribute<?>... attrs) throws UncheckedIOException {
        final Path aDir = Paths.get(checkNotNullParam("dir", dir));
        return doCreateDirectories(aDir, attrs);
    }

    static Path doCreateDirectories(final Path dir, final FileAttribute<?>... attrs) throws UncheckedIOException {
        try {
            return Files.createDirectories(dir, attrs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a new file if not exists or update the last modified time to now.
     *
     * @param path to create, must be not null
     * @return the created or updated path
     * @throws IllegalArgumentException if path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path touch(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doTouch(path);
    }

    /**
     * Create a new file if not exists or update the last modified time to now.
     *
     * @param path to create, must be not null, not empty and a valid path
     * @return the created or updated path
     * @throws IllegalArgumentException if path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path touch(final String path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doTouch(aPath);
    }

    /**
     * Create a new file if not exists or update the last modified time to now.
     *
     * @param path to create, must be not null and a valid path
     * @return the created or updated path
     * @throws IllegalArgumentException if path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path touch(final URI path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return doTouch(aPath);
    }

    static Path doTouch(final Path path) throws UncheckedIOException {
        try {
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
            }
            return Files.createFile(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createSymbolicLink(Path, Path, FileAttribute...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to create, must be not null
     * @param target to point to, must be not null
     * @param attrs optional attributes of the link
     * @return the created symbolic link
     * @throws IllegalArgumentException if the link or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createSymbolicLink(final Path link, final Path target, final FileAttribute<?>... attrs)
            throws UncheckedIOException {
        checkNotNullParam("link", link);
        checkNotNullParam("target", target);
        return doCreateSymbolicLink(link, target, attrs);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createSymbolicLink(Path, Path, FileAttribute...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to create, must be not null, not empty, and a valid path
     * @param target to point to, must be not null, not empty, and a valid path
     * @param attrs optional attributes of the link
     * @return the created symbolic link
     * @throws IllegalArgumentException if the link or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createSymbolicLink(final String link, final String target, final FileAttribute<?>... attrs)
            throws UncheckedIOException {
        final Path aLink = Paths.get(checkNotEmptyAfterTrimParam("link", link));
        final Path aTarget = Paths.get(checkNotEmptyAfterTrimParam("target", target));
        return doCreateSymbolicLink(aLink, aTarget, attrs);
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createSymbolicLink(Path, Path, FileAttribute...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to create, must be not null and a valid path
     * @param target to point to, must be not null and a valid path
     * @param attrs optional attributes of the link
     * @return the created symbolic link
     * @throws IllegalArgumentException if the link or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createSymbolicLink(final URI link, final URI target, final FileAttribute<?>... attrs)
            throws UncheckedIOException {
        final Path aLink = Paths.get(checkNotNullParam("link", link));
        final Path aTarget = Paths.get(checkNotNullParam("target", target));
        return doCreateSymbolicLink(aLink, aTarget, attrs);
    }

    static Path doCreateSymbolicLink(final Path link, final Path target, final FileAttribute<?>... attrs)
            throws UncheckedIOException {
        doCheckNotExistsNoFollowLinks(link);
        doCheckExistsNoFollowLinks(target);
        try {
            return Files.createSymbolicLink(link, target, attrs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around {@link java.nio.file.Files#createLink(Path, Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to create, must be not null
     * @param target to point to, must be not null
     * @return the created link
     * @throws IllegalArgumentException if the link or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createLink(final Path link, final Path target) throws UncheckedIOException {
        checkNotNullParam("link", link);
        checkNotNullParam("target", target);
        return doCreateLink(link, target);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#createLink(Path, Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to create, must be not null, not empty, and a valid path
     * @param target to point to, must be not null, not empty, and a valid path
     * @return the created link
     * @throws IllegalArgumentException if the link or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createLink(final String link, final String target) throws UncheckedIOException {
        final Path aLink = Paths.get(checkNotEmptyAfterTrimParam("link", link));
        final Path aTarget = Paths.get(checkNotEmptyAfterTrimParam("target", target));
        return doCreateLink(aLink, aTarget);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#createLink(Path, Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to create, must be not null and a valid path
     * @param target to point to, must be not null and a valid path
     * @return the created link
     * @throws IllegalArgumentException if the link or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path createLink(final URI link, final URI target) throws UncheckedIOException {
        final Path aLink = Paths.get(checkNotNullParam("link", link));
        final Path aTarget = Paths.get(checkNotNullParam("target", target));
        return doCreateLink(aLink, aTarget);
    }

    static Path doCreateLink(final Path link, final Path target) throws UncheckedIOException {
        doCheckNotExistsNoFollowLinks(link);
        doCheckExistsNoFollowLinks(target);
        try {
            return Files.createLink(link, target);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around {@link java.nio.file.Files#delete(Path)} with explicit null
     * check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to delete, must be not null
     * @return the deleted path
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path delete(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doDelete(path);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#delete(Path)} with explicit null
     * check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to delete, must be not null, not empty, and a valid path
     * @return the deleted path
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path delete(final String path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doDelete(aPath);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#delete(Path)} with explicit null
     * check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to delete, must be not null and a valid path
     * @return the deleted path
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path delete(final URI path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return doDelete(aPath);
    }

    static Path doDelete(final Path path) throws UncheckedIOException {
        try {
            Files.delete(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        assert (notExistsNoFollowLinks(path));
        return path;
    }

    /**
     * A wrapper around {@link java.nio.file.Files#deleteIfExists(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to delete, must be not null
     * @return the deleted path
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path deleteIfExists(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doDeleteIfExists(path);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#deleteIfExists(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to delete, must be not null, not empty, and a valid path
     * @return the deleted path
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path deleteIfExists(final String path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doDeleteIfExists(aPath);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#deleteIfExists(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to delete, must be not null and a valid path
     * @return the deleted path
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path deleteIfExists(final URI path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return doDeleteIfExists(aPath);
    }

    static Path doDeleteIfExists(final Path path) throws UncheckedIOException {
        try {
            Files.deleteIfExists(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        assert (notExistsNoFollowLinks(path));
        return path;
    }

    /**
     * Recursive deletion of path. If path is a directory it will cleaned, including
     * subfolders and deleted itself.
     * <p>
     * Adopted from Google Guava <a href=
     * "https://github.com/google/guava/blob/master/guava/src/com/google/common/io/MoreFiles.java">com.google.common.io.MoreFiles</a>.
     * </p>
     *
     * @param path to delete, must be not null, and exists.
     * @return the path to delete
     * @throws UncheckedIOException around original IOExceptions and if not exists
     *         path
     */
    public static Path deleteRecursively(final Path path) throws UncheckedIOException {
        return doDeleteRecursively(checkExistsNoFollowLinks(path));
    }

    static Path doDeleteRecursively(final Path path) throws UncheckedIOException {
        Path parentPath = getParentPath(path);
        if (parentPath == null) {
            throw new UncheckedIOException(new FileSystemException(path.toString(), null, "can't delete recursively"));
        }

        Collection<IOException> exceptions = null; // created lazily if needed
        try {
            boolean sdsSupported = false;
            try (DirectoryStream<Path> parent = Files.newDirectoryStream(parentPath)) {
                if (parent instanceof SecureDirectoryStream) {
                    sdsSupported = true;
                    exceptions = deleteRecursivelySecure((SecureDirectoryStream<Path>) parent,
                            /*
                             * requireNonNull is safe because paths have file names when they have parents,
                             * and we checked for a parent at the beginning of the method.
                             */
                            checkNotNullParam("fileName", path.getFileName()), exceptions);
                }
            }

            if (!sdsSupported) {
                exceptions = deleteRecursivelyInsecure(path, exceptions);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (exceptions != null) {
            FileSystemException deleteFailed = new FileSystemException(path.toString(), null,
                    "failed to delete one or more files; see suppressed exceptions for details");
            for (IOException e : exceptions) {
                deleteFailed.addSuppressed(e);
            }
            throw new UncheckedIOException(deleteFailed);
        }
        return path;
    }

    /**
     * Recursive deletion of directories content, including subfolders.
     * <p>
     * Adopted from Google Guava <a href=
     * "https://github.com/google/guava/blob/master/guava/src/com/google/common/io/MoreFiles.java">com.google.common.io.MoreFiles</a>.
     * </p>
     *
     * @param path to delete, must be not null, a path, and exists.
     * @return the path to delete
     * @throws UncheckedIOException around original IOExceptions and if not exists
     *         path
     */
    public static Path deleteDirectoryContents(Path path) throws UncheckedIOException {
        return doDeleteDirectoryContents(checkIsDirectoryNoFollowLinks(path));
    }

    static Path doDeleteDirectoryContents(Path path) throws UncheckedIOException {
        Collection<IOException> exceptions = null; // created lazily if needed
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            if (stream instanceof SecureDirectoryStream) {
                SecureDirectoryStream<Path> sds = (SecureDirectoryStream<Path>) stream;
                exceptions = deleteDirectoryContentsSecure(sds, exceptions);
            } else {
                exceptions = deleteDirectoryContentsInsecure(stream, exceptions);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (exceptions != null) {
            FileSystemException deleteFailed = new FileSystemException(path.toString(), null,
                    "failed to delete one or more files; see suppressed exceptions for details");
            for (IOException e : exceptions) {
                deleteFailed.addSuppressed(e);
            }
            throw new UncheckedIOException(deleteFailed);
        }
        return path;
    }

    private static Collection<IOException> deleteRecursivelySecure(final SecureDirectoryStream<Path> dir,
            final Path path, Collection<IOException> excCol) {
        Collection<IOException> exceptions = excCol;
        try {
            boolean isDirectory = dir
                    .getFileAttributeView(path, BasicFileAttributeView.class, NO_FOLLOW_LINK_OPTION_ARRAY)
                    .readAttributes().isDirectory();
            if (isDirectory) {
                try (SecureDirectoryStream<Path> childDir = dir.newDirectoryStream(path, NO_FOLLOW_LINK_OPTION_ARRAY)) {
                    exceptions = deleteDirectoryContentsSecure(childDir, exceptions);
                }

                if (exceptions == null) {
                    dir.deleteDirectory(path);
                }
            } else {
                dir.deleteFile(path);
            }
            assert (notExistsNoFollowLinks(path));
            return exceptions;
        } catch (IOException e) {
            return addException(exceptions, e);
        }
    }

    private static Collection<IOException> deleteDirectoryContentsSecure(final SecureDirectoryStream<Path> dir,
            Collection<IOException> excCol) {
        Collection<IOException> exceptions = excCol;
        try {
            for (Path path : dir) {
                exceptions = deleteRecursivelySecure(dir, path.getFileName(), exceptions);
            }

            return exceptions;
        } catch (DirectoryIteratorException e) {
            return addException(exceptions, e.getCause());
        }
    }

    private static Collection<IOException> deleteRecursivelyInsecure(final Path path,
            final Collection<IOException> excCol) {
        Collection<IOException> exceptions = excCol;
        try {
            if (Files.isDirectory(path, NO_FOLLOW_LINK_OPTION_ARRAY)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    exceptions = deleteDirectoryContentsInsecure(stream, exceptions);
                }
            }

            if (exceptions == null) {
                doDeleteIgnoreExists(path);
            }

            return exceptions;
        } catch (IOException e) {
            return addException(exceptions, e);
        }
    }

    private static Collection<IOException> deleteDirectoryContentsInsecure(DirectoryStream<Path> dir,
            final Collection<IOException> excCol) {
        Collection<IOException> exceptions = excCol;
        try {
            for (Path entry : dir) {
                exceptions = deleteRecursivelyInsecure(entry, exceptions);
            }

            return exceptions;
        } catch (DirectoryIteratorException e) {
            return addException(exceptions, e.getCause());
        }
    }

    private static Collection<IOException> addException(Collection<IOException> col, final IOException ex) {
        if (col == null) {
            col = new ArrayList<IOException>();
        }
        col.add(ex);
        return col;
    }

    /**
     * This methods intentionally uses {@link java.nio.file.Files#delete(Path)} and
     * ignores a {@link java.nio.file.NoSuchFileException}. This is useful for cases
     * of invalid links.
     *
     * Do not refactor usage to deleteIfExists!
     *
     * @param path to delete
     * @return the deleted path
     * @throws IOException if delete fails
     */
    private static Path doDeleteIgnoreExists(final Path path) throws IOException {
        try {
            Files.delete(path);
        } catch (final NoSuchFileException nsfEx) {
            // intentionally ignored.
        }
        assert (notExistsNoFollowLinks(path));
        return path;
    }

    private static Path getParentPath(Path path) {
        if (path == null) {
            return null;
        }
        Path parent = path.getParent();

        // simple Case "/mypath" parent is "/" or "c:\mypath" is "c:\"
        if (parent != null) {
            return parent;
        }

        // no parent
        if (path.getNameCount() == 0) {
            return null;
        } else {
            // working dir
            return path.getFileSystem().getPath(".");
        }
    }

    /**
     * A wrapper around {@link java.nio.file.Files#copy(Path, Path, CopyOption...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param source of the copy, must be not null
     * @param target of the copy, must be not null
     * @param options of the copy process
     * @return the target path
     * @throws IllegalArgumentException if the source or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path copy(final Path source, final Path target, final CopyOption... options)
            throws UncheckedIOException {
        checkNotNullParam("source", source);
        checkNotNullParam("target", target);
        return doCopy(source, target, options);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#copy(Path, Path, CopyOption...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param source of the copy, must be not null, not empty, and a valid path
     * @param target of the copy, must be not null, not empty, and a valid path
     * @param options of the copy process
     * @return the target path
     * @throws IllegalArgumentException if the source or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path copy(final String source, final String target, final CopyOption... options)
            throws UncheckedIOException {
        final Path aSource = Paths.get(checkNotEmptyAfterTrimParam("source", source));
        final Path aTarget = Paths.get(checkNotEmptyAfterTrimParam("target", target));
        return doCopy(aSource, aTarget, options);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#copy(Path, Path, CopyOption...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param source of the copy, must be not null and a valid path
     * @param target of the copy, must be not null and a valid path
     * @param options of the copy process
     * @return the target path
     * @throws IllegalArgumentException if the source or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path copy(final URI source, final URI target, final CopyOption... options)
            throws UncheckedIOException {
        final Path aSource = Paths.get(checkNotNullParam("source", source));
        final Path aTarget = Paths.get(checkNotNullParam("target", target));
        return doCopy(aSource, aTarget, options);
    }

    static Path doCopy(final Path source, final Path target, final CopyOption... options) throws UncheckedIOException {
        try {
            return Files.copy(source, target, options);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around {@link java.nio.file.Files#move(Path, Path, CopyOption...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param source of the move, must be not null
     * @param target of the move, must be not null
     * @param options of the move process
     * @return the target path
     * @throws IllegalArgumentException if the source or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path move(final Path source, final Path target, final CopyOption... options)
            throws UncheckedIOException {
        checkNotNullParam("source", source);
        checkNotNullParam("target", target);
        return doMove(source, target, options);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#move(Path, Path, CopyOption...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param source of the move, must be not null, not empty, and a valid path
     * @param target of the move, must be not null, not empty, and a valid path
     * @param options of the move process
     * @return the target path
     * @throws IllegalArgumentException if the source or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path move(final String source, final String target, final CopyOption... options)
            throws UncheckedIOException {
        final Path aSource = Paths.get(checkNotEmptyAfterTrimParam("source", source));
        final Path aTarget = Paths.get(checkNotEmptyAfterTrimParam("target", target));
        return doMove(aSource, aTarget, options);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#move(Path, Path, CopyOption...)}
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param source of the move, must be not null and a valid path
     * @param target of the move, must be not null and a valid path
     * @param options of the move process
     * @return the target path
     * @throws IllegalArgumentException if the source or target is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path move(final URI source, final URI target, final CopyOption... options)
            throws UncheckedIOException {
        final Path aSource = Paths.get(checkNotNullParam("source", source));
        final Path aTarget = Paths.get(checkNotNullParam("target", target));
        return doMove(aSource, aTarget, options);
    }

    static Path doMove(final Path source, final Path target, final CopyOption... options) throws UncheckedIOException {
        try {
            return Files.move(source, target, options);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around {@link java.nio.file.Files#readSymbolicLink(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to read
     * @return the target of the link
     * @throws IllegalArgumentException if the link is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path readSymbolicLink(final Path link) throws UncheckedIOException {
        checkNotNullParam("link", link);
        return doReadSymbolicLink(link);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#readSymbolicLink(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to read
     * @return the target of the link
     * @throws IllegalArgumentException if the link is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path readSymbolicLink(final String link) throws UncheckedIOException {
        final Path aLink = Paths.get(checkNotEmptyAfterTrimParam("link", link));
        return doReadSymbolicLink(aLink);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#readSymbolicLink(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param link to read
     * @return the target of the link, regardless whether the target exists or not!
     * @throws IllegalArgumentException if the link is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static Path readSymbolicLink(final URI link) throws UncheckedIOException {
        final Path aLink = Paths.get(checkNotNullParam("link", link));
        return doReadSymbolicLink(aLink);
    }

    static Path doReadSymbolicLink(final Path link) throws UncheckedIOException {
        try {
            return Files.readSymbolicLink(link);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around {@link java.nio.file.Files#isSameFile(Path, Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path first part to compare, must be not null
     * @param path2 with second part to compare, must be not null
     * @return true if both are same according to underlying logic
     * @throws IllegalArgumentException if the path or path2 is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static boolean isSameFile(final Path path, final Path path2) throws UncheckedIOException {
        checkNotNullParam("path", path);
        checkNotNullParam("path2", path2);
        return doIsSameFile(path, path2);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#isSameFile(Path, Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path first part to compare, must be not null, not empty, and a valid
     *        path
     * @param path2 with second part to compare, must be not null, not empty, and a
     *        valid path
     * @return true if both are same according to underlying logic
     * @throws IllegalArgumentException if the path or path2 is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static boolean isSameFile(final String path, final String path2) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        final Path aPath2 = Paths.get(checkNotEmptyAfterTrimParam("path2", path2));
        return doIsSameFile(aPath, aPath2);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#isSameFile(Path, Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path first part to compare, must be not null and a valid path
     * @param path2 with second part to compare, must be not null and a valid path
     * @return true if both are same according to underlying logic
     * @throws IllegalArgumentException if the path or path2 is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static boolean isSameFile(final URI path, final URI path2) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        final Path aPath2 = Paths.get(checkNotNullParam("path2", path2));
        return doIsSameFile(aPath, aPath2);
    }

    static boolean doIsSameFile(final Path path, final Path path2) throws UncheckedIOException {
        try {
            return Files.isSameFile(path, path2);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A fluent style symbolic link check, returning the passed argument if path is
     * a symbolic link. Check is done exactly as
     * {@link java.nio.file.Files#isSymbolicLink(Path)} but with explicit null check
     * and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null, not empty, and a valid path
     * @return the passed path if it is a symbolic link
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a link or determination of type
     *         fails
     */
    public static Path checkIsSymbolicLink(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckIsSymbolicLink(path);
    }

    /**
     * A fluent style symbolic link check, returning the passed argument if path is
     * a symbolic link. Check is done exactly as
     * {@link java.nio.file.Files#isSymbolicLink(Path)} but with explicit null check
     * and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null
     * @return the passed path if it is a symbolic link
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a link or determination of type
     *         fails
     */
    public static Path checkIsSymbolicLink(final String path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckIsSymbolicLink(aPath);
    }

    /**
     * A fluent style symbolic link check, returning the passed argument if path is
     * a symbolic link. Check is done exactly as
     * {@link java.nio.file.Files#isSymbolicLink(Path)} but with explicit null check
     * and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null and a valid path
     * @return the passed path if it is a symbolic link
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a link or determination of type
     *         fails
     */
    public static Path checkIsSymbolicLink(final URI path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckIsSymbolicLink(aPath);
    }

    static Path doCheckIsSymbolicLink(final Path path) throws UncheckedIOException {
        boolean isSymbolicLink = false;
        try {
            isSymbolicLink = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                    .isSymbolicLink();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        if (isSymbolicLink) {
            return path;
        }
        throw new UncheckedIOException(new NotLinkException(path.toString(), null, "must be a symbolic link"));
    }

    /**
     * A fluent style directory check, following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null
     * @return the passed path if it is a directory
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a directory or determination of
     *         type fails
     */
    public static Path checkIsDirectoryFollowLinks(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckIsDirectoryFollowLinks(path);
    }

    /**
     * A fluent style directory check, following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null, not empty, and a valid path
     * @return the passed path if it is a directory
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a directory or determination of
     *         type fails
     */
    public static Path checkIsDirectoryFollowLinks(final String path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckIsDirectoryFollowLinks(aPath);
    }

    /**
     * A fluent style directory check, following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null and a valid path
     * @return the passed path if it is a directory
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a directory or determination of
     *         type fails
     */
    public static Path checkIsDirectoryFollowLinks(final URI path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckIsDirectoryFollowLinks(aPath);
    }

    static Path doCheckIsDirectoryFollowLinks(final Path path) throws UncheckedIOException {
        boolean isDirectory = false;
        try {
            isDirectory = Files.readAttributes(path, BasicFileAttributes.class).isDirectory();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        if (isDirectory) {
            return path;
        }
        throw new UncheckedIOException(new NotDirectoryException(path.toString()));
    }

    /**
     * A fluent style directory check, not following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null
     * @return the passed path if it is a directory
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a directory or determination of
     *         type fails
     */
    public static Path checkIsDirectoryNoFollowLinks(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckIsDirectoryNoFollowLinks(path);
    }

    /**
     * A fluent style directory check, not following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null, not empty and a valid path
     * @return the passed path if it is a directory
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a directory or determination of
     *         type fails
     */
    public static Path checkIsDirectoryNoFollowLinks(final String path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckIsDirectoryNoFollowLinks(aPath);
    }

    /**
     * A fluent style directory check, not following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null and a valid path
     * @return the passed path if it is a directory
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a directory or determination of
     *         type fails
     */
    public static Path checkIsDirectoryNoFollowLinks(final URI path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckIsDirectoryNoFollowLinks(aPath);
    }

    static Path doCheckIsDirectoryNoFollowLinks(final Path path) throws UncheckedIOException {
        boolean isDirectory = false;
        try {
            isDirectory = Files.readAttributes(path, BasicFileAttributes.class, NO_FOLLOW_LINK_OPTION_ARRAY)
                    .isDirectory();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        if (isDirectory) {
            return path;
        }
        throw new UncheckedIOException(new NotDirectoryException(path.toString()));
    }

    /**
     * A fluent style regular file check, following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null
     * @return the passed path if it is a regular file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a regular file or determination of
     *         type fails
     */
    public static Path checkIsRegularFileFollowLinks(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckIsRegularFileFollowLinks(path);
    }

    /**
     * A fluent style regular file check, following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null, not empty, and a valid path
     * @return the passed path if it is a regular file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a regular file or determination of
     *         type fails
     */
    public static Path checkIsRegularFileFollowLinks(final String path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckIsRegularFileFollowLinks(aPath);
    }

    /**
     * A fluent style regular file check, following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null and a valid path
     * @return the passed path if it is a regular file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a regular file or determination of
     *         type fails
     */
    public static Path checkIsRegularFileFollowLinks(final URI path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckIsRegularFileFollowLinks(aPath);
    }

    static Path doCheckIsRegularFileFollowLinks(final Path path) throws UncheckedIOException {
        boolean isRegularFile = false;
        try {
            isRegularFile = Files.readAttributes(path, BasicFileAttributes.class).isRegularFile();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        if (isRegularFile) {
            return path;
        }
        throw new UncheckedIOException(new FileSystemException(path.toString(), null, "must be a regular file"));
    }

    /**
     * A fluent style regular file check, not following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null
     * @return the passed path if it is a regular file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a regular file or determination of
     *         type fails
     */
    public static Path checkIsRegularFileNoFollowLinks(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckIsRegularFileNoFollowLinks(path);
    }

    /**
     * A fluent style regular file check, not following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null, not empty, and a valid path
     * @return the passed path if it is a regular file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a regular file or determination of
     *         type fails
     */
    public static Path checkIsRegularFileNoFollowLinks(final String path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckIsRegularFileNoFollowLinks(aPath);
    }

    /**
     * A fluent style regular file check, not following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, must be not null and a valid path
     * @return the passed path if it is a regular file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a regular file or determination of
     *         type fails
     */
    public static Path checkIsRegularFileNoFollowLinks(final URI path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckIsRegularFileNoFollowLinks(aPath);
    }

    static Path doCheckIsRegularFileNoFollowLinks(final Path path) throws UncheckedIOException {
        boolean isRegularFile = false;
        try {
            isRegularFile = Files.readAttributes(path, BasicFileAttributes.class, NO_FOLLOW_LINK_OPTION_ARRAY)
                    .isRegularFile();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        if (isRegularFile) {
            return path;
        }
        throw new UncheckedIOException(new FileSystemException(path.toString(), null, "must be a regular file"));
    }

    /**
     * A wrapper around {@link java.nio.file.Files#exists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are followed, must be not null
     * @return true if path is present
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean existsFollowLinks(final Path path) {
        checkNotNullParam("path", path);
        return Files.exists(path);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#exists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are followed, must be not null, not empty,
     *        and a valid path
     * @return true if path is present
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean existsFollowLinks(final String path) {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return Files.exists(aPath);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#exists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are followed, must be not null and a valid
     *        path
     * @return true if path is present
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean existsFollowLinks(final URI path) {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return Files.exists(aPath);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#exists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are not followed, must be not null
     * @return true if path is present
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean existsNoFollowLinks(final Path path) {
        checkNotNullParam("path", path);
        return Files.exists(path, NO_FOLLOW_LINK_OPTION_ARRAY);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#exists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are not followed, must be not null, not
     *        empty, and a valid path
     * @return true if path is present
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean existsNoFollowLinks(final String path) {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return Files.exists(aPath, NO_FOLLOW_LINK_OPTION_ARRAY);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#exists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are not followed, must be not null and a
     *        valid path
     * @return true if path is present
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean existsNoFollowLinks(final URI path) {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return Files.exists(aPath, NO_FOLLOW_LINK_OPTION_ARRAY);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#notExists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are followed, must be not null
     * @return true if path is absent
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean notExistsFollowLinks(final Path path) {
        checkNotNullParam("path", path);
        return Files.notExists(path);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#notExists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are followed, must be not null, not empty,
     *        and a valid path
     * @return true if path is absent
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean notExistsFollowLinks(final String path) {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return Files.notExists(aPath);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#notExists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are followed, must be not null and a valid
     *        path
     * @return true if path is absent
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean notExistsFollowLinks(final URI path) {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return Files.notExists(aPath);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#notExists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are not followed, must be not null
     * @return true if path is absent
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean notExistsNoFollowLinks(final Path path) {
        checkNotNullParam("path", path);
        return Files.notExists(path, NO_FOLLOW_LINK_OPTION_ARRAY);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#notExists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are not followed, must be not null, not
     *        empty, and a valid path
     * @return true if path is absent
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean notExistsNoFollowLinks(final String path) {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return Files.notExists(aPath, NO_FOLLOW_LINK_OPTION_ARRAY);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#notExists(Path, LinkOption...)}.
     *
     * @param path to be checked, links are not followed, must be not null and a
     *        valid path
     * @return true if path is absent
     * @throws IllegalArgumentException if the path is {@code null}
     */
    public static boolean notExistsNoFollowLinks(final URI path) {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return Files.notExists(aPath, NO_FOLLOW_LINK_OPTION_ARRAY);
    }

    /**
     * A fluent style existence check, returning the passed argument if path exists.
     * If <code>path</code> is a link, it will be followed. Check is done exactly as
     * {@link java.nio.file.Files#exists(Path, LinkOption...)} but with explicit
     * null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are followed, must be not null
     * @return the passed path if exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is absent or determination of
     *         presence failed
     */
    public static Path checkExistsFollowLinks(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckExistsFollowLinks(path);
    }

    /**
     * A fluent style existence check, returning the passed argument if path exists.
     * If <code>path</code> is a link, it will be followed. Check is done exactly as
     * {@link java.nio.file.Files#exists(Path, LinkOption...)} but with explicit
     * null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are followed, must be not null, not empty,
     *        and a valid path
     * @return the passed path if exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is absent or determination of
     *         presence failed
     */
    public static Path checkExistsFollowLinks(final String path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckExistsFollowLinks(aPath);
    }

    /**
     * A fluent style existence check, returning the passed argument if path exists.
     * If <code>path</code> is a link, it will be followed. Check is done exactly as
     * {@link java.nio.file.Files#exists(Path, LinkOption...)} but with explicit
     * null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are followed, must be not null and a valid
     *        path
     * @return the passed path if exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is absent or determination of
     *         presence failed
     */
    public static Path checkExistsFollowLinks(final URI path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckExistsFollowLinks(aPath);
    }

    static Path doCheckExistsFollowLinks(final Path path) throws UncheckedIOException {
        try {
            path.getFileSystem().provider().checkAccess(path);
        } catch (final IOException e) {
            // does not exist or unable to determine if file exists
            throw new UncheckedIOException(e);
        }
        return path;
    }

    /**
     * A fluent style existence check, returning the passed argument if path exists.
     * If <code>path</code> is a link, it will be not followed. Check is done
     * exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are not followed, must be not null
     * @return the passed path if exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is absent or determination of
     *         presence failed
     */
    public static Path checkExistsNoFollowLinks(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckExistsNoFollowLinks(path);
    }

    /**
     * A fluent style existence check, returning the passed argument if path exists.
     * If <code>path</code> is a link, it will be not followed. Check is done
     * exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are not followed, must be not null, not
     *        empty, and a valid path
     * @return the passed path if exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is absent or determination of
     *         presence failed
     */
    public static Path checkExistsNoFollowLinks(final String path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckExistsNoFollowLinks(aPath);
    }

    /**
     * A fluent style existence check, returning the passed argument if path exists.
     * If <code>path</code> is a link, it will be not followed. Check is done
     * exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are not followed, must be not null and a
     *        valid path
     * @return the passed path if exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is absent or determination of
     *         presence failed
     */
    public static Path checkExistsNoFollowLinks(final URI path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckExistsNoFollowLinks(aPath);
    }

    static Path doCheckExistsNoFollowLinks(final Path path) throws UncheckedIOException {
        try {
            // attempt to read attributes without following links
            Files.readAttributes(path, BasicFileAttributes.class, NO_FOLLOW_LINK_OPTION_ARRAY);
        } catch (final IOException e) {
            // does not exist or unable to determine if file exists
            throw new UncheckedIOException(e);
        }
        return path;
    }

    /**
     * A fluent style absence check, returning the passed argument if path not
     * exists. If <code>path</code> is a link, it will be followed. Check is done
     * exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are followed, must be not null
     * @return the passed path if not exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is present or determination of
     *         presence failed
     */
    public static Path checkNotExistsFollowLinks(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckNotExistsFollowLinks(path);
    }

    /**
     * A fluent style absence check, returning the passed argument if path not
     * exists. If <code>path</code> is a link, it will be followed. Check is done
     * exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are followed, must be not null, not empty,
     *        and a valid path
     * @return the passed path if not exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is present or determination of
     *         presence failed
     */
    public static Path checkNotExistsFollowLinks(final String path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckNotExistsFollowLinks(aPath);
    }

    /**
     * A fluent style absence check, returning the passed argument if path not
     * exists. If <code>path</code> is a link, it will be followed. Check is done
     * exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are followed, must be not null and a valid
     *        path
     * @return the passed path if not exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is present or determination of
     *         presence failed
     */
    public static Path checkNotExistsFollowLinks(final URI path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckNotExistsFollowLinks(aPath);
    }

    static Path doCheckNotExistsFollowLinks(final Path path) throws UncheckedIOException {
        try {
            path.getFileSystem().provider().checkAccess(path);
            // file exists
            throw new UncheckedIOException(
                    new FileAlreadyExistsException(path.toString(), null, "path must not exist"));
        } catch (final NoSuchFileException x) {
            // file confirmed not to exist
            return path;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A fluent style absence check, returning the passed argument if path not
     * exists. If <code>path</code> is a link, it will be not followed. Check is
     * done exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are not followed, must be not null
     * @return the passed path if not exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is present or determination of
     *         presence failed
     */
    public static Path checkNotExistsNoFollowLinks(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckNotExistsNoFollowLinks(path);
    }

    /**
     * A fluent style absence check, returning the passed argument if path not
     * exists. If <code>path</code> is a link, it will be not followed. Check is
     * done exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are not followed, must be not null, not
     *        empty, and a valid path
     * @return the passed path if not exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is present or determination of
     *         presence failed
     */
    public static Path checkNotExistsNoFollowLinks(final String path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckNotExistsNoFollowLinks(aPath);
    }

    /**
     * A fluent style absence check, returning the passed argument if path not
     * exists. If <code>path</code> is a link, it will be not followed. Check is
     * done exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to be checked, links are not followed, must be not null and a
     *        valid path
     * @return the passed path if not exists
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if path is present or determination of
     *         presence failed
     */
    public static Path checkNotExistsNoFollowLinks(final URI path) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckNotExistsNoFollowLinks(aPath);
    }

    static Path doCheckNotExistsNoFollowLinks(final Path path) throws UncheckedIOException {
        try {
            Files.readAttributes(path, BasicFileAttributes.class, NO_FOLLOW_LINK_OPTION_ARRAY);
            // file exists
            throw new UncheckedIOException(
                    new FileAlreadyExistsException(path.toString(), null, "path must not exist"));
        } catch (final NoSuchFileException x) {
            // file confirmed not to exist
            return path;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A wrapper around {@link java.nio.file.Files#size(Path)}.
     *
     * @param path to calc, must be not null
     * @return the size in bytes
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static long size(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doSize(path);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#size(Path)}.
     *
     * @param path to calc, must be not null, not empty, and a valid path
     * @return the size in bytes
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static long size(final String path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doSize(aPath);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#size(Path)}.
     *
     * @param path to calc, must be not null and a valid path
     * @return the size in bytes
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static long size(final URI path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doSize(aPath);
    }

    static long doSize(final Path path) throws UncheckedIOException {
        try {
            return Files.size(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Path doCheckAccess(final Path path, final AccessMode... modes) throws UncheckedIOException {
        Assert.checkNotEmptyParam("modes", modes);
        try {
            path.getFileSystem().provider().checkAccess(path, modes);
            return path;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A fluent style read access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isReadable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null
     * @return the passed path if read access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no read access or determination of access
     *         rights fails
     */
    public static Path checkIsReadable(final Path path) throws UncheckedIOException {
        checkNotNullParam("path", path);
        return doCheckAccess(path, AccessMode.READ);
    }

    /**
     * A fluent style read access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isReadable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null, not empty and a valid path
     * @return the passed path if read access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no read access or determination of access
     *         rights fails
     */
    public static Path checkIsReadable(final String path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckAccess(aPath, AccessMode.READ);
    }

    /**
     * A fluent style read access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isReadable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null and a valid path
     * @return the passed path if read access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no read access or determination of access
     *         rights fails
     */
    public static Path checkIsReadable(final URI path) throws UncheckedIOException {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckAccess(aPath, AccessMode.READ);
    }

    /**
     * A fluent style write access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isWritable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null
     * @return the passed path if write access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no write access or determination of
     *         access rights fails
     */
    public static Path checkIsWritable(final Path path) {
        checkNotNullParam("path", path);
        return doCheckAccess(path, AccessMode.WRITE);
    }

    /**
     * A fluent style write access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isWritable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null, not empty, and a valid path
     * @return the passed path if write access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no write access or determination of
     *         access rights fails
     */
    public static Path checkIsWritable(final String path) {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckAccess(aPath, AccessMode.WRITE);
    }

    /**
     * A fluent style write access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isWritable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null and a valid path
     * @return the passed path if write access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no write access or determination of
     *         access rights fails
     */
    public static Path checkIsWritable(final URI path) {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckAccess(aPath, AccessMode.WRITE);
    }

    /**
     * A fluent style execute access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isExecutable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null
     * @return the passed path if execute access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no execute access or determination of
     *         access rights fails
     */
    public static Path checkIsExecutable(final Path path) {
        checkNotNullParam("path", path);
        return doCheckAccess(path, AccessMode.EXECUTE);
    }

    /**
     * A fluent style execute access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isExecutable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null, not empty, and a valid path
     * @return the passed path if execute access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no execute access or determination of
     *         access rights fails
     */
    public static Path checkIsExecutable(final String path) {
        Path aPath = Paths.get(checkNotEmptyAfterTrimParam("path", path));
        return doCheckAccess(aPath, AccessMode.EXECUTE);
    }

    /**
     * A fluent style execute access check, returning the passed argument. Check is
     * done exactly as {@link java.nio.file.Files#isExecutable(Path)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null and a valid path
     * @return the passed path if execute access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no execute access or determination of
     *         access rights fails
     */
    public static Path checkIsExecutable(final URI path) {
        Path aPath = Paths.get(checkNotNullParam("path", path));
        return doCheckAccess(aPath, AccessMode.EXECUTE);
    }

    /**
     * Checks if a directory contains no elements.
     *
     * @param dir to check, must not be null
     * @return the passed dir if it is empty
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException if directory contains elements or check
     *         fails
     */
    public static Path checkIsEmptyDirectory(final Path dir) throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        return doCheckIsEmptyDirectory(dir);
    }

    /**
     * Checks if a directory contains no elements.
     *
     * @param dir to check, must not be null, not empty and a valid directory
     * @return the passed dir if it is empty
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException if directory contains elements or check
     *         fails
     */
    public static Path checkIsEmptyDirectory(final String dir) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotEmptyAfterTrimParam("dir", dir));
        return doCheckIsEmptyDirectory(aPath);
    }

    /**
     * Checks if a directory contains no elements.
     *
     * @param dir to check, must be not null and a valid directory
     * @return the passed dir if it is empty
     * @throws IllegalArgumentException if the dir is {@code null}
     * @throws UncheckedIOException if directory contains elements or check
     *         fails
     */
    public static Path checkIsEmptyDirectory(final URI dir) throws UncheckedIOException {
        final Path aPath = Paths.get(checkNotNullParam("dir", dir));
        return doCheckIsEmptyDirectory(aPath);
    }

    static Path doCheckIsEmptyDirectory(final Path dir) throws UncheckedIOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            if (!directoryStream.iterator().hasNext()) {
                return dir;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new UncheckedIOException(new DirectoryNotEmptyException(dir.toString()));
    }

    /**
     * Obtains the present working directory with
     * {@code System.getProperty("user.dir")}
     *
     * @return a valid directory pointing to the present working directory.
     * @throws IllegalArgumentException if the property is {@code null} or empty
     * @throws UncheckedIOException if not a directory or check fails
     */
    public static Path getPresentWorkingDirectory() throws UncheckedIOException {
        String prop = System.getProperty("user.dir");
        return checkIsDirectoryFollowLinks(prop);
    }

    /**
     * No instance.
     */
    private PathUtils() {
    }

}
