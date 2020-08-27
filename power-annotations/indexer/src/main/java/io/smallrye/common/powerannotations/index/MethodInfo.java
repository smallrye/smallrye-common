package io.smallrye.common.powerannotations.index;

import static java.lang.annotation.ElementType.METHOD;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.lang.annotation.ElementType;
import java.util.Objects;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

public class MethodInfo extends AnnotationTarget {
    private final ClassInfo declaringClass;
    private final org.jboss.jandex.MethodInfo delegate;

    MethodInfo(ClassInfo declaringClass, org.jboss.jandex.MethodInfo delegate) {
        super(declaringClass.index);
        this.declaringClass = requireNonNull(declaringClass);
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public String toString() {
        return declaringClass.name() + "." + signature();
    }

    public String signature() {
        return signature(delegate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MethodInfo that = (MethodInfo) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public ElementType elementType() {
        return METHOD;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    protected Stream<org.jboss.jandex.AnnotationInstance> rawAnnotations() {
        return delegate.annotations().stream()
                .filter(instance -> instance.target().kind() == Kind.METHOD); // Jandex also returns METHOD_PARAMETER or TYPE
    }

    public boolean isDefaultConstructor() {
        return isConstructor() && delegate.parameters().size() == 0;
    }

    public boolean isConstructor() {
        return name().equals("<init>");
    }

    public boolean isNotConstructor() {
        return !isConstructor();
    }

    public boolean hasNoAnnotations() {
        return delegate.annotations().isEmpty();
    }

    public AnnotationValue defaultValue() {
        return delegate.defaultValue();
    }

    public String[] parameterTypeNames() {
        return parameterTypeNames(delegate);
    }

    public ClassInfo declaringClass() {
        return declaringClass;
    }

    static String signature(org.jboss.jandex.MethodInfo methodInfo) {
        return signature(methodInfo.name(), parameterTypeNames(methodInfo));
    }

    static String signature(String methodName, String... parameterTypeNames) {
        return methodName + Stream.of(parameterTypeNames).collect(joining(", ", "(", ")"));
    }

    private static String[] parameterTypeNames(org.jboss.jandex.MethodInfo methodInfo) {
        return methodInfo.parameters().stream()
                .map(Type::name)
                .map(DotName::toString)
                .collect(Utils.toArray(String.class));
    }
}
