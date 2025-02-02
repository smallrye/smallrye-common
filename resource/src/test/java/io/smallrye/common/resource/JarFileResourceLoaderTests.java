package io.smallrye.common.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

public class JarFileResourceLoaderTests {

    public static final String FILE_TXT = "This is a plain text file\nIf you can read this, it's working!\n";

    @Test
    public void testOpenJar() throws IOException {
        try (JarFileResourceLoader rl = makeJar(
                // keep these two first
                dir("META-INF"),
                deflate("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n"),
                // test entries
                dir("dir1"),
                store("dir1/file-stored.txt", FILE_TXT),
                deflate("dir1/file-deflated.txt", FILE_TXT),
                // hidden directory
                store("dir2/hidden-file.txt", FILE_TXT),
                dir("dir2/dir3"),
                dir("dir2/dir3/dir4"),
                store("dir2/dir3/blah.txt", FILE_TXT),
                // keep this as last entry
                dir("end"))) {
            assertNull(rl.findResource("missing"));
            Resource dir1_file_stored_txt = rl.findResource("dir1/file-stored.txt");
            assertNotNull(dir1_file_stored_txt);
            assertEquals("jar:memory:test.jar!/dir1/file-stored.txt", dir1_file_stored_txt.url().toString());
            assertEquals(FILE_TXT, dir1_file_stored_txt.asString(StandardCharsets.UTF_8));
            Resource dir1_file_deflated_txt = rl.findResource("dir1/file-deflated.txt");
            assertNotNull(dir1_file_deflated_txt);
            assertEquals(FILE_TXT, dir1_file_deflated_txt.asString(StandardCharsets.UTF_8));
            Resource dir1 = rl.findResource("dir1");
            assertNotNull(dir1);
            assertEquals(0, dir1.size());
            assertEquals("jar:memory:test.jar!/dir1", dir1.url().toString());
            try (DirectoryStream<Resource> ds = dir1.openDirectoryStream()) {
                Iterator<Resource> iterator = ds.iterator();
                assertTrue(iterator.hasNext());
                assertEquals("dir1/file-stored.txt", iterator.next().pathName());
                assertTrue(iterator.hasNext());
                assertEquals("dir1/file-deflated.txt", iterator.next().pathName());
                assertFalse(iterator.hasNext());
            }
            Resource dir2_hidden_file_txt = rl.findResource("dir2/hidden-file.txt");
            assertNotNull(dir2_hidden_file_txt);
            assertEquals(FILE_TXT, dir2_hidden_file_txt.asString(StandardCharsets.UTF_8));
            Resource dir2 = rl.findResource("dir2");
            assertNotNull(dir2);
            assertEquals(0, dir2.size());
            assertEquals("jar:memory:test.jar!/dir2", dir2.url().toString());
            try (DirectoryStream<Resource> ds = dir2.openDirectoryStream()) {
                Iterator<Resource> iterator = ds.iterator();
                assertTrue(iterator.hasNext());
                assertEquals("dir2/hidden-file.txt", iterator.next().pathName());
                assertTrue(iterator.hasNext());
                Resource dir3 = iterator.next();
                assertEquals("dir2/dir3", dir3.pathName());
                assertFalse(iterator.hasNext());
                try (DirectoryStream<Resource> ds3 = dir3.openDirectoryStream()) {
                    Iterator<Resource> iter3 = ds3.iterator();
                    assertTrue(iter3.hasNext());
                    assertEquals("dir2/dir3/dir4", iter3.next().pathName());
                    assertTrue(iter3.hasNext());
                    assertEquals("dir2/dir3/blah.txt", iter3.next().pathName());
                    assertFalse(iter3.hasNext());
                }
            }
            Resource rootDir = rl.findResource("/");
            assertNotNull(rootDir);
            assertEquals(0, rootDir.size());
            assertEquals("jar:memory:test.jar!/", rootDir.url().toString());
            try (DirectoryStream<Resource> ds = rootDir.openDirectoryStream()) {
                Iterator<Resource> iterator = ds.iterator();
                assertTrue(iterator.hasNext());
                assertEquals("META-INF", iterator.next().pathName());
                assertTrue(iterator.hasNext());
                assertEquals("dir1", iterator.next().pathName());
                assertEquals("end", iterator.next().pathName());
                assertFalse(iterator.hasNext());
            }
        }
    }

    private static JarFileResourceLoader makeJar(Entry... entries) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            for (Entry entry : entries)
                try {
                    entry.writeTo(zos);
                } catch (Throwable t) {
                    t.setStackTrace(entry.stack);
                    throw t;
                }
            zos.finish();
        }
        // create a temp file, force it to be deleted on exit
        return new JarFileResourceLoader(new MemoryResource("test.jar", os.toByteArray()));
    }

    private static Entry dir(String name) {
        return new DirEntry(name);
    }

    private static Entry deflate(String name, String content) {
        return new DeflateEntry(name, content);
    }

    private static Entry store(String name, String content) {
        return new StoredEntry(name, content);
    }

    private static abstract class Entry {
        final String name;
        final StackTraceElement[] stack = new Throwable().getStackTrace();

        private Entry(final String name) {
            this.name = name;
        }

        abstract void writeTo(ZipOutputStream os) throws IOException;
    }

    private static final class DeflateEntry extends Entry {
        private final String content;

        private DeflateEntry(final String name, final String content) {
            super(name);
            this.content = content;
        }

        void writeTo(final ZipOutputStream os) throws IOException {
            ZipEntry e = new ZipEntry(name);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            e.setMethod(ZipEntry.DEFLATED);
            e.setSize(data.length);
            os.putNextEntry(e);
            os.write(data);
            os.closeEntry();
        }
    }

    private static final class StoredEntry extends Entry {
        private final String content;

        private StoredEntry(final String name, final String content) {
            super(name);
            this.content = content;
        }

        void writeTo(final ZipOutputStream os) throws IOException {
            ZipEntry e = new ZipEntry(name);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            e.setMethod(ZipEntry.STORED);
            e.setSize(data.length);
            e.setCompressedSize(data.length);
            CRC32 crc32 = new CRC32();
            crc32.update(data);
            e.setCrc(crc32.getValue());
            os.putNextEntry(e);
            os.write(data);
            os.closeEntry();
        }
    }

    private static final class DirEntry extends Entry {

        private DirEntry(final String name) {
            super(name.endsWith("/") ? name : name + "/");
        }

        void writeTo(final ZipOutputStream os) throws IOException {
            ZipEntry e = new ZipEntry(name);
            e.setMethod(ZipEntry.STORED);
            e.setSize(0);
            e.setCompressedSize(0);
            e.setCrc(0);
            os.putNextEntry(e);
            os.closeEntry();
        }
    }
}
