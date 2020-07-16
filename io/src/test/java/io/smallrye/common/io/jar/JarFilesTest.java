package io.smallrye.common.io.jar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

public class JarFilesTest {

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    public void shouldReadMultiReleaseJars() throws IOException {
        File tmpFile = MultiReleaseJarGenerator.generateMultiReleaseJar();
        JarFile jarFile = JarFiles.create(tmpFile);
        assertTrue(jarFile.isMultiRelease());
    }
}
