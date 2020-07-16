package io.smallrye.common.io.jar;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.junit.Test;

public class JarFilesTest {

    @Test
    public void shouldReadMultiReleaseJars() throws IOException {
        File tmpFile = MultiReleaseJarGenerator.generateMultiReleaseJar();
        JarFile jarFile = JarFiles.create(tmpFile);
        assertTrue(jarFile.isMultiRelease());
    }
}
