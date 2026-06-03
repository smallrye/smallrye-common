package io.smallrye.common.archive.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Util {
    public static byte[] makeJar(Entry... entries) throws IOException {
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
        return os.toByteArray();
    }

    public static Entry dir(String name) {
        return new DirEntry(name);
    }

    public static Entry deflate(String name, String content) {
        return new DeflateEntry(name, content);
    }

    public static Entry store(String name, String content) {
        return new StoredEntry(name, content);
    }

    public static abstract class Entry {
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
