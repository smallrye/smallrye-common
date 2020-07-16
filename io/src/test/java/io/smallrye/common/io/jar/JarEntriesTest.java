package io.smallrye.common.io.jar;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Test;

public class JarEntriesTest {

    @Test
    public void shouldUseRealName() throws IOException {
        File tmpFile = MultiReleaseJarGenerator.generateMultiReleaseJar();
        JarFile jarFile = JarFiles.create(tmpFile);
        JarEntry jarEntry = jarFile.getJarEntry("foo.txt");
        assertEquals(jarEntry.getRealName(), JarEntries.getRealName(jarEntry));
    }

}
