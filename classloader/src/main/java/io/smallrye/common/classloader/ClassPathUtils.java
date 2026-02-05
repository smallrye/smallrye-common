package io.smallrye.common.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Miscellaneous class path related utilities.
 */
public class ClassPathUtils {
    private static final String FILE = "file";
    private static final String JAR = "jar";

    private ClassPathUtils() {
    }

    /**
     * Invokes {@link #consumeAsStreams(ClassLoader, String, Consumer)} passing in
     * an instance of the current thread's context classloader as the classloader
     * from which to load the resources.
     *
     * @param resource resource path
     * @param consumer resource input stream consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeAsStreams(String resource, Consumer<InputStream> consumer) throws IOException {
        consumeAsStreams(Thread.currentThread().getContextClassLoader(), resource, consumer);
    }

    /**
     * Locates all the occurrences of a resource on the classpath of the provided classloader
     * and invokes the consumer providing the input streams for each located resource.
     * The consumer does not have to close the provided input stream.
     * This method was introduced to avoid calling {@link java.net.URL#openStream()} which
     * in case the resource is found in an archive (such as JAR) locks the containing archive
     * even if the caller properly closes the stream.
     *
     * @param cl classloader to load the resources from
     * @param resource resource path
     * @param consumer resource input stream consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeAsStreams(ClassLoader cl, String resource, Consumer<InputStream> consumer) throws IOException {
        final Enumeration<URL> resources = cl == null ? ClassLoader.getSystemResources(resource) : cl.getResources(resource);
        while (resources.hasMoreElements()) {
            consumeStream(resources.nextElement(), consumer);
        }
    }

    /**
     * Invokes {@link #consumeAsPaths(ClassLoader, String, Consumer)} passing in
     * an instance of the current thread's context classloader as the classloader
     * from which to load the resources.
     *
     * @param resource resource path
     * @param consumer resource path consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeAsPaths(String resource, Consumer<Path> consumer) throws IOException {
        consumeAsPaths(Thread.currentThread().getContextClassLoader(), resource, consumer);
    }

    /**
     * Locates specified resources on the classpath and attempts to represent them as local file system paths
     * to be processed by a consumer. If a resource appears to be an actual file or a directory, it is simply
     * passed to the consumer as-is. If a resource is an entry in a JAR, the entry will be resolved as an instance
     * of {@link java.nio.file.Path} in a {@link java.nio.file.FileSystem} representing the JAR.
     * If the protocol of the URL representing the resource is neither 'file' nor 'jar', the method will fail
     * with an exception.
     *
     * @param cl classloader to load the resources from
     * @param resource resource path
     * @param consumer resource path consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeAsPaths(ClassLoader cl, String resource, Consumer<Path> consumer) throws IOException {
        final Enumeration<URL> resources = cl == null ? ClassLoader.getSystemResources(resource) : cl.getResources(resource);
        while (resources.hasMoreElements()) {
            consumeAsPath(resources.nextElement(), consumer);
        }
    }

    /**
     * Attempts to represent a resource as a local file system path to be processed by a consumer.
     * If a resource appears to be an actual file or a directory, it is simply passed to the consumer as-is.
     * If a resource is an entry in a JAR, the entry will be resolved as an instance
     * of {@link java.nio.file.Path} in a {@link java.nio.file.FileSystem} representing the JAR.
     * If the protocol of the URL representing the resource is neither 'file' nor 'jar', the method will fail
     * with an exception.
     *
     * @param url resource url
     * @param consumer resource path consumer
     */
    public static void consumeAsPath(URL url, Consumer<Path> consumer) {
        processAsPath(url, p -> {
            consumer.accept(p);
            return null;
        });
    }

