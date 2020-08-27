package io.smallrye.common.powerannotations.impl;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import io.smallrye.common.powerannotations.index.AnnotationInstance;
import io.smallrye.common.powerannotations.index.ClassInfo;
import io.smallrye.common.powerannotations.index.Index;

class ContainingTypeResolver {
    private final Index index;

    public ContainingTypeResolver(Index index) {
        this.index = index;
    }

    public void resolve() {
        index.allClasses()
                .flatMap(ClassInfo::annotations)
                .forEach(this::resolveToMembers);
    }

    private void resolveToMembers(AnnotationInstance annotationInstance) {
        ClassInfo classInfo = (ClassInfo) annotationInstance.target();
        ClassInfo annotationType = annotationInstance.type();
        if (annotationType.isExplicitlyAllowedOn(FIELD))
            classInfo.fields()
                    .filter(field -> field.canBeAdded(annotationType))
                    .forEach(field -> field.add(annotationInstance));
        if (annotationType.isExplicitlyAllowedOn(METHOD))
            classInfo.methods()
                    .filter(method -> method.canBeAdded(annotationType))
                    .forEach(method -> method.add(annotationInstance));
    }
}
