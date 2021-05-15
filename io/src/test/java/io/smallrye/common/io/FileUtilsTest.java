package io.smallrye.common.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilsTest {

    private PathUtilsTest.TemporaryFolder basePath = null;

    @TempDir
    public Path tempPath;

    @BeforeEach
    void init() {
        Objects.requireNonNull(tempPath);
        assertTrue(Files.exists(tempPath, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        this.basePath = new PathUtilsTest.TemporaryFolder(tempPath);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#get(java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testGetStringStringArray() throws IOException {
        // valid
        File actPath = null;
        actPath = FileUtils.get(basePath.getRoot().toString());
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), basePath.getRoot().toPath()));

        actPath = null;
        actPath = FileUtils.get(basePath.getRoot().toString(), "second");
        assertNotNull(actPath);

        // invalid
        Exception actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.get("notA \0 path");
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof java.nio.file.InvalidPathException);
        assertNull(actPath);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.get((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
        assertNull(actPath);
    }

    /**
     * Test method for {@link org.wildfly.common.io.FileUtils#get(java.net.URI)}.
     */
    @Test
    public void testGetURI() throws IOException {
        // valid
        File actPath = null;
        actPath = FileUtils.get(basePath.getRoot().toURI());
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), basePath.getRoot().toPath()));

        // invalid
        Exception actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.get(new URI("http://notAPath.com"));
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof java.nio.file.FileSystemNotFoundException);
        assertNull(actPath);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.get((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
        assertNull(actPath);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#createFile(java.io.File,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateFile() throws IOException {
        File toCreate = Paths.get(basePath.getRoot().toString(), "testFile.tmp").toFile();
        File actual = FileUtils.createFile(toCreate);
        assertTrue(Files.exists(toCreate.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate.toPath(), actual.toPath()));

        Exception actEx = null;
        try {
            FileUtils.createFile((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#createDirectory(java.io.File,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateDirectory() throws IOException {
        File toCreate = Paths.get(basePath.getRoot().toString(), "testSubFolder").toFile();
        File actual = FileUtils.createDirectory(toCreate);
        assertTrue(Files.exists(toCreate.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate.toPath(), actual.toPath()));

        Exception actEx = null;
        try {
            FileUtils.createDirectory((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#createDirectories(java.io.File,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateDirectories() throws IOException {
        File toCreate = Paths.get(basePath.getRoot().toString(), "testSubFolder1", "testSubFolder2").toFile();
        File actual = FileUtils.createDirectories(toCreate);
        assertTrue(Files.exists(toCreate.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate.toPath(), actual.toPath()));

        Exception actEx = null;
        try {
            FileUtils.createDirectories((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for {@link org.wildfly.common.io.FileUtils#touch(java.io.File)}.
     */
    @Test
    public void testTouch() throws IOException {
        File toCreate = Paths.get(basePath.getRoot().toString(), "testFile.tmp").toFile();
        File actual = FileUtils.touch(toCreate);
        assertTrue(Files.exists(toCreate.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate.toPath(), actual.toPath()));

        Exception actEx = null;
        try {
            FileUtils.touch((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#createSymbolicLink(java.io.File,
     * java.io.File, java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateSymbolicLink() throws IOException {
        File target = basePath.newFile();
        File link = Paths.get(basePath.getRoot().toString(), "linkToTarget").toFile();
        File actual = FileUtils.createSymbolicLink(link, target);
        assertTrue(Files.exists(actual.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(actual.toPath()));
        assertTrue(Files.isSameFile(link.toPath(), actual.toPath()));

        Exception actEx = null;
        try {
            FileUtils.createSymbolicLink((File) null, target);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            FileUtils.createSymbolicLink(link, (File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#createLink(java.io.File, java.io.File)}.
     */
    @Test
    public void testCreateLink() throws IOException {
        File target = basePath.newFile();
        File link = Paths.get(basePath.getRoot().toString(), "linkToTarget").toFile();
        File actual = FileUtils.createLink(link, target);
        assertTrue(Files.exists(actual.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(actual.toPath()));
        assertTrue(Files.isSameFile(link.toPath(), actual.toPath()));

        Exception actEx = null;
        try {
            FileUtils.createLink((File) null, target);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            FileUtils.createLink(link, (File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for {@link org.wildfly.common.io.FileUtils#delete(java.io.File)}.
     */
    @Test
    public void testDelete() throws IOException {
        File toDeleteFolder = basePath.newFolder("subFolder");
        File toDeleteFile = basePath.newFile();
        File notExists = Paths.get(basePath.toString(), "notExist").toFile();

        File actualPath = FileUtils.delete(toDeleteFolder);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFolder.toPath(), actualPath.toPath()));
        assertTrue(Files.notExists(toDeleteFolder.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        actualPath = null;
        actualPath = FileUtils.delete(toDeleteFile);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFile.toPath(), actualPath.toPath()));
        assertTrue(Files.notExists(toDeleteFile.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        Exception actual = null;
        actualPath = null;
        try {
            actualPath = FileUtils.delete(notExists);
        } catch (UncheckedIOException e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            FileUtils.delete((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#deleteIfExists(java.io.File)}.
     */
    @Test
    public void testDeleteIfExists() throws IOException {
        File toDeleteFolder = basePath.newFolder("subFolder");
        File toDeleteFile = basePath.newFile();
        File notExists = Paths.get(basePath.toString(), "notExist").toFile();
        FileUtils.deleteIfExists(toDeleteFolder);
        FileUtils.deleteIfExists(toDeleteFile);
        Exception actual = null;
        try {
            FileUtils.deleteIfExists(notExists);
        } catch (UncheckedIOException e) {
            actual = e;
        }
        assertNull(actual);
        assertTrue(Files.notExists(toDeleteFolder.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFile.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        Exception actEx = null;
        try {
            FileUtils.deleteIfExists((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#deleteDirectoryContents(File file)}.
     */
    @Test
    public void testdeleteDirectoryContentsPath() throws IOException {
        File toDeleteFolder = basePath.newFolder("subFolder");
        File toDeleteFile = null;
        File notExists = Paths.get(basePath.toString(), "notExist").toFile();

        toDeleteFile = Files.createTempFile(toDeleteFolder.toPath(), "test", "tmp").toFile();
        File actualPath = FileUtils.deleteDirectoryContents(toDeleteFolder);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFolder.toPath(), actualPath.toPath()));
        assertTrue(Files.exists(toDeleteFolder.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFile.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        toDeleteFile = Files.createTempFile(toDeleteFolder.toPath(), "test", "tmp").toFile();
        Exception actual = null;
        actualPath = null;
        try {
            actualPath = FileUtils.deleteDirectoryContents(toDeleteFile);
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NotDirectoryException);
        assertNull(actualPath);

        toDeleteFile = Files.createTempFile(toDeleteFolder.toPath(), "test", "tmp").toFile();
        actual = null;
        actualPath = null;
        try {
            actualPath = FileUtils.deleteDirectoryContents(notExists);
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            FileUtils.deleteDirectoryContents((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#deleteRecursively(File file)}.
     */
    @Test
    public void testdeleteRecursivelyPath() throws IOException {
        File toDeleteFolder = null;
        File toDeleteFile = null;
        File notExists = Paths.get(basePath.toString(), "notExist").toFile();

        toDeleteFolder = basePath.newFolder("subFolder");
        toDeleteFile = Files.createTempFile(toDeleteFolder.toPath(), "test", "tmp").toFile();
        File actualPath = FileUtils.deleteRecursively(toDeleteFolder);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFolder.toPath(), actualPath.toPath()));
        assertTrue(Files.notExists(toDeleteFile.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFolder.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        toDeleteFolder = basePath.newFolder("subFolder");
        toDeleteFile = Files.createTempFile(toDeleteFolder.toPath(), "test", "tmp").toFile();
        Exception actual = null;
        actualPath = null;
        try {
            actualPath = FileUtils.deleteRecursively(toDeleteFile);
        } catch (Exception e) {
            actual = e;
        }
        assertNull(actual);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFile.toPath(), actualPath.toPath()));
        assertTrue(Files.notExists(toDeleteFile.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(toDeleteFolder.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        actual = null;
        actualPath = null;
        try {
            actualPath = FileUtils.deleteRecursively(notExists);
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.deleteRecursively((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
        assertNull(actualPath);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#copy(java.io.File, java.io.File, java.nio.file.CopyOption[])}.
     */
    @Test
    public void testCopy() throws IOException {
        File source = basePath.newFile();
        File target = Paths.get(basePath.getRoot().toString(), "testTarget.tmp").toFile();
        File actual = FileUtils.copy(source, target);
        assertTrue(Files.exists(actual.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(target.toPath(), actual.toPath()));

        Exception actEx = null;
        try {
            FileUtils.copy((File) null, target);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            FileUtils.copy(source, (File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#move(java.io.File, java.io.File, java.nio.file.CopyOption[])}.
     */
    @Test
    public void testMove() throws IOException {
        File source = basePath.newFile();
        File target = Paths.get(basePath.getRoot().toString(), "testTarget.tmp").toFile();
        File actual = FileUtils.move(source, target);
        assertTrue(Files.exists(actual.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(source.toPath(), PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(target.toPath(), actual.toPath()));

        Exception actEx = null;
        try {
            FileUtils.move((File) null, target);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            FileUtils.move(source, (File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#readSymbolicLink(java.io.File)}.
     */
    @Test
    public void testReadSymbolicLink() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        Exception actEx = null;
        File actualPath = null;
        try {
            actualPath = FileUtils.readSymbolicLink(doesExist);
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotLinkException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.readSymbolicLink(symbolicLinkToExist);
        } catch (final Exception e) {
            actEx = e;
        }
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(actualPath.toPath(), doesExist.toPath()));
        assertNull(actEx);

        actEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.readSymbolicLink(doesNotExist);
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.readSymbolicLink(tempFile);
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.readSymbolicLink(symbolicLinkToDeleted);
        } catch (final Exception e) {
            actEx = e;
        }
        // This is right - readSymbolicLink does not check target existence.
        assertNotNull(actualPath);
        assertNull(actEx);

        actEx = null;
        try {
            FileUtils.readSymbolicLink((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#isSameFile(java.io.File, java.io.File)}.
     */
    @Test
    public void testIsSameFile() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        File doesExistSecondRef = Paths.get(doesExist.toString()).toFile();
        assertTrue(FileUtils.isSameFile(doesExist, doesExist));
        assertTrue(FileUtils.isSameFile(doesExist, doesExistSecondRef));
        assertTrue(FileUtils.isSameFile(doesExist, symbolicLinkToExist));

        File doesNotExistSecondRef = Paths.get(doesNotExist.toString()).toFile();
        assertTrue(FileUtils.isSameFile(doesNotExist, doesNotExist));
        assertTrue(FileUtils.isSameFile(doesNotExist, doesNotExistSecondRef));

        File tempFileSecondRef = Paths.get(tempFile.toString()).toFile();
        assertTrue(FileUtils.isSameFile(tempFile, tempFile));
        assertTrue(FileUtils.isSameFile(tempFile, tempFileSecondRef));

        Exception actEx = null;
        try {
            FileUtils.isSameFile(tempFile, symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        try {
            FileUtils.isSameFile(tempFile, (File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            FileUtils.isSameFile((File) null, tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkIsSymbolicLink(java.io.File)}.
     */
    @Test
    public void testCheckIsSymbolicLink() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        File actPath = null;
        Exception actEx = null;
        try {
            actPath = FileUtils.checkIsSymbolicLink(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotLinkException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsSymbolicLink(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), symbolicLinkToExist.toPath()));

        actPath = null;
        actEx = null;
        try {
            FileUtils.checkIsSymbolicLink(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsSymbolicLink(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath.toPath(), symbolicLinkToDeleted.toPath()));

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsSymbolicLink((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkIsDirectoryFollowLinks(java.io.File)}.
     */
    @Test
    public void testCheckIsDirectoryFollowLinks() throws IOException {
        File doesExist = basePath.newFolder();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExistsFolder").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFolder();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDeleteFolder").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        File actPath = null;
        Exception actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryFollowLinks(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath.toPath(), doesExist.toPath()));

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath.toPath(), doesExist.toPath()));

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryFollowLinks(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryFollowLinks(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkIsDirectoryNoFollowLinks(java.io.File)}.
     */
    @Test
    public void testCheckIsDirectoryNoFollowLinks() throws IOException {
        File doesExist = basePath.newFolder();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExistsFolder").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFolder();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDeleteFolder").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        File actPath = null;
        Exception actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryNoFollowLinks(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath.toPath(), doesExist.toPath()));

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryNoFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotDirectoryException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryNoFollowLinks(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryNoFollowLinks(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryNoFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotDirectoryException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsDirectoryNoFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkIsRegularFileFollowLinks(java.io.File)}.
     */
    @Test
    public void testCheckIsRegularFileFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File doesExistDirectory = basePath.newFolder();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        File actPath = null;
        Exception actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileFollowLinks(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath.toPath(), doesExist.toPath()));

        actPath = null;
        actEx = null;
        try {
            FileUtils.checkIsRegularFileFollowLinks(doesExistDirectory);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), symbolicLinkToExist.toPath()));

        actPath = null;
        actEx = null;
        try {
            FileUtils.checkIsRegularFileFollowLinks(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkIsRegularFileNoFollowLinks(java.io.File)}.
     */
    @Test
    public void testCheckIsRegularFileNoFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File doesExistDirectory = basePath.newFolder();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        File actPath = null;
        Exception actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileNoFollowLinks(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath.toPath(), doesExist.toPath()));

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileNoFollowLinks(doesExistDirectory);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileNoFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileNoFollowLinks(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileNoFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = FileUtils.checkIsRegularFileNoFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#existsFollowLinks(java.io.File)}.
     */
    @Test
    public void testExistsFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        assertTrue(FileUtils.existsFollowLinks(doesExist));
        assertTrue(FileUtils.existsFollowLinks(symbolicLinkToExist));
        assertFalse(FileUtils.existsFollowLinks(doesNotExist));
        assertFalse(FileUtils.existsFollowLinks(tempFile));
        assertFalse(FileUtils.existsFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            FileUtils.existsFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#existsNoFollowLinks(java.io.File)}.
     */
    @Test
    public void testExistsNoFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        assertTrue(FileUtils.existsNoFollowLinks(doesExist));
        assertTrue(FileUtils.existsNoFollowLinks(symbolicLinkToExist));
        assertFalse(FileUtils.existsNoFollowLinks(doesNotExist));
        assertFalse(FileUtils.existsNoFollowLinks(tempFile));
        assertTrue(FileUtils.existsNoFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            FileUtils.existsNoFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#notExistsFollowLinks(java.io.File)}.
     */
    @Test
    public void testNotExistsFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        assertFalse(FileUtils.notExistsFollowLinks(doesExist));
        assertFalse(FileUtils.notExistsFollowLinks(symbolicLinkToExist));
        assertTrue(FileUtils.notExistsFollowLinks(doesNotExist));
        assertTrue(FileUtils.notExistsFollowLinks(tempFile));
        assertTrue(FileUtils.notExistsFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            FileUtils.notExistsFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#notExistsNoFollowLinks(java.io.File)}.
     */
    @Test
    public void testNotExistsNoFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        assertFalse(FileUtils.notExistsNoFollowLinks(doesExist));
        assertFalse(FileUtils.notExistsNoFollowLinks(symbolicLinkToExist));
        assertTrue(FileUtils.notExistsNoFollowLinks(doesNotExist));
        assertTrue(FileUtils.notExistsNoFollowLinks(tempFile));
        assertFalse(FileUtils.notExistsNoFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            FileUtils.notExistsNoFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkExistsFollowLinks(java.io.File)}.
     */
    @Test
    public void testCheckExistsFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        // same as: assertTrue(PathUtils.existsFollowLinks(doesExist));
        Exception actualEx = null;
        File actualPath = null;
        try {
            actualPath = FileUtils.checkExistsFollowLinks(doesExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), doesExist.toPath()));

        // same as: assertTrue(PathUtils.existsFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkExistsFollowLinks(symbolicLinkToExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), symbolicLinkToExist.toPath()));

        // same as: assertFalse(PathUtils.existsFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkExistsFollowLinks(doesNotExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // same as: assertFalse(PathUtils.existsFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkExistsFollowLinks(tempFile);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // same as: assertFalse(PathUtils.existsFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkExistsFollowLinks(symbolicLinkToDeleted);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            FileUtils.checkExistsFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkExistsNoFollowLinks(java.io.File)}.
     */
    @Test
    public void testCheckExistsNoFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(doesExist));
        Exception actualEx = null;
        File actualPath = null;
        try {
            actualPath = FileUtils.checkExistsNoFollowLinks(doesExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), doesExist.toPath()));

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkExistsNoFollowLinks(symbolicLinkToExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), symbolicLinkToExist.toPath()));

        // Same as: assertFalse(PathUtils.existsNoFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkExistsFollowLinks(doesNotExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.existsNoFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkExistsFollowLinks(tempFile);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkExistsNoFollowLinks(symbolicLinkToDeleted);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), symbolicLinkToDeleted.toPath()));

        Exception actEx = null;
        try {
            FileUtils.checkExistsNoFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkNotExistsFollowLinks(java.io.File)}.
     */
    @Test
    public void testCheckNotExistsFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        // Same as: assertFalse(PathUtils.notExistsFollowLinks(doesExist));
        Exception actualEx = null;
        File actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsFollowLinks(doesExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.notExistsFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsFollowLinks(symbolicLinkToExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsFollowLinks(doesNotExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), doesNotExist.toPath()));

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsFollowLinks(tempFile);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), tempFile.toPath()));

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsFollowLinks(symbolicLinkToDeleted);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), symbolicLinkToDeleted.toPath()));

        Exception actEx = null;
        try {
            FileUtils.checkNotExistsFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkNotExistsNoFollowLinks(java.io.File)}.
     */
    @Test
    public void testCheckNotExistsNoFollowLinks() throws IOException {
        File doesExist = basePath.newFile();
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        // Same as: assertFalse(PathUtils.notExistsNoFollowLinks(doesExist));
        Exception actualEx = null;
        File actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsNoFollowLinks(doesExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsNoFollowLinks(symbolicLinkToExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.notExistsNoFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsNoFollowLinks(doesNotExist);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), doesNotExist.toPath()));

        // Same as: assertTrue(PathUtils.notExistsNoFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsNoFollowLinks(tempFile);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath.toPath(), tempFile.toPath()));

        // Same as:
        // assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = FileUtils.checkNotExistsNoFollowLinks(symbolicLinkToDeleted);
        } catch (UncheckedIOException e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            FileUtils.checkNotExistsFollowLinks((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for {@link org.wildfly.common.io.FileUtils#size(java.io.File)}.
     */
    @Test
    public void testSize() throws IOException {
        File doesExist = basePath.newFile();
        final byte[] data = "Hello World".getBytes();
        Files.write(doesExist.toPath(), data);
        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExist.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        Exception actualEx = null;
        long actualSize = -1L;
        try {
            actualSize = FileUtils.size(doesExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertEquals((long) data.length, actualSize);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = FileUtils.size(symbolicLinkToExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertEquals((long) data.length, actualSize);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = FileUtils.size(doesNotExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = FileUtils.size(tempFile);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = FileUtils.size(symbolicLinkToDeleted);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = FileUtils.size((File) null);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkIsReadable(java.io.File)}.
     */
    @Test
    public void testCheckIsReadable() throws IOException {
        File doesExistAllowed = basePath.newFile();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("r--r--r--");
        Files.setPosixFilePermissions(doesExistAllowed.toPath(), pfpSet);
        File doesExistNotAllowed = basePath.newFile();
        pfpSet = PosixFilePermissions.fromString("-wx------");
        Files.setPosixFilePermissions(doesExistNotAllowed.toPath(), pfpSet);

        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExistAllowed.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        Exception actEx = null;
        File actPath = null;
        try {
            actPath = FileUtils.checkIsReadable(doesExistAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), doesExistAllowed.toPath()));

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsReadable(doesExistNotAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsReadable(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), symbolicLinkToExist.toPath()));

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsReadable(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsReadable(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsReadable(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsReadable((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkIsWritable(java.io.File)}.
     */
    @Test
    public void testCheckIsWritable() throws IOException {
        File doesExistAllowed = basePath.newFile();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("-w--w--w-");
        Files.setPosixFilePermissions(doesExistAllowed.toPath(), pfpSet);
        File doesExistNotAllowed = basePath.newFile();
        pfpSet = PosixFilePermissions.fromString("r-x------");
        Files.setPosixFilePermissions(doesExistNotAllowed.toPath(), pfpSet);

        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExistAllowed.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        Exception actEx = null;
        File actPath = null;
        try {
            actPath = FileUtils.checkIsWritable(doesExistAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), doesExistAllowed.toPath()));

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsWritable(doesExistNotAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsWritable(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), symbolicLinkToExist.toPath()));

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsWritable(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsWritable(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsWritable(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsWritable((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.FileUtils#checkIsExecutable(java.io.File)}.
     */
    @Test
    public void testCheckIsExecutable() throws IOException {
        File doesExistAllowed = basePath.newFile();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("--x--x--x");
        Files.setPosixFilePermissions(doesExistAllowed.toPath(), pfpSet);
        File doesExistNotAllowed = basePath.newFile();
        pfpSet = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(doesExistNotAllowed.toPath(), pfpSet);

        File symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp").toFile();
        File doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp").toFile();
        File tempFile = basePath.newFile().toPath().toFile();
        File symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp").toFile();
        Files.createSymbolicLink(symbolicLinkToExist.toPath(), doesExistAllowed.toPath());
        Files.createSymbolicLink(symbolicLinkToDeleted.toPath(), tempFile.toPath());
        Files.delete(tempFile.toPath());

        Exception actEx = null;
        File actPath = null;
        try {
            actPath = FileUtils.checkIsExecutable(doesExistAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), doesExistAllowed.toPath()));

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsExecutable(doesExistNotAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsExecutable(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath.toPath(), symbolicLinkToExist.toPath()));

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsExecutable(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsExecutable(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsExecutable(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = FileUtils.checkIsExecutable((File) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

}
