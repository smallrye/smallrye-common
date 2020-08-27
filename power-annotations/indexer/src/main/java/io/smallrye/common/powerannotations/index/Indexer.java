package io.smallrye.common.powerannotations.index;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;

class Indexer {
    private final org.jboss.jandex.Indexer indexer = new org.jboss.jandex.Indexer();
    private final IndexerConfig config = new IndexerConfig();
    private int archivesIndexed;
    private int classesIndexed;

    IndexView scanClassPath() {
        long t0 = System.currentTimeMillis();
        urls()
                .distinct()
                .filter(this::include)
                .forEach(this::index);
        Index index = indexer.complete();
        LOG.info("scanned " + archivesIndexed + " archives with " + classesIndexed + " classes " +
                "in " + (System.currentTimeMillis() - t0) + "ms");
        return index;
    }

    private Stream<URL> urls() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        if (classLoader instanceof URLClassLoader) {
            return Stream.of(((URLClassLoader) classLoader).getURLs());
        } else {
            return classPath().map(Indexer::toUrl);
        }
    }

    private Stream<String> classPath() {
        return Stream.of(System.getProperty("java.class.path").split(System.getProperty("path.separator")));
    }

    private static URL toUrl(String url) {
        try {
            return Paths.get(url).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("invalid classpath url " + url, e);
        }
    }

    private boolean include(URL url) {
        String urlString = url.toString();
        return config.excludes().noneMatch(urlString::matches);
    }

    private void index(URL url) {
        try {
            long t0 = System.currentTimeMillis();
            int classesIndexedBefore = classesIndexed;
            if (url.toString().endsWith(".jar") || url.toString().endsWith(".war"))
                indexArchive(url.openStream());
            else
                indexFolder(url);
            LOG.info("indexed " + (classesIndexed - classesIndexedBefore) + " classes in " + url
                    + " in " + (System.currentTimeMillis() - t0) + " ms");
        } catch (IOException e) {
            throw new RuntimeException("can't index " + url, e);
        }
    }

    private void indexArchive(InputStream inputStream) throws IOException {
        archivesIndexed++;
        ZipInputStream zipInputStream = new ZipInputStream(inputStream, UTF_8);
        while (true) {
            ZipEntry entry = zipInputStream.getNextEntry();
            if (entry == null)
                break;
            String entryName = entry.getName();
            indexFile(entryName, zipInputStream);
        }
    }

    private void indexFile(String fileName, InputStream inputStream) throws IOException {
        if (fileName.endsWith(".class")) {
            classesIndexed++;
            indexer.index(inputStream);
        } else if (fileName.endsWith(".war")) {
            // necessary because of the Thorntail arquillian adapter
            indexArchive(inputStream);
        }
    }

    private void indexFolder(URL url) throws IOException {
        try {
            Path folderPath = Paths.get(url.toURI());
            if (Files.isDirectory(folderPath)) {
                try (Stream<Path> walk = Files.walk(folderPath)) {
                    walk.filter(Files::isRegularFile)
                            .forEach(this::indexFile);
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("invalid folder url " + url, e);
        }
    }

    private void indexFile(Path path) {
        try {
            String entryName = path.getFileName().toString();
            indexFile(entryName, Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException("can't index path " + path, e);
        }
    }

    private static final Logger LOG = Logger.getLogger(Indexer.class.getName());
}
