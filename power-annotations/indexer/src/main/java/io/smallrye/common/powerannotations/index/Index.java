package io.smallrye.common.powerannotations.index;

import static io.smallrye.common.powerannotations.index.AnnotationInstance.resolveRepeatables;
import static io.smallrye.common.powerannotations.index.Utils.toArray;
import static io.smallrye.common.powerannotations.index.Utils.toDotName;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

public class Index {
    public static Index load() {
        IndexView jandex = loadFromIndexFile();
        if (jandex == null)
            jandex = new Indexer().scanClassPath();
        if (LOG.isLoggable(LEVEL)) {
            LOG.log(LEVEL, "------------------------------------------------------------");
            jandex.getKnownClasses()
                    .forEach(classInfo -> LOG.log(LEVEL, classInfo.name() + " :: " + classInfo.classAnnotations().stream()
                            .filter(instance -> !instance.name().toString().equals("kotlin.Metadata")) // contains binary
                            .map(Object::toString).collect(joining(", "))));
            LOG.log(LEVEL, "------------------------------------------------------------");
        }
        return new Index(jandex);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Index from(InputStream inputStream) {
        return new Index(loadFrom(inputStream));
    }

    private static IndexView loadFromIndexFile() {
        try (InputStream inputStream = getClassLoader().getResourceAsStream("META-INF/jandex.idx")) {
            return (inputStream == null) ? null : loadFrom(inputStream);
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException("can't read index file", e);
        }
    }

    private static IndexView loadFrom(InputStream inputStream) {
        try {
            return new IndexReader(inputStream).read();
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException("can't read Jandex input stream", e);
        }
    }

    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    public static Index fromClassPath() {
        return new Index(new Indexer().scanClassPath());
    }

    final IndexView jandex;
    private final Map<DotName, ClassInfo> classInfos = new TreeMap<>();

    Index(IndexView jandex) {
        this.jandex = requireNonNull(jandex);
    }

    /** abstraction leak */
    @Deprecated
    public IndexView getJandex() {
        return jandex;
    }

    public ClassInfo classInfo(Class<?> type) {
        return classInfo(toDotName(type));
    }

    public ClassInfo classInfo(String type) {
        return classInfo(toDotName(type));
    }

    ClassInfo classInfo(DotName typeName) {
        return classInfos.computeIfAbsent(typeName, t -> new ClassInfo(this, getClassByName(t)));
    }

    private org.jboss.jandex.ClassInfo getClassByName(DotName typeName) {
        org.jboss.jandex.ClassInfo classInfo = jandex.getClassByName(typeName);
        return (classInfo == null) ? mock(typeName) : classInfo;
    }

    @SuppressWarnings("deprecation")
    private static org.jboss.jandex.ClassInfo mock(DotName name) {
        return org.jboss.jandex.ClassInfo.create(
                name, null, (short) PUBLIC, new DotName[0], emptyMap(), true);
    }

    public Stream<AnnotationInstance> allAnnotationInstancesOfType(ClassInfo type) {
        return allAnnotationInstancesOfType(type.name());
    }

    public Stream<AnnotationInstance> allAnnotationInstancesOfType(Class<?> type) {
        return allAnnotationInstancesOfType(toDotName(type));
    }

    public Stream<AnnotationInstance> allAnnotationInstancesOfType(String typeName) {
        return allAnnotationInstancesOfType(toDotName(typeName));
    }

    public Stream<AnnotationInstance> allAnnotationInstancesOfType(DotName typeName) {
        return jandex.getAnnotations(typeName).stream()
                .flatMap(instance -> resolveRepeatables(this, instance));
    }

    public Stream<ClassInfo> annotationTypes() {
        return allClasses().filter(ClassInfo::isAnnotationType);
    }

    public Stream<ClassInfo> allClasses() {
        return jandex.getKnownClasses().stream().map(this::classInfo);
    }

    public AnnotationTarget delegateTarget(org.jboss.jandex.AnnotationTarget target) {
        switch (target.kind()) {
            case CLASS:
                return classInfo(target.asClass());
            case FIELD:
                return fieldInfo(target.asField());
            case METHOD:
                return methodInfo(target.asMethod());
            default:
                throw new UnsupportedOperationException("unsupported target type: " + target.kind());
        }
    }

    private ClassInfo classInfo(org.jboss.jandex.ClassInfo classInfo) {
        return classInfo(classInfo.name().toString());
    }

    private FieldInfo fieldInfo(org.jboss.jandex.FieldInfo fieldInfo) {
        return classInfo(fieldInfo.declaringClass())
                .field(fieldInfo.name())
                .orElseThrow(() -> new RuntimeException("expected field " + fieldInfo));
    }

    private MethodInfo methodInfo(org.jboss.jandex.MethodInfo methodInfo) {
        String[] typeNames = methodInfo.parameters().stream().map(Type::name).map(DotName::toString)
                .collect(toArray(String.class));
        return classInfo(methodInfo.declaringClass())
                .method(methodInfo.name(), typeNames)
                .orElseThrow(() -> new RuntimeException("expected method " + methodInfo));
    }

    private static final Logger LOG = Logger.getLogger(Index.class.getName());
    private static final Level LEVEL = FINE;
}
