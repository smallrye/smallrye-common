package io.smallrye.common.archive.tests;

import static io.smallrye.common.archive.tests.Util.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.smallrye.common.io.archive.Archive;

public final class BasicTests {
    @Test
    public void testOpenSimple() throws IOException {
        byte[] bytes = makeJar(
                store("stored", "Good morning!"),
                deflate("hello", "Hello world!"));
        Archive archive = Archive.open(bytes);
        assertTrue(archive.entryNameEquals(0, "hello"));
        assertFalse(archive.entryNameEquals(0, "goodbye"));
        assertEquals("hello", archive.entryName(0));
        assertTrue(archive.entryNameEquals(1, "stored"));
        assertFalse(archive.entryNameEquals(1, "hello"));
        assertEquals("stored", archive.entryName(1));
        try (InputStream is = archive.openEntry(1)) {
            try (InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                try (BufferedReader br = new BufferedReader(r)) {
                    assertEquals("Good morning!", br.readLine());
                }
            }
        }
        try (InputStream is = archive.openEntry(0)) {
            try (InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                try (BufferedReader br = new BufferedReader(r)) {
                    assertEquals("Hello world!", br.readLine());
                }
            }
        }
    }

    @Test
    public void testDirectorySorting() throws IOException {
        String[] names = Stream.generate(BasicTests::randomName).limit(1000).toArray(String[]::new);
        // create a JAR with the unsorted names
        byte[] bytes = makeJar(Stream.of(names).map(n -> store(n, n)).toArray(Entry[]::new));
        // sort the names
        Arrays.sort(names);
        Archive archive = Archive.open(bytes);
        // make sure they all made it into the archive
        assertEquals(names.length, archive.entryCount());
        // make sure the archive entries are sorted
        for (int i = 0; i < names.length; i++) {
            assertTrue(archive.entryNameEquals(i, names[i]));
            assertEquals(names[i], archive.entryName(i));
        }
    }

    private static String randomName() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        char[] name = new char[tlr.nextInt(10, 40)];
        for (int i = 0; i < name.length; i++) {
            int v = tlr.nextInt(62);
            if (v < 26) {
                name[i] = (char) ('A' + v);
            } else if (v < 52) {
                name[i] = (char) ('a' + v - 26);
            } else {
                name[i] = (char) ('0' + v - 52);
            }
        }
        return new String(name);
    }
}
