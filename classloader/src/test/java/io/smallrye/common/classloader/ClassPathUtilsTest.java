package io.smallrye.common.classloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClassPathUtilsTest {
    @Test
    void consumeAsStreams() throws Exception {
        Properties properties = new Properties();
        ClassPathUtils.consumeAsStreams("resources.properties", inputStream -> {
            try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                properties.load(reader);
            } catch (IOException e) {
                fail();
            }
        });
        assertEquals("1234", properties.getProperty("my.prop"));
    }

    @Test
    void consumeAsPaths() throws Exception {
        Properties properties = new Properties();
        ClassPathUtils.consumeAsPaths("resources.properties", path -> {
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path))) {
                properties.load(reader);
            } catch (IOException e) {
                fail();
            }
        });
        assertEquals("1234", properties.getProperty("my.prop"));
    }

    @Test
    void consumeAsPathsWithClassLoader() throws Exception {
        Properties properties = new Properties();
        ClassPathUtils.consumeAsPaths(ClassPathUtils.class.getClassLoader(), "resources.properties", path -> {
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path))) {
                properties.load(reader);
            } catch (IOException e) {
                fail();
            }
        });
        assertEquals("1234", properties.getProperty("my.prop"));
    }

    @Test
    void consumeAsPath() {
        URL resource = ClassPathUtils.class.getClassLoader().getResource("resources.properties");
        Properties properties = new Properties();
        ClassPathUtils.consumeAsPath(resource, path -> {
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path))) {
                properties.load(reader);
            } catch (IOException e) {
                fail();
            }
        });
        assertEquals("1234", properties.getProperty("my.prop"));
    }

    @Test
    void processAsPath(@TempDir Path tempDir) throws Exception {
        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "resources.jar")
                .addAsResource("resources.properties");

        Path filePath = tempDir.resolve("resources.jar");
        jar.as(ZipExporter.class).exportTo(filePath.toFile());

        URI resource = new URI("jar", filePath.toUri().toASCIIString() + "!/resources.properties", null);
        Properties properties = ClassPathUtils.processAsPath(resource.toURL(), path -> {
            Properties properties1 = new Properties();
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path))) {
                properties1.load(reader);
            } catch (IOException e) {
                fail();
            }
            return properties1;
        });

        assertEquals("1234", properties.getProperty("my.prop"));
    }

    @Test
    void readStream(@TempDir Path tempDir) throws Exception {
        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "resources.jar")
                .addAsResource("resources.properties");

        Path filePath = tempDir.resolve("resources.jar");
        jar.as(ZipExporter.class).exportTo(filePath.toFile());

        URI resource = new URI("jar", filePath.toUri().toASCIIString() + "!/resources.properties", null);
        Properties properties = ClassPathUtils.readStream(resource.toURL(), inputStream -> {
            Properties properties1 = new Properties();
            try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                properties1.load(reader);
            } catch (IOException e) {
                fail();
            }
            return properties1;
        });

        assertEquals("1234", properties.getProperty("my.prop"));
    }
}
