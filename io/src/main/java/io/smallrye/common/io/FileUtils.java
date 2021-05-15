package io.smallrye.common.io;

import static io.smallrye.common.constraint.Assert.checkNotEmptyAfterTrimParam;
import static io.smallrye.common.constraint.Assert.checkNotNullParam;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

/**
 * Utilities for {@link java.nio.file.Files} using classic
 * <code>java.io.File</code> as parameter. Different to parameter overload in
 * PathUtils the return value is File, too. Please prefer a full migration to
 * java.nio. This is for legacy convenience only.
 *
 * @author Boris Unckel
 *
 */
public final class FileUtils {

    /**
     * A wrapper around {@link java.nio.file.Paths#get(String, String...)} with
     * explicit null check.
     *
     * @param first part of the path, must be not null, not empty and a valid path
     * @param more optional parts of the path
     * @return the path constructed out of first and more
     * @throws IllegalArgumentException if first is null or empty
     */
    public static File get(final String first, final String... more) {
        return Paths.get(checkNotEmptyAfterTrimParam("first", first), PathUtils.checkElementsNotEmpty(more)).toFile();
    }

    /**
     * A wrapper around {@link java.nio.file.Paths#get(URI)} with explicit null
     * check.
     *
     * @param uri for the path, must be not null and a valid path
     * @return the path constructed out of the uri
     * @throws IllegalArgumentException if uri is null
     */
    public static File get(final URI uri) {
        return Paths.get(checkNotNullParam("uri", uri)).toFile();
    }

    /**
     * A wrapper around
     * {@link java.nio.file.Files#createFile(Path, FileAttribute...)} with explicit
     * null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file of the file to create, must be not null
     * @param attrs optional attributes of the file to create
     * @return the created file
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static File createFile(final File file, final FileAttribute<?>... attrs) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCreateFile(file.toPath(), attrs).toFile();
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
    public static File createDirectory(final File dir, final FileAttribute<?>... attrs) throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        return PathUtils.doCreateDirectory(dir.toPath(), attrs).toFile();
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
    public static File createDirectories(File dir, FileAttribute<?>... attrs) throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        return PathUtils.doCreateDirectories(dir.toPath(), attrs).toFile();
    }

    /**
     * Create a new file if not exists or update the last modified time to now.
     *
     * @param file to create, must be not null
     * @return the created or updated file
     * @throws IllegalArgumentException if file is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static File touch(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doTouch(file.toPath()).toFile();
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
    public static File createSymbolicLink(final File link, final File target, final FileAttribute<?>... attrs)
            throws UncheckedIOException {
        checkNotNullParam("link", link);
        checkNotNullParam("target", target);
        return PathUtils.doCreateSymbolicLink(link.toPath(), target.toPath(), attrs).toFile();
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
    public static File createLink(final File link, final File target) throws UncheckedIOException {
        checkNotNullParam("link", link);
        checkNotNullParam("target", target);
        return PathUtils.doCreateLink(link.toPath(), target.toPath()).toFile();
    }

    /**
     * A wrapper around {@link java.nio.file.Files#delete(Path)} with explicit null
     * check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to delete, must be not null
     * @return the deleted path
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static File delete(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doDelete(file.toPath()).toFile();
    }

    /**
     * A wrapper around {@link java.nio.file.Files#deleteIfExists(Path)} with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to delete, must be not null
     * @return the deleted path
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static File deleteIfExists(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doDeleteIfExists(file.toPath()).toFile();
    }

    /**
     * Recursive deletion of path. If path is a directory it will cleaned, including
     * subfolders and deleted itself.
     * <p>
     * Adopted from Google Guava <a href=
     * "https://github.com/google/guava/blob/master/guava/src/com/google/common/io/MoreFiles.java">com.google.common.io.MoreFiles</a>.
     * </p>
     *
     * @param file to delete, must be not null, and exists.
     * @return the path to delete
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException around original IOExceptions and if not
     *         exists path
     */
    public static File deleteRecursively(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doDeleteRecursively(PathUtils.doCheckExistsNoFollowLinks(file.toPath())).toFile();
    }

