package io.smallrye.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A resource representing an entry in a JAR file.
 */
public final class JarFileResource extends Resource {

    private final URL base;
    private final JarFile jarFile;
    private final JarEntry jarEntry;
    private URL url;

    JarFileResource(final URL base, final JarFile jarFile, final JarEntry jarEntry) {
        super(jarEntry.getName());
        this.base = base;
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
    }

    public URL url() {
        URL url = this.url;
        if (url == null) {
            try {
                // todo: Java 20+: URL.of(new URI("jar", null, base.toURI().toASCIIString() + "!/" + pathName()),
                //                        new ResourceURLStreamHandler(this));
                url = this.url = new URL(null,
                        new URI("jar", base.toURI().toASCIIString() + "!/" + pathName(), null).toASCIIString(),
                        new ResourceURLStreamHandler(this));
            } catch (MalformedURLException | URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return url;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation does not recognize directories that do not have an explicit entry.
     *           This restriction may be lifted in future versions.
     */
    public boolean isDirectory() {
        return jarEntry.isDirectory();
    }

    public DirectoryStream<Resource> openDirectoryStream() throws IOException {
        if (!isDirectory()) {
            return super.openDirectoryStream();
        }
        return new DirectoryStream<Resource>() {
            Enumeration<JarEntry> entries;

            public Iterator<Resource> iterator() {
                if (entries == null) {
                    entries = jarFile.entries();
                    return new Iterator<Resource>() {
                        Resource next;

                        public boolean hasNext() {
                            String ourName = jarEntry.getName();
                            while (next == null) {
                                if (!entries.hasMoreElements()) {
                                    return false;
                                }
                                JarEntry e = entries.nextElement();
                                String name = e.getName();
                                int ourLen = ourName.length();
                                if (name.startsWith(ourName) && !name.equals(ourName)) {
                                    int idx = name.indexOf('/', ourLen);
                                    if (idx == -1 || name.length() == idx + 1) {
                                        next = new JarFileResource(base, jarFile, e);
                                    }
                                    break;
                                }
                            }
                            return true;
                        }

                        public Resource next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            Resource next = this.next;
                            this.next = null;
                            return next;
                        }
                    };
                }
                throw new IllegalStateException();
            }

            public void close() {
                entries = Collections.emptyEnumeration();
            }
        };
    }

    public Instant modifiedTime() {
        FileTime fileTime = jarEntry.getLastModifiedTime();
        return fileTime == null ? null : fileTime.toInstant();
    }

    public InputStream openStream() throws IOException {
        return jarFile.getInputStream(jarEntry);
    }

    public long size() {
        return jarEntry.getSize();
    }
}
