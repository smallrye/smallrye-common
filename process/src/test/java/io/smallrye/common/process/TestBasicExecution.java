package io.smallrye.common.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.smallrye.common.process.helpers.Cat;
import io.smallrye.common.process.helpers.Errorifier;
import io.smallrye.common.process.helpers.ErrorifierWithOutput;

public class TestBasicExecution {

    @Test
    public void testSimpleCat() throws Exception {
        List<String> strings = List.of("Hello", "world", "foo", "bar");
        List<String> result = ProcessBuilder.newBuilder(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .input()
                .fromStrings(strings)
                .output()
                .toStringList(10, 1024)
                .run();
        assertEquals(strings, result);
    }

    @Test
    public void testSimpleCatWithTee() throws Exception {
        List<String> strings = List.of("Hello", "world", "foo", "bar");
        List<String> result = ProcessBuilder.newBuilder(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .input()
                .fromStrings(strings)
                .output()
                .toStringList(10, 1024)
                .copyAndConsumeLinesWith(256, System.out::println)
                .copyAndConsumeLinesWith(256, System.out::println)
                .run();
        assertEquals(strings, result);
    }

    @Test
    public void testWhileRunning() throws Exception {
        var holder = new Object() {
            volatile boolean done;
        };
        ProcessBuilder.newBuilder(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .whileRunning(ph -> {
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                        fail("Unexpected interruption");
                    }
                    holder.done = true;
                })
                .run();
        // should wait for while-running to finish
        assertTrue(holder.done);
    }

    @Test
    public void testCaptureError() throws Exception {
        ArrayDeque<String> q = new ArrayDeque<>();
        ProcessBuilder.newBuilder(ProcessUtil.pathOfJava(), findHelper(Errorifier.class, "0"))
                .error()
                .consumeLinesWith(256, q::add)
                .run();
        assertEquals("Some error text", q.removeFirst());
        assertTrue(q.isEmpty());
    }

    @Test
    public void testGatherErrorAndOutput() throws Exception {
        ArrayDeque<String> oq = new ArrayDeque<>();
        ArrayDeque<String> eq = new ArrayDeque<>();
        try {
            ProcessBuilder.newBuilder(ProcessUtil.pathOfJava(), findHelper(ErrorifierWithOutput.class, "1"))
                    .output()
                    .gatherOnFail(true)
                    .consumeLinesWith(256, oq::add)
                    .error()
                    .consumeLinesWith(256, eq::add)
                    .run();
            fail("Expected exception");
        } catch (ProcessExecutionException ex) {
            String es = ex.toString();
            assertTrue(es.contains("Some output text"));
            assertTrue(es.contains("Some error text"));
        }
        assertEquals("Some output text", oq.removeFirst());
        assertTrue(oq.isEmpty());
        assertEquals("Some error text", eq.removeFirst());
        assertTrue(eq.isEmpty());
    }

    @Test
    public void testFailure() throws Exception {
        try {
            ProcessBuilder.newBuilder(ProcessUtil.pathOfJava(), findHelper(Errorifier.class, "7"))
                    .run();
            fail("Expected specific exception");
        } catch (AbnormalExitException e) {
            assertEquals(7, e.exitCode());
        }
    }

    @Test
    public void testFailingPipeline() throws Exception {
        try {
            ProcessBuilder.newBuilder(ProcessUtil.pathOfJava(), findHelper(Errorifier.class, "1"))
                    .output()
                    .pipeTo(ProcessUtil.pathOfJava(), findHelper(Errorifier.class, "2"))
                    .output()
                    .pipeTo(ProcessUtil.pathOfJava(), findHelper(Errorifier.class, "3"))
                    .output()
                    .pipeTo(ProcessUtil.pathOfJava(), findHelper(Errorifier.class, "4"))
                    .output()
                    .pipeTo(ProcessUtil.pathOfJava(), findHelper(Errorifier.class, "5"))
                    .output()
                    .pipeTo(ProcessUtil.pathOfJava(), findHelper(Errorifier.class, "6"))
                    .run();
            fail("Expected exception");
        } catch (PipelineExecutionException e) {
            Throwable[] suppressed = e.getSuppressed();
            assertEquals(6, suppressed.length);
        }
    }

    @Test
    public void testLongPipeline() throws Exception {
        String output = ProcessBuilder.newBuilder(ProcessUtil.pathOfJava())
                .arguments(findHelper(Cat.class))
                .input()
                .fromString("Hello world!")
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .toSingleString(1024)
                .run();
        assertEquals("Hello world!", output);
    }

    @Test
    public void testSplitPipeline() throws Exception {
        ArrayDeque<String> q = new ArrayDeque<>();
        String output = ProcessBuilder.newBuilder(ProcessUtil.pathOfJava())
                .arguments(findHelper(Cat.class))
                .input()
                .fromString("Hello world!")
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .copyAndConsumeLinesWith(1024, q::add)
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .pipeTo(ProcessUtil.pathOfJava(), findHelper(Cat.class))
                .output()
                .toSingleString(1024)
                .run();
        assertEquals("Hello world!", output);
        assertEquals("Hello world!", q.removeFirst());
        assertTrue(q.isEmpty());
    }

    /**
     * Build a command line for the {@code java} command to execute a class which is on our class path.
     * This allows us to test process execution on any platform.
     *
     * @param helperClass the class to execute (must not be {@code null})
     * @param args the arguments to pass, if any (must not be {@code null})
     * @return the command line arguments to pass to {@code java} as a string list (not {@code null})
     * @throws URISyntaxException if something has gone off the rails
     */
    static List<String> findHelper(Class<?> helperClass, String... args) throws URISyntaxException {
        String classFileName = helperClass.getName().replace('.', '/').concat(".class");
        URL url = helperClass.getClassLoader().getResource(classFileName);
        if (url == null) {
            throw new IllegalArgumentException("No resource for " + helperClass);
        }
        URI uri = url.toURI();
        String classPath = switch (uri.getScheme()) {
            case "file" -> {
                String uriPath = uri.getPath();
                if (uriPath.endsWith(classFileName)) {
                    yield uriPath.substring(0, uriPath.length() - classFileName.length());
                } else {
                    throw new IllegalArgumentException("Invalid class path");
                }
            }
            case "jar" -> {
                String uriFile = uri.getPath();
                if (uriFile.startsWith("file:")) {
                    int idx = uriFile.indexOf("!/");
                    if (idx != -1) {
                        yield uriFile.substring(5, idx);
                    }
                }
                throw new IllegalArgumentException("Invalid class path");
            }
            default -> throw new IllegalArgumentException("Invalid class path");
        };
        return Stream.concat(Stream.of("-classpath", classPath, helperClass.getName()), Stream.of(args)).toList();
    }
}