    /**
     * Recursive deletion of directories content, including subfolders.
     * <p>
     * Adopted from Google Guava <a href=
     * "https://github.com/google/guava/blob/master/guava/src/com/google/common/io/MoreFiles.java">com.google.common.io.MoreFiles</a>.
     * </p>
     *
     * @param file to delete, must be not null, a path, and exists.
     * @return the path to delete
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException around original IOExceptions and if not
     *         exists path or is not directory
     */
    public static File deleteDirectoryContents(File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doDeleteDirectoryContents(PathUtils.doCheckIsDirectoryNoFollowLinks(file.toPath())).toFile();
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
    public static File copy(final File source, final File target, final CopyOption... options)
            throws UncheckedIOException {
        checkNotNullParam("source", source);
        checkNotNullParam("target", target);
        return PathUtils.doCopy(source.toPath(), target.toPath(), options).toFile();
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
    public static File move(final File source, final File target, final CopyOption... options)
            throws UncheckedIOException {
        checkNotNullParam("source", source);
        checkNotNullParam("target", target);
        return PathUtils.doMove(source.toPath(), target.toPath(), options).toFile();
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
    public static File readSymbolicLink(final File link) throws UncheckedIOException {
        checkNotNullParam("link", link);
        return PathUtils.doReadSymbolicLink(link.toPath()).toFile();
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
    public static boolean isSameFile(final File file, final File file2) throws UncheckedIOException {
        checkNotNullParam("file", file);
        checkNotNullParam("file2", file2);
        return PathUtils.doIsSameFile(file.toPath(), file2.toPath());
    }

    /**
     * A fluent style symbolic link check, returning the passed argument if path is
     * a symbolic link. Check is done exactly as
     * {@link java.nio.file.Files#isSymbolicLink(Path)} but with explicit null check
     * and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, must be not null, not empty, and a valid path
     * @return the passed file if it is a symbolic link
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException if is not a link or determination of type
     *         fails
     */
    public static File checkIsSymbolicLink(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckIsSymbolicLink(file.toPath()).toFile();
    }

    /**
     * A fluent style directory check, following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, must be not null
     * @return the passed file if it is a directory
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a directory or determination of
     *         type fails
     */
    public static File checkIsDirectoryFollowLinks(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckIsDirectoryFollowLinks(file.toPath()).toFile();
    }

    /**
     * A fluent style directory check, not following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, must be not null
     * @return the passed file if it is a directory
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a directory or determination of
     *         type fails
     */
    public static File checkIsDirectoryNoFollowLinks(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckIsDirectoryNoFollowLinks(file.toPath()).toFile();
    }

    /**
     * A fluent style regular file check, following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, must be not null
     * @return the passed file if it is a regular file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a regular file or determination of
     *         type fails
     */
    public static File checkIsRegularFileFollowLinks(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckIsRegularFileFollowLinks(file.toPath()).toFile();
    }

    /**
     * A fluent style regular file check, not following links, returning the passed
     * argument if path is a directory. Check is done exactly as
     * {@link java.nio.file.Files#isDirectory(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, must be not null
     * @return the passed path if it is a regular file
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if is not a regular file or determination of
     *         type fails
     */
    public static File checkIsRegularFileNoFollowLinks(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckIsRegularFileNoFollowLinks(file.toPath()).toFile();
    }

    /**
     * A wrapper around {@link java.nio.file.Files#exists(Path, LinkOption...)}.
     *
     * @param file to be checked, links are followed, must be not null
     * @return true if file is present
     * @throws IllegalArgumentException if the file is {@code null}
     */
    public static boolean existsFollowLinks(final File file) {
        checkNotNullParam("file", file);
        return Files.exists(file.toPath());
    }

    /**
     * A wrapper around {@link java.nio.file.Files#exists(Path, LinkOption...)}.
     *
     * @param file to be checked, links are not followed, must be not null
     * @return true if file is present
     * @throws IllegalArgumentException if the file is {@code null}
     */
    public static boolean existsNoFollowLinks(final File file) {
        checkNotNullParam("file", file);
        return Files.exists(file.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY);
    }

    /**
     * A wrapper around {@link java.nio.file.Files#notExists(Path, LinkOption...)}.
     *
     * @param file to be checked, links are followed, must be not null
     * @return true if file is absent
     * @throws IllegalArgumentException if the file is {@code null}
     */
    public static boolean notExistsFollowLinks(final File file) {
        checkNotNullParam("file", file);
        return Files.notExists(file.toPath());
    }

    /**
     * A wrapper around {@link java.nio.file.Files#notExists(Path, LinkOption...)}.
     *
     * @param file to be checked, links are not followed, must be not null
     * @return true if file is absent
     * @throws IllegalArgumentException if the file is {@code null}
     */
    public static boolean notExistsNoFollowLinks(final File file) {
        checkNotNullParam("file", file);
        return Files.notExists(file.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY);
    }

    /**
     * A fluent style existence check, returning the passed argument if path exists.
     * If <code>path</code> is a link, it will be followed. Check is done exactly as
     * {@link java.nio.file.Files#exists(Path, LinkOption...)} but with explicit
     * null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, links are followed, must be not null
     * @return the passed file if exists
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException if path is absent or determination of
     *         presence failed
     */
    public static File checkExistsFollowLinks(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckExistsFollowLinks(file.toPath()).toFile();
    }

    /**
     * A fluent style existence check, returning the passed argument if path exists.
     * If <code>path</code> is a link, it will be not followed. Check is done
     * exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, links are not followed, must be not null
     * @return the passed file if exists
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException if path is absent or determination of
     *         presence failed
     */
    public static File checkExistsNoFollowLinks(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckExistsNoFollowLinks(file.toPath()).toFile();
    }

    /**
     * A fluent style absence check, returning the passed argument if path not
     * exists. If <code>path</code> is a link, it will be followed. Check is done
     * exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but with
     * explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, links are followed, must be not null
     * @return the passed file if not exists
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException if path is present or determination of
     *         presence failed
     */
    public static File checkNotExistsFollowLinks(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckNotExistsFollowLinks(file.toPath()).toFile();
    }

    /**
     * A fluent style absence check, returning the passed argument if path not
     * exists. If <code>path</code> is a link, it will be not followed. Check is
     * done exactly as {@link java.nio.file.Files#exists(Path, LinkOption...)} but
     * with explicit null check and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param file to be checked, links are not followed, must be not null
     * @return the passed file if not exists
     * @throws IllegalArgumentException if the file is {@code null}
     * @throws UncheckedIOException if path is present or determination of
     *         presence failed
     */
    public static File checkNotExistsNoFollowLinks(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckNotExistsNoFollowLinks(file.toPath()).toFile();
    }

    /**
     * A wrapper around {@link java.nio.file.Files#size(Path)}.
     *
     * @param path to calc, must be not null
     * @return the size in bytes
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException around original IOExceptions
     */
    public static long size(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doSize(file.toPath());
    }

    /**
     * A fluent style read access check, returning the passed argument if path is a
     * directory. Check is done exactly as
     * {@link java.nio.file.Files#isReadable(Path)} but with explicit null check and
     * unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null
     * @return the passed path if read access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no read access or determination of access
     *         rights fails
     */
    public static File checkIsReadable(final File file) throws UncheckedIOException {
        checkNotNullParam("file", file);
        return PathUtils.doCheckAccess(file.toPath(), AccessMode.READ).toFile();
    }

    /**
     * A fluent style write access check, returning the passed argument if path is a
     * directory. Check is done exactly as
     * {@link java.nio.file.Files#isWritable(Path)} but with explicit null check and
     * unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null
     * @return the passed path if write access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no write access or determination of
     *         access rights fails
     */
    public static File checkIsWritable(final File file) {
        checkNotNullParam("file", file);
        return PathUtils.doCheckAccess(file.toPath(), AccessMode.WRITE).toFile();
    }

    /**
     * A fluent style execute access check, returning the passed argument if path is
     * a directory. Check is done exactly as
     * {@link java.nio.file.Files#isExecutable(Path)} but with explicit null check
     * and unchecked {@code java.io.UncheckedIOException}.
     *
     * @param path to check, must be not null
     * @return the passed path if execute access is possible
     * @throws IllegalArgumentException if the path is {@code null}
     * @throws UncheckedIOException if no execute access or determination of
     *         access rights fails
     */
    public static File checkIsExecutable(final File file) {
        checkNotNullParam("file", file);
        return PathUtils.doCheckAccess(file.toPath(), AccessMode.EXECUTE).toFile();
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
    public static File checkIsEmptyDirectory(final File dir) throws UncheckedIOException {
        checkNotNullParam("dir", dir);
        return PathUtils.doCheckIsEmptyDirectory(dir.toPath()).toFile();
    }

    /**
     * Obtains the present working directory with
     * {@code System.getProperty("user.dir")}
     *
     * @return a valid directory pointing to the present working directory.
     * @throws IllegalArgumentException if the property is {@code null} or empty
     * @throws UncheckedIOException if not a directory or check fails
     */
    public static File getPresentWorkingDirectory() throws UncheckedIOException {
        return PathUtils.getPresentWorkingDirectory().toFile();
    }

    private FileUtils() {
    }

}
