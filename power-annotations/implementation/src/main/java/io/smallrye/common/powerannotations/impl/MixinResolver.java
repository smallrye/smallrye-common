package io.smallrye.common.powerannotations.impl;

import io.smallrye.common.powerannotations.MixinFor;
import io.smallrye.common.powerannotations.index.AnnotationInstance;
import io.smallrye.common.powerannotations.index.ClassInfo;
import io.smallrye.common.powerannotations.index.FieldInfo;
import io.smallrye.common.powerannotations.index.Index;
import io.smallrye.common.powerannotations.index.MethodInfo;

class MixinResolver {
    private final Index index;

    MixinResolver(Index index) {
        this.index = index;
    }

    public void resolve() {
        index.allAnnotationInstancesOfType(MixinFor.class).forEach(this::resolve);
    }

    private void resolve(AnnotationInstance mixinAnnotation) {
        ClassInfo mixinTarget = mixinAnnotation.value("value").classValue();
        ClassInfo mixinClass = (ClassInfo) mixinAnnotation.target();
        if (mixinTarget.isAnnotationType()) {
            resolveMixinAnnotations(mixinTarget, mixinClass);
        } else {
            resolveClassAnnotations(mixinTarget, mixinClass);
            resolveFieldAnnotations(mixinTarget, mixinClass);
            resolveMethodAnnotations(mixinTarget, mixinClass);
        }
    }

    private void resolveMixinAnnotations(ClassInfo mixinTarget, ClassInfo mixinClass) {
        index.allAnnotationInstancesOfType(mixinTarget)
                .map(annotationInstance -> (ClassInfo) annotationInstance.target())
                .forEach(mixinTargetType -> resolveClassAnnotations(mixinTargetType, mixinClass));
    }

    private void resolveClassAnnotations(ClassInfo mixinTarget, ClassInfo mixinClass) {
        mixinClass.annotations()
                .filter(annotationInstance -> annotationInstance.type().isImplicitlyAllowedOn(mixinTarget.elementType()))
                .filter(annotationInstance -> !annotationInstance.type().name().equals(MixinFor.class.getName()))
                .forEach(mixinTarget::replace);
    }

    private void resolveFieldAnnotations(ClassInfo mixinTarget, ClassInfo mixinClass) {
        mixinClass.fields().forEach(field -> {
            FieldInfo targetField = mixinTarget.field(field.name())
                    .orElseThrow(() -> new IllegalArgumentException(mixinClass.name() + " mixes field " + field.name()
                            + " into " + mixinTarget + " but there is no such field"));
            field.annotations()
                    .filter(annotationInstance -> annotationInstance.type().isImplicitlyAllowedOn(targetField.elementType()))
                    .forEach(targetField::replace);
        });
    }

    private void resolveMethodAnnotations(ClassInfo mixinTarget, ClassInfo mixinClass) {
        mixinClass.methods().forEach(method -> {
            if (method.isDefaultConstructor() && method.hasNoAnnotations())
                return;
            MethodInfo targetMethod = mixinTarget.method(method.name(), method.parameterTypeNames())
                    .orElseThrow(() -> new IllegalArgumentException(mixinClass.name() + " mixes method " + method
                            + " into " + mixinTarget + " but there is no such method"));
            method.annotations()
                    .filter(annotationInstance -> annotationInstance.type().isImplicitlyAllowedOn(targetMethod.elementType()))
                    .forEach(targetMethod::replace);
        });
    }
}
