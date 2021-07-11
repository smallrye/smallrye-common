package io.smallrye.common.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
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

/**
 * @author Boris Unckel
 *
 */
public class PathUtilsTest {

    private TemporaryFolder basePath = null;

    @TempDir
    public Path tempPath;

    /**
     * TemporaryFolder replaces JUnit 4 {@code org.junit.io.TemporaryFolder}
     *
     */
    static class TemporaryFolder {

        private Path underlying;

        public TemporaryFolder(final Path underlying) {
            this.underlying = Objects.requireNonNull(underlying);
        }

        public File getRoot() {
            return underlying.toFile();
        }

        public File newFile() throws IOException {
            return Files.createTempFile(underlying, "testonly", "tmp").toFile();
        }

        public File newFolder() throws IOException {
            return Files.createTempDirectory(underlying, "testfolderonly").toFile();
        }

        public File newFolder(final String folderName) throws IOException {
            Path aPath = Paths.get(underlying.toString(), Objects.requireNonNull(folderName));
            return Files.createDirectory(aPath).toFile();
        }

    }

    @BeforeEach
    void init() {
        Objects.requireNonNull(tempPath);
        assertTrue(Files.exists(tempPath, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        this.basePath = new TemporaryFolder(tempPath);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#get(java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testGetStringStringArray() throws IOException {
        // valid
        Path actPath = null;
        actPath = PathUtils.get(basePath.getRoot().toString());
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, basePath.getRoot().toPath()));

        actPath = null;
        actPath = PathUtils.get(basePath.getRoot().toString(), "second");
        assertNotNull(actPath);

        // invalid
        Exception actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.get("notA \0 path");
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof java.nio.file.InvalidPathException);
        assertNull(actPath);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.get((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
        assertNull(actPath);
    }

    /**
     * Test method for {@link org.wildfly.common.io.PathUtils#get(java.net.URI)}.
     */
    @Test
    public void testGetURI() throws IOException {
        // valid
        Path actPath = null;
        actPath = PathUtils.get(basePath.getRoot().toURI());
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, basePath.getRoot().toPath()));

