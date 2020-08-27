package io.smallrye.common.powerannotations.index;

import static io.smallrye.common.powerannotations.index.AnnotationInstance.resolveRepeatables;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class AnnotationTarget {
    protected final Index index;
    private List<AnnotationInstance> annotations;

    public AnnotationTarget(Index index) {
        this.index = requireNonNull(index);
    }

    public abstract ElementType elementType();

    public abstract String name();

    public Stream<AnnotationInstance> annotations(String name) {
        return annotations()
                .filter(annotationInstance -> annotationInstance.typeName().equals(name));
    }

    public final Stream<AnnotationInstance> annotations() {
        return getAnnotations().stream();
    }

    public List<AnnotationInstance> getAnnotations() {
        if (annotations == null)
            annotations = rawAnnotations()
                    .flatMap(instance -> resolveRepeatables(index, instance))
                    .collect(toList());
        return annotations;
    }

    /**
     * Replace all annotations, while checking that the annotations can actually still be added (e.g. non-repeatables)
     */
    public void replaceAnnotations(List<AnnotationInstance> annotations) {
        this.annotations = new ArrayList<>();
        annotations.stream()
                .filter(this::canBeAdded)
                .forEach(this::add);
    }

    protected abstract Stream<org.jboss.jandex.AnnotationInstance> rawAnnotations();

    public boolean isAnnotationPresent(String typeName) {
        return annotations(typeName).findAny().isPresent();
    }

    public void replace(AnnotationInstance instance) {
        ClassInfo annotationType = instance.type();
        assert annotationType.isImplicitlyAllowedOn(elementType());
        if (!annotationType.isRepeatableAnnotation())
            getAnnotations().removeIf(annotation -> annotation.type().equals(annotationType));
        add(instance);
    }

    public void add(AnnotationInstance instance) {
        assert canBeAdded(instance);
        getAnnotations().add(instance.cloneWithTarget(this));
    }

    public boolean canBeAdded(AnnotationInstance instance) {
        return canBeAdded(instance.type());
    }

    public boolean canBeAdded(ClassInfo annotationType) {
        return (annotationType.isRepeatableAnnotation()
                || !isAnnotationPresent(annotationType.name()))
                && annotationType.isImplicitlyAllowedOn(elementType());
    }
}
