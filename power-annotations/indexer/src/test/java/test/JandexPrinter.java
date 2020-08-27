package test;

import static java.nio.file.Files.newInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;

public class JandexPrinter {

    public static void main(String[] args) {
        JandexPrinter printer = (args.length > 0 && "--classpath".equals(args[0]))
                ? new JandexPrinter(fromClassPath())
                : new JandexPrinter(Paths.get("implementation/target/test-classes/test/indexed/META-INF/jandex.idx"));
        printer.run();
    }

    @SuppressWarnings("deprecation")
    private static IndexView fromClassPath() {
        return io.smallrye.common.powerannotations.index.Index.fromClassPath().getJandex();
    }

    private final IndexView index;

    public JandexPrinter(Path indexFile) {
        this(load(indexFile));
    }

    public JandexPrinter(IndexView index) {
        this.index = index;
    }

    private static IndexView load(Path indexFile) {
        System.out.println("load from " + indexFile);
        try (InputStream inputStream = new BufferedInputStream(newInputStream(indexFile))) {
            return new IndexReader(inputStream).read();
        } catch (IOException e) {
            throw new RuntimeException("can't load Jandex index file", e);
        }
    }

    private void run() {
        System.out.println("------------------------------------------------------------");
        ((Index) index).printAnnotations();
        System.out.println("------------------------------------------------------------");
        ((Index) index).printSubclasses();
        System.out.println("------------------------------------------------------------");
        index.getKnownClasses().forEach(classInfo -> {
            if (!classInfo.name().toString().startsWith("test."))
                return;
            System.out.println(classInfo.name() + ":");
            classInfo.methods()
                    .forEach(method -> System.out.println("    " + method.name() + " [" + method.defaultValue() + "]"));
        });
        System.out.println("------------------------------------------------------------");
    }
}
