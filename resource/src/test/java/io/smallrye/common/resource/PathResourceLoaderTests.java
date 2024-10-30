package io.smallrye.common.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public final class PathResourceLoaderTests {

    @Test
    public void testLoading() throws IOException, URISyntaxException {
        URL myClass = PathResourceLoaderTests.class.getResource("PathResourceLoaderTests.class");
        assumeTrue(myClass != null);
        assumeTrue("file".equals(myClass.getProtocol()));
        Path testClasses = Path.of(myClass.toURI()).getParent().getParent().getParent().getParent().getParent();
        byte[] myClassBytes;
        try (InputStream is = myClass.openStream()) {
            assumeTrue(is != null);
            myClassBytes = is.readAllBytes();
        }
        try (PathResourceLoader rl = new PathResourceLoader(testClasses)) {
            Resource myClassRsrc = rl.findResource("io/smallrye/common/resource/PathResourceLoaderTests.class");
            assertNotNull(myClassRsrc);
            try (InputStream is = myClassRsrc.openStream()) {
                assertArrayEquals(myClassBytes, is.readAllBytes());
            }
            URL url = myClassRsrc.url();
            assertInstanceOf(ResourceURLConnection.class, url.openConnection());
        }
    }
}
