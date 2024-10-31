package io.smallrye.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.FileTime;
import java.security.CodeSigner;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
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

    public boolean isDirectory() {
        return jarEntry.isDirectory();
    }

    public DirectoryStream<Resource> openDirectoryStream() throws IOException {
        if (!isDirectory()) {
            return super.openDirectoryStream();
        }
        return new DirectoryStream<Resource>() {
            public Iterator<Resource> iterator() {
                String ourName = pathName();
                return jarFile.versionedStream()
                        .filter(e -> {
                            String name = ResourceUtils.canonicalizeRelativePath(e.getName());
                            if (ourName.isEmpty()) {
                                // find root entries
                                return name.indexOf('/') == -1;
                            } else {
                                // find subdirectory entries
                                int si;
                                return name.startsWith(ourName)
                                        && name.length() > ourName.length()
                                        && name.charAt(ourName.length()) == '/'
                                        && ((si = name.indexOf('/', ourName.length() + 1)) == -1 || si == name.length() - 1);
                            }
                        })
                        .map(e -> new JarFileResource(base, jarFile, e))
                        // appease the generics demons
                        .map(r -> (Resource) r)
                        .iterator();
            }

            public void close() {
            }
        };
    }

    public Instant modifiedTime() {
        FileTime fileTime = jarEntry.getLastModifiedTime();
        return fileTime == null ? null : fileTime.toInstant();
    }

    public List<CodeSigner> codeSigners() {
        CodeSigner[] array = jarEntry.getCodeSigners();
        return array == null ? List.of() : List.of(array);
    }

    public InputStream openStream() throws IOException {
        return isDirectory() ? InputStream.nullInputStream() : jarFile.getInputStream(jarEntry);
    }

    public long size() {
        return jarEntry.getSize();
    }
}
