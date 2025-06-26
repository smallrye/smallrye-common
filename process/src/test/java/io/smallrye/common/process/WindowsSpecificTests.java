package io.smallrye.common.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.common.os.OS;

public class WindowsSpecificTests {
    @Test
    public void testBatchExecution() throws Exception {
        assumeTrue(OS.current() == OS.WINDOWS);
        List<String> output = ProcessBuilder.newBuilder(findScript("batch/hello_world.cmd"))
                .output().toStringList(16, 1024)
                .run();
        assertEquals(List.of("Hello world!"), output);
    }

    @Test
    public void testBatchArguments() throws Exception {
        assumeTrue(OS.current() == OS.WINDOWS);
        List<String> output = ProcessBuilder.newBuilder(findScript("batch/echo_2_args.cmd"))
                .arguments("hello", "world")
                .output().toStringList(16, 1024)
                .run();
        assertEquals(List.of("hello", "world"), output);

        output = ProcessBuilder.newBuilder(findScript("batch/echo_2_args.cmd"))
                .arguments("with spaces", "with ^ caret")
                .output().toStringList(16, 1024)
                .run();
        assertEquals(List.of("with spaces", "with ^ caret"), output);

        output = ProcessBuilder.newBuilder(findScript("batch/echo_2_args.cmd"))
                .arguments("\"withQuotes\"", "\"quotes and spaces\"")
                .output().toStringList(16, 1024)
                .run();
        assertEquals(List.of("withQuotes", "quotes and spaces"), output);

        output = ProcessBuilder.newBuilder(findScript("batch/echo_2_args.cmd"))
                .arguments("\"with \"interior\" quotes\"", "it works!")
                .output().toStringList(16, 1024)
                .run();
        assertEquals(List.of("with \"interior\" quotes", "it works!"), output);
    }

    @Test
    public void testBatchRejectedArguments() {
        assumeTrue(OS.current() == OS.WINDOWS);
        assertThrows(IllegalArgumentException.class, () -> {
            ProcessBuilder.newBuilder(Path.of("ignored.cmd"))
                    .arguments("\0");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ProcessBuilder.newBuilder(Path.of("ignored.cmd"))
                    .arguments("\"quoted at start\" but not at end");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ProcessBuilder.newBuilder(Path.of("ignored.cmd"))
                    .arguments("not at start \"but quoted at end\"");
        });
    }

    @Test
    public void testPowershellExecution() throws Exception {
        assumeTrue(OS.current() == OS.WINDOWS);
        List<String> output = ProcessBuilder.newBuilder(findScript("powershell/hello_world.ps1"))
                .output().toStringList(16, 1024)
                .run();
        assertEquals(List.of("Hello world!"), output);
    }

    @Test
    public void testPowershellArguments() throws Exception {
        assumeTrue(OS.current() == OS.WINDOWS);
        List<String> output = ProcessBuilder.newBuilder(findScript("powershell/echo_args.ps1"))
                .arguments("hello", "world")
                .output().toStringList(16, 1024)
                .run();
        assertEquals(List.of("hello", "world"), output);
    }

    private static Path findScript(final String scriptName) throws URISyntaxException {
        URL url = WindowsSpecificTests.class.getClassLoader().getResource(scriptName);
        if (url == null) {
            throw new IllegalStateException("No resource found for " + scriptName);
        }
        if (!url.getProtocol().equals("file")) {
            throw new IllegalStateException("Wrong protocol: " + url);
        }
        return Path.of(url.toURI());
    }
}
