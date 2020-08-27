package io.smallrye.common.powerannotations.index;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.stream.Stream;

public class AnnotationValue {
    public static Object of(Object value) {
        return ((org.jboss.jandex.AnnotationValue) value).value();
    }

    private final Optional<Index> index;
    private final org.jboss.jandex.AnnotationValue value;

    AnnotationValue(Optional<Index> index, org.jboss.jandex.AnnotationValue value) {
        this.index = index;
        this.value = requireNonNull(value);
    }

    public Object value() {
        return this.value.value();
    }

    public <T> T value(Class<T> type) {
        return type.cast(this.value.value());
    }

    public Stream<AnnotationValue> annotationValues() {
        return Stream.of(value(org.jboss.jandex.AnnotationValue[].class))
                .map(v -> new AnnotationValue(index, v));
    }

    public ClassInfo classValue() {
        return index().classInfo(this.value.asClass().name());
    }

    private Index index() {
        return index.orElseThrow(() -> new UnsupportedOperationException("not supported for meta annotations"));
    }
}
