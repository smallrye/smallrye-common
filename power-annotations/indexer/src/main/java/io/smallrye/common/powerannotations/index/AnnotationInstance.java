package io.smallrye.common.powerannotations.index;

import static io.smallrye.common.powerannotations.index.Utils.toDotName;
import static java.util.Objects.requireNonNull;
import static org.jboss.jandex.AnnotationValue.Kind.ARRAY;
import static org.jboss.jandex.AnnotationValue.Kind.NESTED;

import java.lang.annotation.Repeatable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;

public class AnnotationInstance {
    public static AnnotationInstance from(Object value) {
        return new AnnotationInstance(Optional.empty(), (org.jboss.jandex.AnnotationInstance) value);
    }

    static boolean isRepeatable(org.jboss.jandex.ClassInfo classInfo) {
        return classInfo.classAnnotation(REPEATABLE) != null;
    }

    /** the stream of one single or several repeatable annotations */
    static Stream<AnnotationInstance> resolveRepeatables(Index index, org.jboss.jandex.AnnotationInstance instance) {
        if (isRepeatable(index, instance))
            return resolveRepeatable(index, instance);
        return Stream.of(new AnnotationInstance(Optional.of(index), instance));
    }

    private static boolean isRepeatable(Index index, org.jboss.jandex.AnnotationInstance instance) {
        if (instance.values().size() == 1
                && instance.values().get(0).name().equals("value")
                && instance.value().kind() == ARRAY
                && instance.value().componentKind() == NESTED
                && instance.value().asNestedArray().length > 0) {
            org.jboss.jandex.AnnotationInstance annotationInstance = instance.value().asNestedArray()[0];
            org.jboss.jandex.ClassInfo classInfo = index.jandex.getClassByName(annotationInstance.name());
            return classInfo.classAnnotation(REPEATABLE) != null;
        }
        return false;
    }

    private static final DotName REPEATABLE = Utils.toDotName(Repeatable.class);

    private static Stream<AnnotationInstance> resolveRepeatable(Index index, org.jboss.jandex.AnnotationInstance repeatable) {
        return Stream.of((org.jboss.jandex.AnnotationValue[]) repeatable.value().value())
                .map(annotationValue -> resolveAnnotationInstance(annotationValue, repeatable.target()))
                .map(annotationInstance -> new AnnotationInstance(Optional.of(index), annotationInstance));
    }

    private static org.jboss.jandex.AnnotationInstance resolveAnnotationInstance(
            org.jboss.jandex.AnnotationValue annotationValue, org.jboss.jandex.AnnotationTarget repeatable) {
        org.jboss.jandex.AnnotationInstance value = (org.jboss.jandex.AnnotationInstance) annotationValue.value();
        if (value.target() == null)
            value = org.jboss.jandex.AnnotationInstance.create(value.name(), repeatable, value.values());
        return value;
    }

    /** The Index is null for meta annotations, i.e. when the annotation instance is on another annotation */
    private final Optional<Index> index;
    private final org.jboss.jandex.AnnotationInstance delegate;
    private final Optional<AnnotationTarget> target;

    private AnnotationInstance(Optional<Index> index, org.jboss.jandex.AnnotationInstance delegate) {
        this.index = index;
        this.delegate = requireNonNull(delegate);
        this.target = index.map(i -> i.delegateTarget(delegate.target()));
    }

    private AnnotationInstance(AnnotationInstance instance, AnnotationTarget target) {
        this.index = instance.index;
        this.delegate = instance.delegate;
        this.target = Optional.of(target);
    }

    public AnnotationInstance cloneWithTarget(AnnotationTarget target) {
        return new AnnotationInstance(this, target);
    }

    @Override
    public String toString() {
        return delegate.toString(false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnnotationInstance that = (AnnotationInstance) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    public String typeName() {
        return delegate.name().toString();
    }

    public ClassInfo type() {
        return index().classInfo(delegate.name());
    }

    private Index index() {
        return index.orElseThrow(this::notForMetaAnnotations);
    }

    public AnnotationValue value(String name) {
        org.jboss.jandex.AnnotationValue value = delegate.value(name);
        if (value == null)
            value = defaultValue(name);
        return new AnnotationValue(index, value);
    }

    private org.jboss.jandex.AnnotationValue defaultValue(String name) {
        return type().method(name, new String[0]) // annotation properties don't take args
                .orElseThrow(() -> new RuntimeException("no value '" + name + "' in " + this))
                .defaultValue();
    }

    public AnnotationTarget target() {
        return target.orElseThrow(this::notForMetaAnnotations);
    }

    private UnsupportedOperationException notForMetaAnnotations() {
        return new UnsupportedOperationException("not supported for meta annotations");
    }
}
