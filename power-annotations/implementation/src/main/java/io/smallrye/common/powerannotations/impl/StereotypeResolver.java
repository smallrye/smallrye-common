package io.smallrye.common.powerannotations.impl;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.Stream;

import io.smallrye.common.powerannotations.index.AnnotationInstance;
import io.smallrye.common.powerannotations.index.AnnotationTarget;
import io.smallrye.common.powerannotations.index.ClassInfo;
import io.smallrye.common.powerannotations.index.Index;

class StereotypeResolver implements AnnotationResolver {
    private final Index index;

    StereotypeResolver(Index index) {
        this.index = index;
    }

    @Override
    public void resolve() {
        index.annotationTypes()
                .filter(StereotypeResolver::isStereotype)
                .sorted(comparing(StereotypeResolver::stereotypeLevel) // resolve indirect stereotypes first
                        .thenComparing(ClassInfo::name)) // for more control in tests
                .forEach(this::resolve);
    }

    private static boolean isStereotype(ClassInfo classInfo) {
        return classInfo.annotations()
                .map(AnnotationInstance::typeName)
                .anyMatch(StereotypeResolver::isStereotypeName);
    }

    private static boolean isStereotypeName(String typeName) {
        return typeName.endsWith(".Stereotype");
    }

    private static int stereotypeLevel(ClassInfo stereotypeType) {
        return stereotypeType.annotations()
                .map(AnnotationInstance::type)
                .filter(StereotypeResolver::isStereotype)
                .map(StereotypeResolver::stereotypeLevel)
                .max(Integer::compareTo)
                .map(i -> i + 1)
                .orElse(0);
    }

    private void resolve(ClassInfo stereotypeType) {
        index.allAnnotationInstancesOfType(stereotypeType)
                .forEach(annotationInstance -> resolve(stereotypeType, annotationInstance.target()));
    }

    private void resolve(ClassInfo stereotypeType, AnnotationTarget target) {
        stereotypeType.annotations()
                .filter(StereotypeResolver::shouldBeResolved)
                .filter(target::canBeAdded)
                // .peek(instance -> System.out.println("add " + instance + " from " + stereotypeType + " to " + target))
                .forEach(target::add);
    }

    private static boolean shouldBeResolved(AnnotationInstance annotation) {
        return !isStereotypeName(annotation.typeName())
                && !DO_NON_RESOLVE.contains(annotation.typeName());
    }

    private static final List<String> DO_NON_RESOLVE = Stream.of(Retention.class, Target.class)
            .map(Class::getTypeName)
            .collect(toList());
}
