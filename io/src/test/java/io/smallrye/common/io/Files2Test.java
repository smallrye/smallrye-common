package io.smallrye.common.io;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static java.nio.file.attribute.PosixFilePermission.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.common.os.OS;

public class Files2Test {
    public Files2Test() {
    }

    private void makeStructure(Path rootPath) {
        FileMaker.of(rootPath, root -> {
            root.file("blah.bin", 100);
            root.file("empty.txt", 0);
            root.dir("subDir", subDir -> {
                subDir.file("subfile", 999);
                subDir.symlink("tricky", "..");
                subDir.dir("subDir2", subDir2 -> {
                    subDir2.dir("subDir3", subDir3 -> {
                        subDir3.symlink("nothing", "blah blah blah");
                        subDir3.file("innermost", 7777);
                    });
                });
            });
        });
    }

    @Test
    public void testDeleteRecursively() throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        // first we have to create a bunch of files
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        Files2.deleteRecursively(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testDeleteRecursivelyAbsolute(@TempDir Path testArea) throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        // first we have to create a bunch of files
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        Files2.deleteRecursively(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testDeleteRecursivelyEvenIfInsecure() throws IOException {
        // first we have to create a bunch of files
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        Files2.deleteRecursivelyEvenIfInsecure(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testCleanRecursively() throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        // first we have to create a bunch of files
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        Files2.cleanRecursively(testArea);
        assertTrue(Files.exists(testArea));
        assertFalse(Files.exists(testArea.resolve("blah.bin")));
        assertFalse(Files.exists(testArea.resolve("empty.txt")));
        assertTrue(Files.exists(testArea.resolve("subDir")));
        assertFalse(Files.exists(testArea.resolve("subDir").resolve("subfile")));
        Files2.deleteRecursively(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testCleanRecursivelyAbsolute(@TempDir Path testArea) throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        // first we have to create a bunch of files
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        Files2.cleanRecursively(testArea);
        assertTrue(Files.exists(testArea));
        assertFalse(Files.exists(testArea.resolve("blah.bin")));
        assertFalse(Files.exists(testArea.resolve("empty.txt")));
        assertTrue(Files.exists(testArea.resolve("subDir")));
        assertFalse(Files.exists(testArea.resolve("subDir").resolve("subfile")));
        Files2.deleteRecursively(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testCleanRecursivelyEvenIfInsecure() throws IOException {
        // first we have to create a bunch of files
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        Files2.cleanRecursivelyEvenIfInsecure(testArea);
        assertTrue(Files.exists(testArea));
        assertFalse(Files.exists(testArea.resolve("blah.bin")));
        assertFalse(Files.exists(testArea.resolve("empty.txt")));
        assertTrue(Files.exists(testArea.resolve("subDir")));
        assertFalse(Files.exists(testArea.resolve("subDir").resolve("subfile")));
        Files2.deleteRecursivelyEvenIfInsecure(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testDeleteRecursivelyQuietly() {
        assumeTrue(Files2.hasSecureDirectories());
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        // do it
        DeleteStats stats = Files2.deleteRecursivelyQuietly(testArea);
        assertFalse(Files.exists(testArea));
        assertEquals(6, stats.filesFound());
        assertEquals(6, stats.filesRemoved());
        assertEquals(4, stats.directoriesFound());
        assertEquals(4, stats.directoriesRemoved());
    }

    @Test
    public void testDeleteRecursivelyQuietlyAbsolute(@TempDir Path testArea) {
        assumeTrue(Files2.hasSecureDirectories());
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        // do it
        DeleteStats stats = Files2.deleteRecursivelyQuietly(testArea);
        assertFalse(Files.exists(testArea));
        assertEquals(6, stats.filesFound());
        assertEquals(6, stats.filesRemoved());
        assertEquals(4, stats.directoriesFound());
        assertEquals(4, stats.directoriesRemoved());
    }

    @Test
    public void testDeleteRecursivelyQuietlyEvenIfInsecure() {
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        // do it
        DeleteStats stats = Files2.deleteRecursivelyQuietlyEvenIfInsecure(testArea);
        assertFalse(Files.exists(testArea));
        assertEquals(6, stats.filesFound());
        assertEquals(6, stats.filesRemoved());
        assertEquals(4, stats.directoriesFound());
        assertEquals(4, stats.directoriesRemoved());
    }

    @Test
    public void testDeleteRecursivelyQuietlyOnError() {
        assumeTrue(Files2.hasSecureDirectories());
        Path nePath = Path.of("target/non-existent-path");
        assertFalse(Files.exists(nePath));
        DeleteStats stats = Files2.deleteRecursivelyQuietly(nePath);
        assertEquals(0, stats.filesFound());
        assertEquals(0, stats.filesRemoved());
        assertEquals(0, stats.directoriesFound());
        assertEquals(0, stats.directoriesRemoved());
    }

    @Test
    public void testDeleteRecursivelyQuietlyEvenIfInsecureOnError() {
        Path nePath = Path.of("target/non-existent-path");
        assertFalse(Files.exists(nePath));
        DeleteStats stats = Files2.deleteRecursivelyQuietlyEvenIfInsecure(nePath);
        assertEquals(0, stats.filesFound());
        assertEquals(0, stats.filesRemoved());
        assertEquals(0, stats.directoriesFound());
        assertEquals(0, stats.directoriesRemoved());
    }

    @Test
    public void testCopy() throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        Path srcFile = testArea.resolve("blah.bin");
        Path destFile = testArea.resolve("blah-copy.bin");
        assertTrue(Files.exists(srcFile));
        Files2.copy(srcFile, destFile);
        assertTrue(Files.exists(srcFile));
        assertTrue(Files.exists(destFile));
        assertEquals(Files.size(srcFile), Files.size(destFile));
        Files2.deleteRecursively(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testCopyAbsolute(@TempDir Path testArea) throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        makeStructure(testArea);
        Path srcFile = testArea.resolve("blah.bin");
        Path destFile = testArea.resolve("blah-copy.bin");
        assertTrue(Files.exists(srcFile));
        Files2.copy(srcFile, destFile);
        assertTrue(Files.exists(srcFile));
        assertTrue(Files.exists(destFile));
        assertEquals(Files.size(srcFile), Files.size(destFile));
    }

    @Test
    public void testCopyReplaceExisting(@TempDir Path testArea) throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        makeStructure(testArea);
        Path srcFile = testArea.resolve("blah.bin");
        Path destFile = testArea.resolve("empty.txt");
        long srcSize = Files.size(srcFile);
        assertTrue(Files.exists(srcFile));
        assertTrue(Files.exists(destFile));
        Files2.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(srcFile));
        assertTrue(Files.exists(destFile));
        assertEquals(srcSize, Files.size(destFile));
    }

    @Test
    public void testCopyEvenIfInsecure() throws IOException {
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        Path srcFile = testArea.resolve("blah.bin");
        Path destFile = testArea.resolve("blah-copy.bin");
        assertTrue(Files.exists(srcFile));
        Files2.copyEvenIfInsecure(srcFile, destFile);
        assertTrue(Files.exists(srcFile));
        assertTrue(Files.exists(destFile));
        assertEquals(Files.size(srcFile), Files.size(destFile));
        Files2.deleteRecursivelyEvenIfInsecure(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testCopyRecursively() throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        Path testArea = Path.of("target/test-area");
        Path destArea = Path.of("target/test-area-copy");
        assumeFalse(Files.exists(testArea));
        assumeFalse(Files.exists(destArea));
        makeStructure(testArea);
        assertTrue(Files.exists(testArea));
        Files2.copyRecursively(testArea, destArea);
        // verify source is still intact
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("blah.bin")));
        // verify dest structure
        assertTrue(Files.exists(destArea));
        assertTrue(Files.exists(destArea.resolve("blah.bin")));
        assertTrue(Files.exists(destArea.resolve("empty.txt")));
        assertTrue(Files.exists(destArea.resolve("subDir")));
        assertTrue(Files.exists(destArea.resolve("subDir").resolve("subfile")));
        assertTrue(Files.exists(destArea.resolve("subDir").resolve("subDir2").resolve("subDir3").resolve("innermost")));
        assertEquals(Files.size(testArea.resolve("blah.bin")), Files.size(destArea.resolve("blah.bin")));
        Files2.deleteRecursively(testArea);
        Files2.deleteRecursively(destArea);
        assertFalse(Files.exists(testArea));
        assertFalse(Files.exists(destArea));
    }

    @Test
    public void testCopyRecursivelyAbsolute(@TempDir Path testArea) throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        Path srcDir = testArea.resolve("src");
        Path destDir = testArea.resolve("dest");
        makeStructure(srcDir);
        assertTrue(Files.exists(srcDir));
        Files2.copyRecursively(srcDir, destDir);
        // verify source is still intact
        assertTrue(Files.exists(srcDir));
        assertTrue(Files.exists(srcDir.resolve("blah.bin")));
        // verify dest structure
        assertTrue(Files.exists(destDir));
        assertTrue(Files.exists(destDir.resolve("blah.bin")));
        assertTrue(Files.exists(destDir.resolve("empty.txt")));
        assertTrue(Files.exists(destDir.resolve("subDir")));
        assertTrue(Files.exists(destDir.resolve("subDir").resolve("subfile")));
        assertTrue(Files.exists(destDir.resolve("subDir").resolve("subDir2").resolve("subDir3").resolve("innermost")));
        assertEquals(Files.size(srcDir.resolve("blah.bin")), Files.size(destDir.resolve("blah.bin")));
    }

    @Test
    public void testCopyRecursivelyEvenIfInsecure() throws IOException {
        Path testArea = Path.of("target/test-area");
        Path destArea = Path.of("target/test-area-copy");
        assumeFalse(Files.exists(testArea));
        assumeFalse(Files.exists(destArea));
        makeStructure(testArea);
        assertTrue(Files.exists(testArea));
        Files2.copyRecursivelyEvenIfInsecure(testArea, destArea);
        // verify source is still intact
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("blah.bin")));
        // verify dest structure
        assertTrue(Files.exists(destArea));
        assertTrue(Files.exists(destArea.resolve("blah.bin")));
        assertTrue(Files.exists(destArea.resolve("empty.txt")));
        assertTrue(Files.exists(destArea.resolve("subDir")));
        assertTrue(Files.exists(destArea.resolve("subDir").resolve("subfile")));
        assertTrue(Files.exists(destArea.resolve("subDir").resolve("subDir2").resolve("subDir3").resolve("innermost")));
        assertEquals(Files.size(testArea.resolve("blah.bin")), Files.size(destArea.resolve("blah.bin")));
        Files2.deleteRecursivelyEvenIfInsecure(testArea);
        Files2.deleteRecursivelyEvenIfInsecure(destArea);
        assertFalse(Files.exists(testArea));
        assertFalse(Files.exists(destArea));
    }

    @Test
    public void testMove() throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        Path testArea = Path.of("target/test-area");
        Path destArea = Path.of("target/test-area-moved");
        assumeFalse(Files.exists(testArea));
        assumeFalse(Files.exists(destArea));
        makeStructure(testArea);
        assertTrue(Files.exists(testArea));
        Files2.move(testArea, destArea);
        // verify source is gone
        assertFalse(Files.exists(testArea));
        // verify dest structure
        assertTrue(Files.exists(destArea));
        assertTrue(Files.exists(destArea.resolve("blah.bin")));
        assertTrue(Files.exists(destArea.resolve("empty.txt")));
        assertTrue(Files.exists(destArea.resolve("subDir")));
        assertTrue(Files.exists(destArea.resolve("subDir").resolve("subfile")));
        assertTrue(Files.exists(destArea.resolve("subDir").resolve("subDir2").resolve("subDir3").resolve("innermost")));
        Files2.deleteRecursively(destArea);
        assertFalse(Files.exists(destArea));
    }

    @Test
    public void testMoveAbsolute(@TempDir Path testArea) throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        Path srcDir = testArea.resolve("src");
        Path destDir = testArea.resolve("dest");
        makeStructure(srcDir);
        assertTrue(Files.exists(srcDir));
        Files2.move(srcDir, destDir);
        // verify source is gone
        assertFalse(Files.exists(srcDir));
        // verify dest structure
        assertTrue(Files.exists(destDir));
        assertTrue(Files.exists(destDir.resolve("blah.bin")));
        assertTrue(Files.exists(destDir.resolve("empty.txt")));
        assertTrue(Files.exists(destDir.resolve("subDir")));
        assertTrue(Files.exists(destDir.resolve("subDir").resolve("subfile")));
        assertTrue(Files.exists(destDir.resolve("subDir").resolve("subDir2").resolve("subDir3").resolve("innermost")));
    }

