package io.smallrye.common.io.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

// This needs to be run as an integration-test
public class JarEntriesIT {

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    public void shouldUseMultiReleaseName() throws IOException {
        File tmpFile = MultiReleaseJarGenerator.generateMultiReleaseJar();
        JarFile jarFile = JarFiles.create(tmpFile);
        JarEntry jarEntry = jarFile.getJarEntry("foo.txt");
        assertEquals("META-INF/versions/9/foo.txt", JarEntries.getRealName(jarEntry));
    }

    @Test
    @EnabledOnJre(JRE.JAVA_8)
    public void shouldUseName() throws IOException {
        File tmpFile = MultiReleaseJarGenerator.generateMultiReleaseJar();
        JarFile jarFile = JarFiles.create(tmpFile);
        JarEntry jarEntry = jarFile.getJarEntry("foo.txt");
        assertEquals("foo.txt", JarEntries.getRealName(jarEntry));
    }

}
