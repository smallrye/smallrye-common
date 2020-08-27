package io.smallrye.common.powerannotations.index;

import static io.smallrye.common.powerannotations.index.MethodInfo.signature;
import static io.smallrye.common.powerannotations.index.Utils.streamOfNullable;
import static io.smallrye.common.powerannotations.index.Utils.toArray;
import static io.smallrye.common.powerannotations.index.Utils.toDotName;
import static io.smallrye.common.powerannotations.index.Utils.toTreeMap;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.jboss.jandex.ClassType;

public class ClassInfo extends AnnotationTarget {
    public static Class<?> toClass(Object value) {
        String className = ((ClassType) value).name().toString();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            // TODO does this work in Quarkus?
            return Class.forName(className, true, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class not found '" + className + "'", e);
        }
    }

    private final org.jboss.jandex.ClassInfo delegate;
    private final Map<String, FieldInfo> fields = new TreeMap<>();
    private Map<String, MethodInfo> methods;

    ClassInfo(Index index, org.jboss.jandex.ClassInfo delegate) {
        super(index);
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        ClassInfo that = (ClassInfo) other;
        return delegate.name().equals(that.delegate.name());
    }

    @Override
    public int hashCode() {
        return delegate.name().hashCode();
    }

    @Override
    public ElementType elementType() {
        return isAnnotationType() ? ANNOTATION_TYPE : TYPE;
    }

    @Override
    public String name() {
        return delegate.name().toString();
    }

    @Override
    protected Stream<org.jboss.jandex.AnnotationInstance> rawAnnotations() {
        return delegate.classAnnotations().stream();
    }

    public boolean isAnnotationType() {
        return implementsInterface(Annotation.class.getName());
    }

    public Stream<ClassInfo> typeTree() {
        return thisAndSuperClasses().flatMap(this::thisAndSuperInterfaces);
    }

    private Stream<ClassInfo> thisAndSuperInterfaces(ClassInfo classInfo) {
        return Stream.concat(Stream.of(classInfo), classInfo.superInterfaces());
    }

    private Stream<ClassInfo> superInterfaces() {
        return delegate.interfaceNames().stream().map(index::classInfo).flatMap(this::thisAndSuperInterfaces);
    }

    private Stream<ClassInfo> thisAndSuperClasses() {
        // Java 9+: return Stream.iterate(this, ClassInfo::hasSuperClass, ClassInfo::superClass);
        Builder<ClassInfo> builder = Stream.builder();
        for (ClassInfo classInfo = this; classInfo.hasSuperClass(); classInfo = classInfo.superClass())
            builder.accept(classInfo);
        return builder.build();
    }

    public boolean hasSuperClass() {
        return delegate.superClassType() != null;
    }

    public ClassInfo superClass() {
        return index.classInfo(delegate.superName());
    }

    public boolean implementsInterface(String typeName) {
        return delegate.interfaceNames().contains(toDotName(typeName));
    }

    public boolean isImplicitlyAllowedOn(ElementType elementType) {
        return isAllowedOn(elementType).orElse(true);
    }

    public boolean isExplicitlyAllowedOn(ElementType elementType) {
        return isAllowedOn(elementType).orElse(false);
    }

    private Optional<Boolean> isAllowedOn(ElementType targetElementType) {
        // would like to `assert isAnnotationType()`, but `this` may not be in the index
        Set<String> allowed = new HashSet<>();
        allowed.add(targetElementType.name());
        if (targetElementType == ANNOTATION_TYPE)
            allowed.add(TYPE.name());
        return annotations(Target.class.getName()).findAny()
                .map(annotationInstance -> annotationValues(annotationInstance).anyMatch(allowed::contains));
    }

    private Stream<String> annotationValues(AnnotationInstance annotationInstance) {
        return annotationInstance.value("value").annotationValues()
                .map(annotationValue -> annotationValue.value(String.class));
    }

    public Stream<FieldInfo> fields() {
        return typeTree().flatMap(c -> c.delegate.fields().stream()).map(this::fieldInfo);
    }

    public Optional<FieldInfo> field(String fieldName) {
        return typeTree().flatMap(c -> streamOfNullable(c.delegate.field(fieldName))).map(this::fieldInfo).findFirst();
    }

    private FieldInfo fieldInfo(org.jboss.jandex.FieldInfo fieldInfo) {
        return fields.computeIfAbsent(fieldInfo.name(), f -> new FieldInfo(index, fieldInfo));
    }

    public Stream<MethodInfo> methods() {
        return getMethods().values().stream();
    }

    private Map<String, MethodInfo> getMethods() {
        if (methods == null) {
            methods = typeTree()
                    .flatMap(c -> c.delegate.methods().stream())
                    .map(methodInfo -> new MethodInfo(this, methodInfo))
                    .filter(MethodInfo::isNotConstructor)
                    .distinct()
                    .collect(toTreeMap(MethodInfo::signature, identity()));
        }
        return methods;
    }

    public Optional<MethodInfo> method(String methodName, Class<?>... argTypes) {
        return method(methodName, Stream.of(argTypes).map(Class::getName).collect(toArray(String.class)));
    }

    public Optional<MethodInfo> method(String methodName, String... argTypeNames) {
        return Optional.ofNullable(getMethods().get(signature(methodName, argTypeNames)));
    }

    public Stream<MethodInfo> findMethod(String signature) {
        return streamOfNullable(getMethods().get(signature));
    }

    public boolean isRepeatableAnnotation() {
        return AnnotationInstance.isRepeatable(delegate);
    }
}
