package io.smallrye.common.powerannotations.impl;

import static java.util.stream.Collectors.toList;

import java.util.List;

import io.smallrye.common.powerannotations.index.AnnotationInstance;
import io.smallrye.common.powerannotations.index.ClassInfo;
import io.smallrye.common.powerannotations.index.Index;
import io.smallrye.common.powerannotations.index.MethodInfo;

public class InheritedResolver {
    private final Index index;

    public InheritedResolver(Index index) {
        this.index = index;
    }

    public void resolve() {
        index.allClasses().forEach(this::resolveFromSuperTypes);
    }

    private void resolveFromSuperTypes(ClassInfo classInfo) {
        List<AnnotationInstance> annotations = classInfo.typeTree()
                .flatMap(ClassInfo::annotations)
                .distinct() // can already have been added to a super type
                .collect(toList());
        classInfo.replaceAnnotations(annotations);

        classInfo.methods().forEach(this::resolveFromSuperTypes);
    }

    private void resolveFromSuperTypes(MethodInfo methodInfo) {
        List<AnnotationInstance> annotations = methodInfo.declaringClass().typeTree()
                .flatMap(classInfo -> classInfo.findMethod(methodInfo.signature()))
                .flatMap(MethodInfo::annotations)
                .distinct() // can already have been added to a super type
                .collect(toList());
        methodInfo.replaceAnnotations(annotations);
    }
}