    @Test
    public void testMoveFile() throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        Path srcFile = testArea.resolve("blah.bin");
        Path destFile = testArea.resolve("blah-moved.bin");
        long srcSize = Files.size(srcFile);
        assertTrue(Files.exists(srcFile));
        Files2.move(srcFile, destFile);
        assertFalse(Files.exists(srcFile));
        assertTrue(Files.exists(destFile));
        assertEquals(srcSize, Files.size(destFile));
        Files2.deleteRecursively(testArea);
        assertFalse(Files.exists(testArea));
    }

    @Test
    public void testMoveFileAbsolute(@TempDir Path testArea) throws IOException {
        assumeTrue(Files2.hasSecureDirectories());
        makeStructure(testArea);
        Path srcFile = testArea.resolve("blah.bin");
        Path destFile = testArea.resolve("blah-moved.bin");
        long srcSize = Files.size(srcFile);
        assertTrue(Files.exists(srcFile));
        Files2.move(srcFile, destFile);
        assertFalse(Files.exists(srcFile));
        assertTrue(Files.exists(destFile));
        assertEquals(srcSize, Files.size(destFile));
    }

    /**
     * Asserts that the given file path has private file permissions.
     */
    private void assertPrivateFilePermissions(Path path) throws IOException {
        if (OS.WINDOWS.isCurrent()) {
            AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
            assertNotNull(aclView, "ACL view should be available on Windows");
            List<AclEntry> acl = aclView.getAcl();
            assertEquals(1, acl.size(), "Private file should have exactly one ACL entry");
            AclEntry entry = acl.get(0);
            assertEquals(AclEntryType.ALLOW, entry.type());
            EnumSet<AclEntryPermission> expected = EnumSet.allOf(AclEntryPermission.class);
            expected.remove(AclEntryPermission.EXECUTE);
            assertEquals(expected, entry.permissions());
        } else {
            assertEquals(Set.of(OWNER_READ, OWNER_WRITE), Files.getPosixFilePermissions(path));
        }
    }

    /**
     * Asserts that the given directory path has private directory permissions.
     */
    private void assertPrivateDirectoryPermissions(Path path) throws IOException {
        if (OS.WINDOWS.isCurrent()) {
            AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
            assertNotNull(aclView, "ACL view should be available on Windows");
            List<AclEntry> acl = aclView.getAcl();
            assertEquals(1, acl.size(), "Private directory should have exactly one ACL entry");
            AclEntry entry = acl.get(0);
            assertEquals(AclEntryType.ALLOW, entry.type());
            assertEquals(EnumSet.allOf(AclEntryPermission.class), entry.permissions());
        } else {
            assertEquals(Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE), Files.getPosixFilePermissions(path));
        }
    }

    @Test
    public void testCurrentUserPrincipal() {
        UserPrincipal principal = Files2.currentUserPrincipal();
        assertNotNull(principal);
        String name = principal.getName();
        assertEquals(ProcessHandle.current().info().user().orElseThrow(), name);
        if (OS.WINDOWS.isCurrent()) {
            int idx = name.lastIndexOf('\\');
            if (idx != -1) {
                name = name.substring(idx + 1);
            }
        }
        assertEquals(System.getProperty("user.name"), name);
    }

    @Test
    public void testPrivateFileAttributeNotNull() {
        FileAttribute<?> attr = Files2.privateFileAttribute();
        assertNotNull(attr);
        assertNotNull(attr.value());
        if (OS.WINDOWS.isCurrent()) {
            assertEquals("acl:acl", attr.name());
        } else {
            assertEquals("posix:permissions", attr.name());
        }
    }

    @Test
    public void testPrivateDirectoryAttributeNotNull() {
        FileAttribute<?> attr = Files2.privateDirectoryAttribute();
        assertNotNull(attr);
        assertNotNull(attr.value());
        if (OS.WINDOWS.isCurrent()) {
            assertEquals("acl:acl", attr.name());
        } else {
            assertEquals("posix:permissions", attr.name());
        }
    }

    @Test
    public void testCreatePrivateFile(@TempDir Path testArea) throws IOException {
        Path file = testArea.resolve("private.txt");
        Files.createFile(file, Files2.privateFileAttribute());
        assertTrue(Files.exists(file));
        // verify read/write round-trip
        byte[] content = "hello private world".getBytes();
        Files.write(file, content);
        assertArrayEquals(content, Files.readAllBytes(file));
        // verify permissions
        assertPrivateFilePermissions(file);
        // verify deletion
        Files.delete(file);
        assertFalse(Files.exists(file));
    }

    @Test
    public void testCreatePrivateDirectory(@TempDir Path testArea) throws IOException {
        Path dir = testArea.resolve("private-dir");
        Files.createDirectory(dir, Files2.privateDirectoryAttribute());
        assertTrue(Files.exists(dir));
        assertTrue(Files.isDirectory(dir));
        // verify the directory is usable by creating a file inside it
        Path innerFile = dir.resolve("inner.txt");
        Files.createFile(innerFile);
        assertTrue(Files.exists(innerFile));
        // verify directory permissions
        assertPrivateDirectoryPermissions(dir);
        // verify deletion
        Files.delete(innerFile);
        assertFalse(Files.exists(innerFile));
        Files.delete(dir);
        assertFalse(Files.exists(dir));
    }

    @Test
    public void testCreatePrivateTempFile(@TempDir Path testArea) throws IOException {
        Path file = Files.createTempFile(testArea, "private", ".tmp", Files2.privateFileAttribute());
        assertTrue(Files.exists(file));
        assertPrivateFilePermissions(file);
        Files.delete(file);
        assertFalse(Files.exists(file));
    }

    @Test
    public void testCreatePrivateTempDirectory(@TempDir Path testArea) throws IOException {
        Path dir = Files.createTempDirectory(testArea, "private", Files2.privateDirectoryAttribute());
        assertTrue(Files.exists(dir));
        assertTrue(Files.isDirectory(dir));
        assertPrivateDirectoryPermissions(dir);
        Files.delete(dir);
        assertFalse(Files.exists(dir));
    }

    @Test
    public void testOpenBufferedCreateNewWithAttrs(@TempDir Path testArea) throws IOException {
        Path file = testArea.resolve("private-buffered.bin");
        try (BufferedFile bf = Files2.openBuffered(file,
                Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.READ),
                List.of(Files2.privateFileAttribute()))) {
            bf.writeByte(42);
        }
        assertTrue(Files.exists(file));
        assertPrivateFilePermissions(file);
        assertEquals(1, Files.size(file));
    }

    @Test
    public void testOpenBufferedCreateWithAttrsNewFile(@TempDir Path testArea) throws IOException {
        Path file = testArea.resolve("private-created.bin");
        try (BufferedFile bf = Files2.openBuffered(file,
                Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ),
                List.of(Files2.privateFileAttribute()))) {
            bf.writeByte(99);
        }
        assertTrue(Files.exists(file));
        assertPrivateFilePermissions(file);
        assertEquals(1, Files.size(file));
    }

    @Test
    public void testOpenBufferedCreateWithAttrsExistingFile(@TempDir Path testArea) throws IOException {
        Path file = testArea.resolve("existing.bin");
        byte[] content = { 1, 2, 3 };
        Files.write(file, content);
        assertTrue(Files.exists(file));
        try (BufferedFile bf = Files2.openBuffered(file,
                Set.of(StandardOpenOption.CREATE, StandardOpenOption.READ),
                List.of(Files2.privateFileAttribute()))) {
            assertEquals(1, bf.readByte());
        }
        assertEquals(3, Files.size(file));
    }

    @Test
    public void testOpenBufferedCreateNewWithAttrsFailsIfExists(@TempDir Path testArea) throws IOException {
        Path file = testArea.resolve("already-exists.bin");
        Files.createFile(file);
        assertTrue(Files.exists(file));
        try {
            Files2.openBuffered(file,
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                    List.of(Files2.privateFileAttribute())).close();
            throw new AssertionError("Expected FileAlreadyExistsException");
        } catch (FileAlreadyExistsException expected) {
        }
    }

    @Test
    public void testOpenBufferedCreateNewWithoutAttrs(@TempDir Path testArea) throws IOException {
        Path file = testArea.resolve("no-attrs.bin");
        try (BufferedFile bf = Files2.openBuffered(file,
                Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.READ))) {
            bf.writeByte(7);
        }
        assertTrue(Files.exists(file));
        assertEquals(1, Files.size(file));
    }

    @Test
    public void testMoveEvenIfInsecure() throws IOException {
        Path testArea = Path.of("target/test-area");
        Path destArea = Path.of("target/test-area-moved");
        assumeFalse(Files.exists(testArea));
        assumeFalse(Files.exists(destArea));
        makeStructure(testArea);
        assertTrue(Files.exists(testArea));
        Files2.moveEvenIfInsecure(testArea, destArea);
        // verify source is gone
        assertFalse(Files.exists(testArea));
        // verify dest structure
        assertTrue(Files.exists(destArea));
        assertTrue(Files.exists(destArea.resolve("blah.bin")));
        assertTrue(Files.exists(destArea.resolve("empty.txt")));
        assertTrue(Files.exists(destArea.resolve("subDir")));
        assertTrue(Files.exists(destArea.resolve("subDir").resolve("subfile")));
        assertTrue(Files.exists(destArea.resolve("subDir").resolve("subDir2").resolve("subDir3").resolve("innermost")));
        Files2.deleteRecursivelyEvenIfInsecure(destArea);
        assertFalse(Files.exists(destArea));
    }
}
