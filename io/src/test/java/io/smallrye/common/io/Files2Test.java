package io.smallrye.common.io;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
