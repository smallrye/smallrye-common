package io.smallrye.common.powerannotations.index;

import static java.lang.annotation.ElementType.FIELD;
import static java.util.Objects.requireNonNull;

import java.lang.annotation.ElementType;
import java.util.stream.Stream;

public class FieldInfo extends AnnotationTarget {
    private final org.jboss.jandex.FieldInfo delegate;

    FieldInfo(Index index, org.jboss.jandex.FieldInfo delegate) {
        super(index);
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public ElementType elementType() {
        return FIELD;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    protected Stream<org.jboss.jandex.AnnotationInstance> rawAnnotations() {
        return delegate.annotations().stream();
    }
}
