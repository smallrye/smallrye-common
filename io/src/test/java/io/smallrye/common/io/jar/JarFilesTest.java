package io.smallrye.common.io.jar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

public class JarFilesTest {

    @Test
    public void shouldReadPlainJars() throws IOException {
        File tmpFile = JarGenerator.generatePlainJar();
        JarFile jarFile = JarFiles.create(tmpFile);
        assertFalse(JarFiles.isMultiRelease(jarFile));
    }

    @Test
    public void shouldReadMultiReleaseJars() throws IOException {
        File tmpFile = JarGenerator.generateMultiReleaseJar();
        JarFile jarFile = JarFiles.create(tmpFile);
        assertTrue(JarFiles.isMultiRelease(jarFile));
    }

}