        // invalid
        Exception actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.get(new URI("http://notAPath.com"));
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof java.nio.file.FileSystemNotFoundException);
        assertNull(actPath);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.get((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
        assertNull(actPath);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.nio.file.Path)}.
     */
    @Test
    public void testNewDirectoryStreamPath() throws IOException {
        basePath.newFile();
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toPath());
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(2, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.lang.String)}.
     */
    @Test
    public void testNewDirectoryStreamString() throws IOException {
        basePath.newFile();
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toString());
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(2, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.net.URI)}.
     */
    @Test
    public void testNewDirectoryStreamURI() throws IOException {
        basePath.newFile();
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toURI());
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(2, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.nio.file.Path, java.lang.String)}.
     */
    @Test
    public void testNewDirectoryStreamPathString() throws IOException {
        File fileA = basePath.newFile();
        String filter = fileA.getName();
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toPath(), filter);
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(1, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((Path) null, filter);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.newDirectoryStream(basePath.getRoot().toPath(), (String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testNewDirectoryStreamStringString() throws IOException {
        File fileA = basePath.newFile();
        String filter = fileA.getName();
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toString(), filter);
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(1, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((String) null, filter);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.newDirectoryStream(basePath.getRoot().toString(), (String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.net.URI, java.lang.String)}.
     */
    @Test
    public void testNewDirectoryStreamURIString() throws IOException {
        File fileA = basePath.newFile();
        String filter = fileA.getName();
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toURI(), filter);
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(1, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((URI) null, filter);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.newDirectoryStream(basePath.getRoot().toURI(), (String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream.Filter)}.
     */
    @Test
    public void testNewDirectoryStreamPathFilterOfQsuperPath() throws IOException {
        File fileA = basePath.newFile();
        TestFilter filter = new TestFilter(fileA.getName());
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toPath(), filter);
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(1, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((Path) null, filter);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.newDirectoryStream(basePath.getRoot().toPath(),
                    (java.nio.file.DirectoryStream.Filter<Path>) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    private static class TestFilter implements DirectoryStream.Filter<Path> {

        final String filename;

        public TestFilter(final String filename) {
            this.filename = filename;
        }

        @Override
        public boolean accept(Path entry) throws IOException {
            String name = entry.getFileName().toString();
            return name.equals(filename);
        }

    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.lang.String, java.nio.file.DirectoryStream.Filter)}.
     */
    @Test
    public void testNewDirectoryStreamStringFilterOfQsuperPath() throws IOException {
        File fileA = basePath.newFile();
        TestFilter filter = new TestFilter(fileA.getName());
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toString(), filter);
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(1, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((String) null, filter);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.newDirectoryStream(basePath.getRoot().toString(),
                    (java.nio.file.DirectoryStream.Filter<Path>) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#newDirectoryStream(java.net.URI, java.nio.file.DirectoryStream.Filter)}.
     */
    @Test
    public void testNewDirectoryStreamURIFilterOfQsuperPath() throws IOException {
        File fileA = basePath.newFile();
        TestFilter filter = new TestFilter(fileA.getName());
        basePath.newFile();
        DirectoryStream<Path> ds = PathUtils.newDirectoryStream(basePath.getRoot().toURI(), filter);
        int i = 0;
        for (Path p : ds) {
            i++;
            Files.delete(p);
        }
        ds.close();
        assertEquals(1, i);

        Exception actEx = null;
        try {
            PathUtils.newDirectoryStream((URI) null, filter);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.newDirectoryStream(basePath.getRoot().toURI(), (java.nio.file.DirectoryStream.Filter<Path>) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createFile(java.nio.file.Path,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateFilePathFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testFile.tmp");
        Path actual = PathUtils.createFile(toCreate);
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createFile((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createFile(java.lang.String,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateFileStringFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testFile.tmp");
        Path actual = PathUtils.createFile(toCreate.toString());
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createFile((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createFile(java.net.URI,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateFileURIFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testFile.tmp");
        Path actual = PathUtils.createFile(toCreate.toUri());
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createFile((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createDirectory(java.nio.file.Path,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateDirectoryPathFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testSubFolder");
        Path actual = PathUtils.createDirectory(toCreate);
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createDirectory((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createDirectory(java.lang.String,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateDirectoryStringFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testSubFolder");
        Path actual = PathUtils.createDirectory(toCreate.toString());
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createDirectory((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createDirectory(java.net.URI,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateDirectoryURIFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testSubFolder");
        Path actual = PathUtils.createDirectory(toCreate.toUri());
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createDirectory((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createDirectories(java.nio.file.Path,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateDirectoriesPathFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testSubFolder1", "testSubFolder2");
        Path actual = PathUtils.createDirectories(toCreate);
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createDirectories((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createDirectories(java.lang.String,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateDirectoriesStringFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testSubFolder1", "testSubFolder2");
        Path actual = PathUtils.createDirectories(toCreate.toString());
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createDirectories((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createDirectories(java.net.URI,
     * java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateDirectoriesURIFileAttributeOfQArray() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testSubFolder1", "testSubFolder2");
        Path actual = PathUtils.createDirectories(toCreate.toUri());
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.createDirectories((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#touch(java.nio.file.Path)}.
     */
    @Test
    public void testTouchPath() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testFile.tmp");
        Path actual = PathUtils.touch(toCreate);
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.touch((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#touch(java.lang.String)}.
     */
    @Test
    public void testTouchString() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testFile.tmp");
        Path actual = PathUtils.touch(toCreate.toString());
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.touch((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for {@link org.wildfly.common.io.PathUtils#touch(java.net.URI)}.
     */
    @Test
    public void testTouchURI() throws IOException {
        Path toCreate = Paths.get(basePath.getRoot().toString(), "testFile.tmp");
        Path actual = PathUtils.touch(toCreate.toUri());
        assertTrue(Files.exists(toCreate, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(toCreate, actual));

        Exception actEx = null;
        try {
            PathUtils.touch((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createSymbolicLink(java.nio.file.Path,
     * java.nio.file.Path, java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateSymbolicLinkPathPathFileAttributeOfQArray() throws IOException {
        File target = basePath.newFile();
        Path link = Paths.get(basePath.getRoot().toString(), "linkToTarget");
        Path actual = PathUtils.createSymbolicLink(link, target.toPath());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(actual));
        assertTrue(Files.isSameFile(link, actual));

        Exception actEx = null;
        try {
            PathUtils.createSymbolicLink((Path) null, target.toPath());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.createSymbolicLink(link, (Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createSymbolicLink(java.lang.String,
     * java.lang.String, java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateSymbolicLinkStringStringFileAttributeOfQArray() throws IOException {
        File target = basePath.newFile();
        Path link = Paths.get(basePath.getRoot().toString(), "linkToTarget");
        Path actual = PathUtils.createSymbolicLink(link.toString(), target.toString());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(actual));
        assertTrue(Files.isSameFile(link, actual));

        Exception actEx = null;
        try {
            PathUtils.createSymbolicLink((String) null, target.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.createSymbolicLink(link.toString(), (String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createSymbolicLink(java.net.URI,
     * java.net.URI, java.nio.file.attribute.FileAttribute<?>[])}.
     */
    @Test
    public void testCreateSymbolicLinkURIURIFileAttributeOfQArray() throws IOException {
        File target = basePath.newFile();
        Path link = Paths.get(basePath.getRoot().toString(), "linkToTarget");
        Path actual = PathUtils.createSymbolicLink(link.toUri(), target.toURI());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(actual));
        assertTrue(Files.isSameFile(link, actual));

        Exception actEx = null;
        try {
            PathUtils.createSymbolicLink((URI) null, target.toURI());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.createSymbolicLink(link.toUri(), (URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createLink(java.nio.file.Path, java.nio.file.Path)}.
     */
    @Test
    public void testCreateLinkPathPath() throws IOException {
        File target = basePath.newFile();
        Path link = Paths.get(basePath.getRoot().toString(), "linkToTarget");
        Path actual = PathUtils.createLink(link, target.toPath());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(actual));
        assertTrue(Files.isSameFile(link, actual));

        Exception actEx = null;
        try {
            PathUtils.createLink((Path) null, target.toPath());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.createLink(link, (Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createLink(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testCreateLinkStringString() throws IOException {
        File target = basePath.newFile();
        Path link = Paths.get(basePath.getRoot().toString(), "linkToTarget");
        Path actual = PathUtils.createLink(link.toString(), target.toString());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(actual));
        assertTrue(Files.isSameFile(link, actual));

        Exception actEx = null;
        try {
            PathUtils.createLink((String) null, target.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.createLink(link.toString(), (String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#createLink(java.net.URI, java.net.URI)}.
     */
    @Test
    public void testCreateLinkURIURI() throws IOException {
        File target = basePath.newFile();
        Path link = Paths.get(basePath.getRoot().toString(), "linkToTarget");
        Path actual = PathUtils.createLink(link.toUri(), target.toURI());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(actual));
        assertTrue(Files.isSameFile(link, actual));

        Exception actEx = null;
        try {
            PathUtils.createLink((URI) null, target.toURI());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.createLink(link.toUri(), (URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#delete(java.nio.file.Path)}.
     */
    @Test
    public void testDeletePath() throws IOException {
        Path toDeleteFolder = basePath.newFolder("subFolder").toPath();
        Path toDeleteFile = basePath.newFile().toPath();
        Path notExists = Paths.get(basePath.toString(), "notExist");

        Path actualPath = PathUtils.delete(toDeleteFolder);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFolder, actualPath));
        assertTrue(Files.notExists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        actualPath = null;
        actualPath = PathUtils.delete(toDeleteFile);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFile, actualPath));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        Exception actual = null;
        actualPath = null;
        try {
            actualPath = PathUtils.delete(notExists);
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.delete((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#delete(java.lang.String)}.
     */
    @Test
    public void testDeleteString() throws IOException {
        Path toDeleteFolder = basePath.newFolder("subFolder").toPath();
        Path toDeleteFile = basePath.newFile().toPath();
        Path notExists = Paths.get(basePath.toString(), "notExist");
        PathUtils.delete(toDeleteFolder.toString());
        PathUtils.delete(toDeleteFile.toString());
        Exception actual = null;
        try {
            PathUtils.delete(notExists.toString());
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NoSuchFileException);
        assertTrue(Files.notExists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        Exception actEx = null;
        try {
            PathUtils.delete((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for {@link org.wildfly.common.io.PathUtils#delete(java.net.URI)}.
     */
    @Test
    public void testDeleteURI() throws IOException {
        Path toDeleteFolder = basePath.newFolder("subFolder").toPath();
        Path toDeleteFile = basePath.newFile().toPath();
        Path notExists = Paths.get(basePath.toString(), "notExist");
        PathUtils.delete(toDeleteFolder.toUri());
        PathUtils.delete(toDeleteFile.toUri());
        Exception actual = null;
        try {
            PathUtils.delete(notExists.toUri());
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NoSuchFileException);
        assertTrue(Files.notExists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        Exception actEx = null;
        try {
            PathUtils.delete((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#deleteIfExists(java.nio.file.Path)}.
     */
    @Test
    public void testDeleteIfExistsPath() throws IOException {
        Path toDeleteFolder = basePath.newFolder("subFolder").toPath();
        Path toDeleteFile = basePath.newFile().toPath();
        Path notExists = Paths.get(basePath.toString(), "notExist");
        PathUtils.deleteIfExists(toDeleteFolder);
        PathUtils.deleteIfExists(toDeleteFile);
        Exception actual = null;
        try {
            PathUtils.deleteIfExists(notExists);
        } catch (Exception e) {
            actual = e;
        }
        assertNull(actual);
        assertTrue(Files.notExists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        Exception actEx = null;
        try {
            PathUtils.deleteIfExists((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#deleteIfExists(java.lang.String)}.
     */
    @Test
    public void testDeleteIfExistsString() throws IOException {
        Path toDeleteFolder = basePath.newFolder("subFolder").toPath();
        Path toDeleteFile = basePath.newFile().toPath();
        Path notExists = Paths.get(basePath.toString(), "notExist");
        PathUtils.deleteIfExists(toDeleteFolder.toString());
        PathUtils.deleteIfExists(toDeleteFile.toString());
        Exception actual = null;
        try {
            PathUtils.deleteIfExists(notExists.toString());
        } catch (Exception e) {
            actual = e;
        }
        assertNull(actual);
        assertTrue(Files.notExists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        Exception actEx = null;
        try {
            PathUtils.deleteIfExists((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#deleteIfExists(java.net.URI)}.
     */
    @Test
    public void testDeleteIfExistsURI() throws IOException {
        Path toDeleteFolder = basePath.newFolder("subFolder").toPath();
        Path toDeleteFile = basePath.newFile().toPath();
        Path notExists = Paths.get(basePath.toString(), "notExist");
        PathUtils.deleteIfExists(toDeleteFolder.toUri());
        PathUtils.deleteIfExists(toDeleteFile.toUri());
        Exception actual = null;
        try {
            PathUtils.deleteIfExists(notExists.toUri());
        } catch (Exception e) {
            actual = e;
        }
        assertNull(actual);
        assertTrue(Files.notExists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        Exception actEx = null;
        try {
            PathUtils.deleteIfExists((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#deleteDirectoryContents(Path path)}.
     */
    @Test
    public void testdeleteDirectoryContentsPath() throws IOException {
        Path toDeleteFolder = basePath.newFolder("subFolder").toPath();
        Path toDeleteFile = null;
        Path notExists = Paths.get(basePath.toString(), "notExist");

        toDeleteFile = Files.createTempFile(toDeleteFolder, "test", "tmp");
        Path actualPath = PathUtils.deleteDirectoryContents(toDeleteFolder);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFolder, actualPath));
        assertTrue(Files.exists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        toDeleteFile = Files.createTempFile(toDeleteFolder, "test", "tmp");
        Exception actual = null;
        actualPath = null;
        try {
            actualPath = PathUtils.deleteDirectoryContents(toDeleteFile);
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NotDirectoryException);
        assertNull(actualPath);

        toDeleteFile = Files.createTempFile(toDeleteFolder, "test", "tmp");
        actual = null;
        actualPath = null;
        try {
            actualPath = PathUtils.deleteDirectoryContents(notExists);
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.deleteDirectoryContents((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#deleteRecursively(Path path)}.
     */
    @Test
    public void testdeleteRecursivelyPath() throws IOException {
        Path toDeleteFolder = null;
        Path toDeleteFile = null;
        Path notExists = Paths.get(basePath.toString(), "notExist");

        toDeleteFolder = basePath.newFolder("subFolder").toPath();
        toDeleteFile = Files.createTempFile(toDeleteFolder, "test", "tmp");
        Path actualPath = PathUtils.deleteRecursively(toDeleteFolder);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFolder, actualPath));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        toDeleteFolder = basePath.newFolder("subFolder").toPath();
        toDeleteFile = Files.createTempFile(toDeleteFolder, "test", "tmp");
        Exception actual = null;
        actualPath = null;
        try {
            actualPath = PathUtils.deleteRecursively(toDeleteFile);
        } catch (Exception e) {
            actual = e;
        }
        assertNull(actual);
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(toDeleteFile, actualPath));
        assertTrue(Files.notExists(toDeleteFile, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.exists(toDeleteFolder, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));

        actual = null;
        actualPath = null;
        try {
            actualPath = PathUtils.deleteRecursively(notExists);
        } catch (Exception e) {
            actual = e;
        }
        assertNotNull(actual);
        assertTrue(actual.getCause() instanceof java.nio.file.NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.deleteRecursively((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])}.
     */
    @Test
    public void testCopyPathPathCopyOptionArray() throws IOException {
        Path source = basePath.newFile().toPath();
        Path target = Paths.get(basePath.getRoot().toString(), "testTarget.tmp");
        Path actual = PathUtils.copy(source, target);
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(target, actual));

        Exception actEx = null;
        try {
            PathUtils.copy((Path) null, target);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.copy(source, (Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#copy(java.lang.String, java.lang.String, java.nio.file.CopyOption[])}.
     */
    @Test
    public void testCopyStringStringCopyOptionArray() throws IOException {
        Path source = basePath.newFile().toPath();
        Path target = Paths.get(basePath.getRoot().toString(), "testTarget.tmp");
        Path actual = PathUtils.copy(source.toString(), target.toString());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(target, actual));

        Exception actEx = null;
        try {
            PathUtils.copy((String) null, target.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.copy(source.toString(), (String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#copy(java.net.URI, java.net.URI, java.nio.file.CopyOption[])}.
     */
    @Test
    public void testCopyURIURICopyOptionArray() throws IOException {
        Path source = basePath.newFile().toPath();
        Path target = Paths.get(basePath.getRoot().toString(), "testTarget.tmp");
        Path actual = PathUtils.copy(source.toUri(), target.toUri());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(target, actual));

        Exception actEx = null;
        try {
            PathUtils.copy((URI) null, target.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.copy(source.toUri(), (URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#move(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])}.
     */
    @Test
    public void testMovePathPathCopyOptionArray() throws IOException {
        Path source = basePath.newFile().toPath();
        Path target = Paths.get(basePath.getRoot().toString(), "testTarget.tmp");
        Path actual = PathUtils.move(source, target);
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(source, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(target, actual));

        Exception actEx = null;
        try {
            PathUtils.move((Path) null, target);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.move(source, (Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#move(java.lang.String, java.lang.String, java.nio.file.CopyOption[])}.
     */
    @Test
    public void testMoveStringStringCopyOptionArray() throws IOException {
        Path source = basePath.newFile().toPath();
        Path target = Paths.get(basePath.getRoot().toString(), "testTarget.tmp");
        Path actual = PathUtils.move(source.toString(), target.toString());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(source, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(target, actual));

        Exception actEx = null;
        try {
            PathUtils.move((String) null, target.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.move(source.toString(), (String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#move(java.net.URI, java.net.URI, java.nio.file.CopyOption[])}.
     */
    @Test
    public void testMoveURIURICopyOptionArray() throws IOException {
        Path source = basePath.newFile().toPath();
        Path target = Paths.get(basePath.getRoot().toString(), "testTarget.tmp");
        Path actual = PathUtils.move(source.toUri(), target.toUri());
        assertTrue(Files.exists(actual, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.notExists(source, PathUtils.NO_FOLLOW_LINK_OPTION_ARRAY));
        assertTrue(Files.isSameFile(target, actual));

        Exception actEx = null;
        try {
            PathUtils.move((URI) null, target.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.move(source.toUri(), (URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#readSymbolicLink(java.nio.file.Path)}.
     */
    @Test
    public void testReadSymbolicLinkPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(doesExist);
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotLinkException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(symbolicLinkToExist);
        } catch (final Exception e) {
            actEx = e;
        }
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(actualPath, doesExist));
        assertNull(actEx);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(doesNotExist);
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(tempFile);
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(symbolicLinkToDeleted);
        } catch (final Exception e) {
            actEx = e;
        }
        // This is right - readSymbolicLink does not check target existence.
        assertNotNull(actualPath);
        assertNull(actEx);

        actEx = null;
        try {
            PathUtils.readSymbolicLink((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#readSymbolicLink(java.lang.String)}.
     */
    @Test
    public void testReadSymbolicLinkString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(doesExist.toString());
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotLinkException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(symbolicLinkToExist.toString());
        } catch (final Exception e) {
            actEx = e;
        }
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(actualPath, doesExist));
        assertNull(actEx);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(doesNotExist.toString());
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(tempFile.toString());
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(symbolicLinkToDeleted.toString());
        } catch (final Exception e) {
            actEx = e;
        }
        // This is right - readSymbolicLink does not check target existence.
        assertNotNull(actualPath);
        assertNull(actEx);

        actEx = null;
        try {
            PathUtils.readSymbolicLink((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#readSymbolicLink(java.net.URI)}.
     */
    @Test
    public void testReadSymbolicLinkURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(doesExist.toUri());
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotLinkException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(symbolicLinkToExist.toUri());
        } catch (final Exception e) {
            actEx = e;
        }
        assertNotNull(actualPath);
        assertTrue(Files.isSameFile(actualPath, doesExist));
        assertNull(actEx);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(doesNotExist.toUri());
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(tempFile.toUri());
        } catch (final Exception e) {
            actEx = e;
        }
        assertNull(actualPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.readSymbolicLink(symbolicLinkToDeleted.toUri());
        } catch (final Exception e) {
            actEx = e;
        }
        // This is right - readSymbolicLink does not check target existence.
        assertNotNull(actualPath);
        assertNull(actEx);

        actEx = null;
        try {
            PathUtils.readSymbolicLink((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#isSameFile(java.nio.file.Path, java.nio.file.Path)}.
     */
    @Test
    public void testIsSameFilePathPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path doesExistSecondRef = Paths.get(doesExist.toString());
        assertTrue(PathUtils.isSameFile(doesExist, doesExist));
        assertTrue(PathUtils.isSameFile(doesExist, doesExistSecondRef));
        assertTrue(PathUtils.isSameFile(doesExist, symbolicLinkToExist));

        Path doesNotExistSecondRef = Paths.get(doesNotExist.toString());
        assertTrue(PathUtils.isSameFile(doesNotExist, doesNotExist));
        assertTrue(PathUtils.isSameFile(doesNotExist, doesNotExistSecondRef));

        Path tempFileSecondRef = Paths.get(tempFile.toString());
        assertTrue(PathUtils.isSameFile(tempFile, tempFile));
        assertTrue(PathUtils.isSameFile(tempFile, tempFileSecondRef));

        Exception actEx = null;
        try {
            PathUtils.isSameFile(tempFile, symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        try {
            PathUtils.isSameFile(tempFile, (Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.isSameFile((Path) null, tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#isSameFile(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIsSameFileStringString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path doesExistSecondRef = Paths.get(doesExist.toString());
        assertTrue(PathUtils.isSameFile(doesExist.toString(), doesExist.toString()));
        assertTrue(PathUtils.isSameFile(doesExist.toString(), doesExistSecondRef.toString()));
        assertTrue(PathUtils.isSameFile(doesExist.toString(), symbolicLinkToExist.toString()));

        Path doesNotExistSecondRef = Paths.get(doesNotExist.toString());
        assertTrue(PathUtils.isSameFile(doesNotExist.toString(), doesNotExist.toString()));
        assertTrue(PathUtils.isSameFile(doesNotExist.toString(), doesNotExistSecondRef.toString()));

        Path tempFileSecondRef = Paths.get(tempFile.toString());
        assertTrue(PathUtils.isSameFile(tempFile.toString(), tempFile.toString()));
        assertTrue(PathUtils.isSameFile(tempFile.toString(), tempFileSecondRef.toString()));

        Exception actEx = null;
        try {
            PathUtils.isSameFile(tempFile, symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        try {
            PathUtils.isSameFile(tempFile.toString(), (String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.isSameFile((String) null, tempFile.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#isSameFile(java.net.URI, java.net.URI)}.
     */
    @Test
    public void testIsSameFileURIURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path doesExistSecondRef = Paths.get(doesExist.toString());
        assertTrue(PathUtils.isSameFile(doesExist.toUri(), doesExist.toUri()));
        assertTrue(PathUtils.isSameFile(doesExist.toUri(), doesExistSecondRef.toUri()));
        assertTrue(PathUtils.isSameFile(doesExist.toUri(), symbolicLinkToExist.toUri()));

        Path doesNotExistSecondRef = Paths.get(doesNotExist.toString());
        assertTrue(PathUtils.isSameFile(doesNotExist.toUri(), doesNotExist.toUri()));
        assertTrue(PathUtils.isSameFile(doesNotExist.toUri(), doesNotExistSecondRef.toUri()));

        Path tempFileSecondRef = Paths.get(tempFile.toString());
        assertTrue(PathUtils.isSameFile(tempFile.toUri(), tempFile.toUri()));
        assertTrue(PathUtils.isSameFile(tempFile.toUri(), tempFileSecondRef.toUri()));

        Exception actEx = null;
        try {
            PathUtils.isSameFile(tempFile.toUri(), symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        try {
            PathUtils.isSameFile(tempFile.toUri(), (URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

        actEx = null;
        try {
            PathUtils.isSameFile((URI) null, tempFile.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsSymbolicLink(java.nio.file.Path)}.
     */
    @Test
    public void testCheckIsSymbolicLinkPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotLinkException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToDeleted));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsSymbolicLink(java.lang.String)}.
     */
    @Test
    public void testCheckIsSymbolicLinkString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(doesExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotLinkException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(doesNotExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToDeleted));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsSymbolicLink(java.net.URI)}.
     */
    @Test
    public void testCheckIsSymbolicLinkURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(doesExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotLinkException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(doesNotExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToDeleted));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsSymbolicLink((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsDirectoryFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testCheckIsDirectoryFollowLinksPath() throws IOException {
        Path doesExist = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExistsFolder");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFolder().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDeleteFolder");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsDirectoryFollowLinks(java.lang.String)}.
     */
    @Test
    public void testCheckIsDirectoryFollowLinksString() throws IOException {
        Path doesExist = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExistsFolder");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFolder().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDeleteFolder");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(doesExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(doesNotExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(tempFile.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsDirectoryFollowLinks(java.net.URI)}.
     */
    @Test
    public void testCheckIsDirectoryFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExistsFolder");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFolder().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDeleteFolder");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(doesExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(doesNotExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(tempFile.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsDirectoryNoFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testCheckIsDirectoryNoFollowLinksPath() throws IOException {
        Path doesExist = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExistsFolder");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFolder().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDeleteFolder");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotDirectoryException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotDirectoryException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsDirectoryNoFollowLinks(java.lang.String)}.
     */
    @Test
    public void testCheckIsDirectoryNoFollowLinksString() throws IOException {
        Path doesExist = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExistsFolder");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFolder().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDeleteFolder");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(doesExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotDirectoryException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(doesNotExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(tempFile.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotDirectoryException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsDirectoryNoFollowLinks(java.net.URI)}.
     */
    @Test
    public void testCheckIsDirectoryNoFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExistsFolder");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFolder().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDeleteFolder");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(doesExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotDirectoryException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(doesNotExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(tempFile.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NotDirectoryException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsDirectoryNoFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsRegularFileFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testCheckIsRegularFileFollowLinksPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path doesExistDirectory = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesExistDirectory);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsRegularFileFollowLinks(java.lang.String)}.
     */
    @Test
    public void testCheckIsRegularFileFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path doesExistDirectory = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesExistDirectory.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesNotExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsRegularFileFollowLinks(java.net.URI)}.
     */
    @Test
    public void testCheckIsRegularFileFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path doesExistDirectory = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesExistDirectory.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(doesNotExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsRegularFileNoFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testCheckIsRegularFileNoFollowLinksPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path doesExistDirectory = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesExistDirectory);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsRegularFileNoFollowLinks(java.lang.String)}.
     */
    @Test
    public void testCheckIsRegularFileNoFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path doesExistDirectory = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesExistDirectory.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesNotExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsRegularFileNoFollowLinks(java.net.URI)}.
     */
    @Test
    public void testCheckIsRegularFileNoFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path doesExistDirectory = basePath.newFolder().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Path actPath = null;
        Exception actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actPath);
        assertNull(actEx);
        assertTrue(Files.isSameFile(actPath, doesExist));

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesExistDirectory.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(doesNotExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.FileSystemException);

        actPath = null;
        actEx = null;
        try {
            actPath = PathUtils.checkIsRegularFileNoFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#existsFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testExistsFollowLinks() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertTrue(PathUtils.existsFollowLinks(doesExist));
        assertTrue(PathUtils.existsFollowLinks(symbolicLinkToExist));
        assertFalse(PathUtils.existsFollowLinks(doesNotExist));
        assertFalse(PathUtils.existsFollowLinks(tempFile));
        assertFalse(PathUtils.existsFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.existsFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#existsFollowLinks(java.lang.String)}.
     */
    @Test
    public void testExistsFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertTrue(PathUtils.existsFollowLinks(doesExist.toString()));
        assertTrue(PathUtils.existsFollowLinks(symbolicLinkToExist.toString()));
        assertFalse(PathUtils.existsFollowLinks(doesNotExist.toString()));
        assertFalse(PathUtils.existsFollowLinks(tempFile.toString()));
        assertFalse(PathUtils.existsFollowLinks(symbolicLinkToDeleted.toString()));

        Exception actEx = null;
        try {
            PathUtils.existsFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#existsFollowLinks(java.net.URI)}.
     */
    @Test
    public void testExistsFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertTrue(PathUtils.existsFollowLinks(doesExist.toUri()));
        assertTrue(PathUtils.existsFollowLinks(symbolicLinkToExist.toUri()));
        assertFalse(PathUtils.existsFollowLinks(doesNotExist.toUri()));
        assertFalse(PathUtils.existsFollowLinks(tempFile.toUri()));
        assertFalse(PathUtils.existsFollowLinks(symbolicLinkToDeleted.toUri()));

        Exception actEx = null;
        try {
            PathUtils.existsFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#existsNoFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testExistsNoFollowLinks() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertTrue(PathUtils.existsNoFollowLinks(doesExist));
        assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToExist));
        assertFalse(PathUtils.existsNoFollowLinks(doesNotExist));
        assertFalse(PathUtils.existsNoFollowLinks(tempFile));
        assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.existsNoFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#existsNoFollowLinks(java.lang.String)}.
     */
    @Test
    public void testExistsNoFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertTrue(PathUtils.existsNoFollowLinks(doesExist));
        assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToExist));
        assertFalse(PathUtils.existsNoFollowLinks(doesNotExist));
        assertFalse(PathUtils.existsNoFollowLinks(tempFile));
        assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.existsNoFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);

    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#existsNoFollowLinks(java.net.URI)}.
     */
    @Test
    public void testExistsNoFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertTrue(PathUtils.existsNoFollowLinks(doesExist.toUri()));
        assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToExist.toUri()));
        assertFalse(PathUtils.existsNoFollowLinks(doesNotExist.toUri()));
        assertFalse(PathUtils.existsNoFollowLinks(tempFile.toUri()));
        assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToDeleted.toUri()));

        Exception actEx = null;
        try {
            PathUtils.existsNoFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#notExistsFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testNotExistsFollowLinks() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertFalse(PathUtils.notExistsFollowLinks(doesExist));
        assertFalse(PathUtils.notExistsFollowLinks(symbolicLinkToExist));
        assertTrue(PathUtils.notExistsFollowLinks(doesNotExist));
        assertTrue(PathUtils.notExistsFollowLinks(tempFile));
        assertTrue(PathUtils.notExistsFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.notExistsFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#notExistsFollowLinks(java.lang.String)}.
     */
    @Test
    public void testNotExistsFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertFalse(PathUtils.notExistsFollowLinks(doesExist.toString()));
        assertFalse(PathUtils.notExistsFollowLinks(symbolicLinkToExist.toString()));
        assertTrue(PathUtils.notExistsFollowLinks(doesNotExist.toString()));
        assertTrue(PathUtils.notExistsFollowLinks(tempFile.toString()));
        assertTrue(PathUtils.notExistsFollowLinks(symbolicLinkToDeleted.toString()));

        Exception actEx = null;
        try {
            PathUtils.notExistsFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#notExistsFollowLinks(java.net.URI)}.
     */
    @Test
    public void testNotExistsFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertFalse(PathUtils.notExistsFollowLinks(doesExist.toUri()));
        assertFalse(PathUtils.notExistsFollowLinks(symbolicLinkToExist.toUri()));
        assertTrue(PathUtils.notExistsFollowLinks(doesNotExist.toUri()));
        assertTrue(PathUtils.notExistsFollowLinks(tempFile.toUri()));
        assertTrue(PathUtils.notExistsFollowLinks(symbolicLinkToDeleted.toUri()));

        Exception actEx = null;
        try {
            PathUtils.notExistsFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#notExistsNoFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testNotExistsNoFollowLinks() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertFalse(PathUtils.notExistsNoFollowLinks(doesExist));
        assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToExist));
        assertTrue(PathUtils.notExistsNoFollowLinks(doesNotExist));
        assertTrue(PathUtils.notExistsNoFollowLinks(tempFile));
        assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.notExistsNoFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#notExistsNoFollowLinks(java.lang.String)}.
     */
    @Test
    public void testNotExistsNoFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertFalse(PathUtils.notExistsNoFollowLinks(doesExist.toString()));
        assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToExist.toString()));
        assertTrue(PathUtils.notExistsNoFollowLinks(doesNotExist.toString()));
        assertTrue(PathUtils.notExistsNoFollowLinks(tempFile.toString()));
        assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToDeleted.toString()));

        Exception actEx = null;
        try {
            PathUtils.notExistsNoFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#notExistsNoFollowLinks(java.net.URI)}.
     */
    @Test
    public void testNotExistsNoFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        assertFalse(PathUtils.notExistsNoFollowLinks(doesExist.toUri()));
        assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToExist.toUri()));
        assertTrue(PathUtils.notExistsNoFollowLinks(doesNotExist.toUri()));
        assertTrue(PathUtils.notExistsNoFollowLinks(tempFile.toUri()));
        assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToDeleted.toUri()));

        Exception actEx = null;
        try {
            PathUtils.notExistsNoFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkExistsFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testCheckExistsFollowLinksPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // same as: assertTrue(PathUtils.existsFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesExist));

        // same as: assertTrue(PathUtils.existsFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToExist));

        // same as: assertFalse(PathUtils.existsFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesNotExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // same as: assertFalse(PathUtils.existsFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(tempFile);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // same as: assertFalse(PathUtils.existsFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.checkExistsFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkExistsFollowLinks(java.lang.String)}.
     */
    @Test
    public void testCheckExistsFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // same as: assertTrue(PathUtils.existsFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesExist));

        // same as: assertTrue(PathUtils.existsFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToExist));

        // same as: assertFalse(PathUtils.existsFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesNotExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // same as: assertFalse(PathUtils.existsFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(tempFile.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // same as: assertFalse(PathUtils.existsFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.checkExistsFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkExistsFollowLinks(java.net.URI)}.
     */
    @Test
    public void testCheckExistsFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // same as: assertTrue(PathUtils.existsFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesExist));

        // same as: assertTrue(PathUtils.existsFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToExist));

        // same as: assertFalse(PathUtils.existsFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesNotExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // same as: assertFalse(PathUtils.existsFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(tempFile.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // same as: assertFalse(PathUtils.existsFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.checkExistsFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkExistsNoFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testCheckExistsNoFollowLinksPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(doesExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesExist));

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToExist));

        // Same as: assertFalse(PathUtils.existsNoFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesNotExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.existsNoFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(tempFile);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.checkExistsNoFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkExistsNoFollowLinks(java.lang.String)}.
     */
    @Test
    public void testCheckExistsNoFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(doesExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesExist));

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToExist));

        // Same as: assertFalse(PathUtils.existsNoFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesNotExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.existsNoFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(tempFile);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.checkExistsNoFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkExistsNoFollowLinks(java.net.URI)}.
     */
    @Test
    public void testCheckExistsNoFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(doesExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesExist));

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToExist));

        // Same as: assertFalse(PathUtils.existsNoFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(doesNotExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.existsNoFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsFollowLinks(tempFile);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof NoSuchFileException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.existsNoFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkExistsNoFollowLinks(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.checkExistsNoFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkNotExistsFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testCheckNotExistsFollowLinksPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertFalse(PathUtils.notExistsFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(doesExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.notExistsFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(doesNotExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesNotExist));

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(tempFile);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, tempFile));

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.checkNotExistsFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkNotExistsFollowLinks(java.lang.String)}.
     */
    @Test
    public void testCheckNotExistsFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertFalse(PathUtils.notExistsFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(doesExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.notExistsFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(doesNotExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesNotExist));

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(tempFile.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, tempFile));

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.checkNotExistsFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkNotExistsFollowLinks(java.net.URI)}.
     */
    @Test
    public void testCheckNotExistsFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertFalse(PathUtils.notExistsFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(doesExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.notExistsFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(doesNotExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesNotExist));

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(tempFile.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, tempFile));

        // Same as: assertTrue(PathUtils.notExistsFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsFollowLinks(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, symbolicLinkToDeleted));

        Exception actEx = null;
        try {
            PathUtils.checkNotExistsFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkNotExistsNoFollowLinks(java.nio.file.Path)}.
     */
    @Test
    public void testCheckNotExistsNoFollowLinksPath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertFalse(PathUtils.notExistsNoFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(doesExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(symbolicLinkToExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.notExistsNoFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(doesNotExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesNotExist));

        // Same as: assertTrue(PathUtils.notExistsNoFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(tempFile);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, tempFile));

        // Same as:
        // assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(symbolicLinkToDeleted);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.checkNotExistsNoFollowLinks((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkNotExistsNoFollowLinks(java.lang.String)}.
     */
    @Test
    public void testCheckNotExistsNoFollowLinksString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertFalse(PathUtils.notExistsNoFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(doesExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.notExistsNoFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(doesNotExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesNotExist));

        // Same as: assertTrue(PathUtils.notExistsNoFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(tempFile.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, tempFile));

        // Same as:
        // assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.checkNotExistsNoFollowLinks((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkNotExistsNoFollowLinks(java.net.URI)}.
     */
    @Test
    public void testCheckNotExistsNoFollowLinksURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        // Same as: assertFalse(PathUtils.notExistsNoFollowLinks(doesExist));
        Exception actualEx = null;
        Path actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(doesExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        // Same as: assertTrue(PathUtils.notExistsNoFollowLinks(doesNotExist));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(doesNotExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, doesNotExist));

        // Same as: assertTrue(PathUtils.notExistsNoFollowLinks(tempFile));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(tempFile.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertTrue(Files.isSameFile(actualPath, tempFile));

        // Same as:
        // assertFalse(PathUtils.notExistsNoFollowLinks(symbolicLinkToDeleted));
        actualEx = null;
        actualPath = null;
        try {
            actualPath = PathUtils.checkNotExistsNoFollowLinks(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof FileAlreadyExistsException);
        assertNull(actualPath);

        Exception actEx = null;
        try {
            PathUtils.checkNotExistsNoFollowLinks((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#size(java.nio.file.Path)}.
     */
    @Test
    public void testSizePath() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        final byte[] data = "Hello World".getBytes();
        Files.write(doesExist, data);
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actualEx = null;
        long actualSize = -1L;
        try {
            actualSize = PathUtils.size(doesExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertEquals((long) data.length, actualSize);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(symbolicLinkToExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertEquals((long) data.length, actualSize);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(doesNotExist);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(tempFile);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(symbolicLinkToDeleted);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size((Path) null);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#size(java.lang.String)}.
     */
    @Test
    public void testSizeString() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        final byte[] data = "Hello World".getBytes();
        Files.write(doesExist, data);
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actualEx = null;
        long actualSize = -1L;
        try {
            actualSize = PathUtils.size(doesExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertEquals((long) data.length, actualSize);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertEquals((long) data.length, actualSize);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(doesNotExist.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(tempFile.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size((String) null);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for {@link org.wildfly.common.io.PathUtils#size(java.net.URI)}.
     */
    @Test
    public void testSizeURI() throws IOException {
        Path doesExist = basePath.newFile().toPath();
        final byte[] data = "Hello World".getBytes();
        Files.write(doesExist, data);
        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExist);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actualEx = null;
        long actualSize = -1L;
        try {
            actualSize = PathUtils.size(doesExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertEquals((long) data.length, actualSize);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNull(actualEx);
        assertEquals((long) data.length, actualSize);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(doesNotExist.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(tempFile.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actualEx = null;
        actualSize = -1L;
        try {
            actualSize = PathUtils.size((URI) null);
        } catch (Exception e) {
            actualEx = e;
        }
        assertNotNull(actualEx);
        assertTrue(actualEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsReadable(java.nio.file.Path)}.
     */
    @Test
    public void testCheckIsReadablePath() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("r--r--r--");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("-wx------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesExistAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesExistNotAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsReadable(java.lang.String)}.
     */
    @Test
    public void testCheckIsReadableString() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("r--r--r--");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("-wx------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesExistAllowed.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesExistNotAllowed.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesNotExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(tempFile.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsReadable(java.net.URI)}.
     */
    @Test
    public void testCheckIsReadableURI() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("r--r--r--");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("-wx------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesExistAllowed.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesExistNotAllowed.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(doesNotExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(tempFile.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsReadable((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsWritable(java.nio.file.Path)}.
     */
    @Test
    public void testCheckIsWritablePath() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("-w--w--w-");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("r-x------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesExistAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesExistNotAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsWritable(java.lang.String)}.
     */
    @Test
    public void testCheckIsWritableString() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("-w--w--w-");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("r-x------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesExistAllowed.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesExistNotAllowed.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesNotExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(tempFile.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsWritable(java.net.URI)}.
     */
    @Test
    public void testCheckIsWritableURI() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("-w--w--w-");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("r-x------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesExistAllowed.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesExistNotAllowed.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(doesNotExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(tempFile.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsWritable((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsExecutable(java.nio.file.Path)}.
     */
    @Test
    public void testCheckIsExecutablePath() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("--x--x--x");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesExistAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesExistNotAllowed);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(symbolicLinkToExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesNotExist);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(tempFile);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(symbolicLinkToDeleted);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable((Path) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsExecutable(java.lang.String)}.
     */
    @Test
    public void testCheckIsExecutableString() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("--x--x--x");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesExistAllowed.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesExistNotAllowed.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(symbolicLinkToExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesNotExist.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(tempFile.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(symbolicLinkToDeleted.toString());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable((String) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#checkIsExecutable(java.net.URI)}.
     */
    @Test
    public void testCheckIsExecutableURI() throws IOException {
        Path doesExistAllowed = basePath.newFile().toPath();
        Set<PosixFilePermission> pfpSet = PosixFilePermissions.fromString("--x--x--x");
        Files.setPosixFilePermissions(doesExistAllowed, pfpSet);
        Path doesExistNotAllowed = basePath.newFile().toPath();
        pfpSet = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(doesExistNotAllowed, pfpSet);

        Path symbolicLinkToExist = Paths.get(basePath.getRoot().toString(), "symLinkExists.tmp");
        Path doesNotExist = Paths.get(basePath.getRoot().toString(), "notExist.tmp");
        Path tempFile = basePath.newFile().toPath();
        Path symbolicLinkToDeleted = Paths.get(basePath.getRoot().toString(), "symLinkToDelete.tmp");
        Files.createSymbolicLink(symbolicLinkToExist, doesExistAllowed);
        Files.createSymbolicLink(symbolicLinkToDeleted, tempFile);
        Files.delete(tempFile);

        Exception actEx = null;
        Path actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesExistAllowed.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, doesExistAllowed));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesExistNotAllowed.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.AccessDeniedException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(symbolicLinkToExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actEx);
        assertNotNull(actPath);
        assertTrue(Files.isSameFile(actPath, symbolicLinkToExist));

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(doesNotExist.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(tempFile.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable(symbolicLinkToDeleted.toUri());
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx.getCause() instanceof java.nio.file.NoSuchFileException);

        actEx = null;
        actPath = null;
        try {
            actPath = PathUtils.checkIsExecutable((URI) null);
        } catch (Exception e) {
            actEx = e;
        }
        assertNull(actPath);
        assertNotNull(actEx);
        assertTrue(actEx instanceof IllegalArgumentException);
    }

    /**
     * Test method for
     * {@link org.wildfly.common.io.PathUtils#getPresentWorkingDirectory()}.
     */
    @Test
    public void testGetPresentWorkingDirectory() throws IOException {
        Path act = PathUtils.getPresentWorkingDirectory();
        assertNotNull(act);
        assertTrue(Files.exists(act));
    }
}
