package io.smallrye.common.io.jar;

import java.io.File;
import java.io.IOException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class MultiReleaseJarGenerator {
    public static File generateMultiReleaseJar() throws IOException {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .addAsManifestResource(new StringAsset("Multi-Release: true\n"), "MANIFEST.MF")
                .addAsResource(new StringAsset("Original"), "foo.txt")
                .addAsManifestResource(new StringAsset("MultiRelease"), "versions/9/foo.txt");
        File tmpFile = File.createTempFile("tmp", ".tmp");
        jar.as(ZipExporter.class).exportTo(tmpFile, true);
        return tmpFile;
    }
}
