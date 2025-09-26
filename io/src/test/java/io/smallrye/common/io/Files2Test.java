package io.smallrye.common.io;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public void testDeleteRecursivelyQuiet() {
        assumeTrue(Files2.hasSecureDirectories());
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        // do it
        DeleteStats stats = Files2.deleteRecursivelyQuiet(testArea);
        assertFalse(Files.exists(testArea));
        assertEquals(6, stats.filesFound());
        assertEquals(6, stats.filesRemoved());
        assertEquals(4, stats.directoriesFound());
        assertEquals(4, stats.directoriesRemoved());
    }

    @Test
    public void testDeleteRecursivelyQuietAbsolute(@TempDir Path testArea) {
        assumeTrue(Files2.hasSecureDirectories());
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        // do it
        DeleteStats stats = Files2.deleteRecursivelyQuiet(testArea);
        assertFalse(Files.exists(testArea));
        assertEquals(6, stats.filesFound());
        assertEquals(6, stats.filesRemoved());
        assertEquals(4, stats.directoriesFound());
        assertEquals(4, stats.directoriesRemoved());
    }

    @Test
    public void testDeleteRecursivelyQuietEvenIfInsecure() {
        Path testArea = Path.of("target/test-area");
        assumeFalse(Files.exists(testArea));
        makeStructure(testArea);
        // rough check to make sure things got created
        assertTrue(Files.exists(testArea));
        assertTrue(Files.exists(testArea.resolve("subDir").resolve("subDir2")));
        // do it
        DeleteStats stats = Files2.deleteRecursivelyQuietEvenIfInsecure(testArea);
        assertFalse(Files.exists(testArea));
        assertEquals(6, stats.filesFound());
        assertEquals(6, stats.filesRemoved());
        assertEquals(4, stats.directoriesFound());
        assertEquals(4, stats.directoriesRemoved());
    }

    @Test
    public void testDeleteRecursivelyQuietOnError() {
        assumeTrue(Files2.hasSecureDirectories());
        Path nePath = Path.of("target/non-existent-path");
        assertFalse(Files.exists(nePath));
        DeleteStats stats = Files2.deleteRecursivelyQuiet(nePath);
        assertEquals(0, stats.filesFound());
        assertEquals(0, stats.filesRemoved());
        assertEquals(0, stats.directoriesFound());
        assertEquals(0, stats.directoriesRemoved());
    }

    @Test
    public void testDeleteRecursivelyQuietEvenIfInsecureOnError() {
        Path nePath = Path.of("target/non-existent-path");
        assertFalse(Files.exists(nePath));
        DeleteStats stats = Files2.deleteRecursivelyQuietEvenIfInsecure(nePath);
        assertEquals(0, stats.filesFound());
        assertEquals(0, stats.filesRemoved());
        assertEquals(0, stats.directoriesFound());
        assertEquals(0, stats.directoriesRemoved());
    }
}