    /**
     * Attempts to represent a resource as a local file system path to be processed by a function.
     * If a resource appears to be an actual file or a directory, it is simply passed to the function as-is.
     * If a resource is an entry in a JAR, the entry will be resolved as an instance
     * of {@link java.nio.file.Path} in a {@link java.nio.file.FileSystem} representing the JAR.
     * If the protocol of the URL representing the resource is neither 'file' nor 'jar', the method will fail
     * with an exception.
     *
     * @param url resource url
     * @param function resource path function
     * @param <R> the result type
     * @return the result of the function
     */
    public static <R> R processAsPath(URL url, Function<Path, R> function) {
        if (JAR.equals(url.getProtocol())) {
            final String file = url.getFile();
            int exclam = file.indexOf("!/");
            for (;;) {
                try {
                    URL fileUrl;
                    String subPath;
                    if (exclam == -1) {
                        // assume the first element is a JAR file, not a plain file, since it was a `jar:` URL
                        fileUrl = new URL(file);
                        subPath = "/";
                    } else {
                        fileUrl = new URL(file.substring(0, exclam));
                        subPath = file.substring(exclam + 1);
                    }
                    if (!fileUrl.getProtocol().equals("file")) {
                        throw new IllegalArgumentException("Sub-URL of JAR URL is expected to have a scheme of `file`");
                    }

                    Path proposedPath = toLocalPath(fileUrl);
                    if (Files.isRegularFile(proposedPath)) {
                        return processAsJarPath(proposedPath, subPath, function);
                    }
                    if (exclam == -1) {
                        throw new IllegalArgumentException("File not found: " + fileUrl);
                    }
                    exclam = file.indexOf("!/", exclam + 1);
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed to create a URL for '" + file.substring(0, exclam) + "'", e);
                }
            }
        }

        if (FILE.equals(url.getProtocol())) {
            return function.apply(toLocalPath(url));
        }

        throw new IllegalArgumentException("Unexpected protocol " + url.getProtocol() + " for URL " + url);
    }

    private static <R> R processAsJarPath(Path jarPath, String path, Function<Path, R> function) {
        try (FileSystem jarFs = JarProviderHolder.JAR_PROVIDER.newFileSystem(jarPath, Map.of())) {
            Path localPath = jarFs.getPath("/");
            int start = 0;
            for (;;) {
                int idx = path.indexOf("!/", start);
                if (idx == -1) {
                    return function.apply(localPath.resolve(path));
                } else {
                    // could be nested JAR?
                    Path subPath = localPath.resolve(path.substring(0, idx));
                    if (Files.isDirectory(subPath)) {
                        // no, it's a plain directory and the `!/` is superfluous
                        localPath = subPath;
                        start = idx + 2;
                    } else {
                        // yes, it's a nested JAR file
                        return processAsJarPath(subPath, path.substring(idx + 1), function);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + jarPath, e);
        }
    }

    /**
     * Invokes a consumer providing the input streams to read the content of the URL.
     * The consumer does not have to close the provided input stream.
     * This method was introduced to avoid calling {@link java.net.URL#openStream()} which
     * in case the resource is found in an archive (such as JAR) locks the containing archive
     * even if the caller properly closes the stream.
     *
     * @param url URL
     * @param consumer input stream consumer
     * @throws IOException in case of an IO failure
     */
    public static void consumeStream(URL url, Consumer<InputStream> consumer) throws IOException {
        readStream(url, is -> {
            consumer.accept(is);
            return null;
        });
    }

    /**
     * Invokes a function providing the input streams to read the content of the URL.
     * The function does not have to close the provided input stream.
     * This method was introduced to avoid calling {@link java.net.URL#openStream()} directly,
     * which in case the resource is found in an archive (such as JAR) locks the containing archive
     * even if the caller properly closes the stream.
     *
     * @param url URL
     * @param function input stream processing function
     * @param <R> the result type
     * @return the result of the function
     * @throws IOException in case of an IO failure
     */
    public static <R> R readStream(URL url, Function<InputStream, R> function) throws IOException {
        if (JAR.equals(url.getProtocol())) {
            URLConnection urlConnection = url.openConnection();
            // prevent locking the jar after the inputstream is closed
            urlConnection.setUseCaches(false);
            try (InputStream is = urlConnection.getInputStream()) {
                return function.apply(is);
            }
        }
        if (FILE.equals(url.getProtocol())) {
            try (InputStream is = Files.newInputStream(toLocalPath(url))) {
                return function.apply(is);
            }
        }
        try (InputStream is = url.openStream()) {
            return function.apply(is);
        }
    }

    /**
     * Translates a URL to local file system path.
     * In case the URL couldn't be translated to a file system path,
     * an instance of {@link IllegalArgumentException} will be thrown.
     *
     * @param url URL
     * @return local file system path
     */
    public static Path toLocalPath(final URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to translate " + url + " to local path", e);
        }
    }

    // Avoids initialization when processAsJarPath is not called
    private static final class JarProviderHolder {
        private static final FileSystemProvider JAR_PROVIDER;

        static {
            final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            FileSystemProvider provider = null;
            try {
                Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
                for (FileSystemProvider p : FileSystemProvider.installedProviders()) {
                    if (p.getScheme().equals("jar")) {
                        provider = p;
                        break;
                    }
                }
                if (provider == null) {
                    throw new NoSuchElementException("Unable to find provider supporting jar scheme");
                }
                JAR_PROVIDER = provider;
            } finally {
                Thread.currentThread().setContextClassLoader(ccl);
            }
        }
    }
}
