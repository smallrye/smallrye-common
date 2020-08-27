package io.smallrye.common.powerannotations.impl;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import io.smallrye.common.powerannotations.Annotations;
import io.smallrye.common.powerannotations.AnnotationsLoader;
import io.smallrye.common.powerannotations.index.AnnotationTarget;
import io.smallrye.common.powerannotations.index.Index;
import io.smallrye.common.powerannotations.index.Utils;

public class PowerAnnotationsLoader extends AnnotationsLoader {
    final Index index;

    /** Used by the ServiceLoader */
    @SuppressWarnings("unused")
    public PowerAnnotationsLoader() {
        this(Index.load());
    }

    /** visible for testing: we need to load different index files */
    public PowerAnnotationsLoader(Index index) {
        this.index = index;
        resolve();
    }

    private void resolve() {
        new InheritedResolver(index).resolve();
        new StereotypeResolver(index).resolve();
        new MixinResolver(index).resolve();
        new ContainingTypeResolver(index).resolve();
    }

    @Override
    public Annotations onType(Class<?> type) {
        return new PowerAnnotations(index.classInfo(type));
    }

    @Override
    public Annotations onField(Class<?> type, String fieldName) {
        return new PowerAnnotations(index.classInfo(type).field(fieldName)
                .orElseThrow(() -> new FieldNotFoundException(fieldName, type)));
    }

    @Override
    public Annotations onMethod(Class<?> type, String methodName, Class<?>... argTypes) {
        String[] argTypeNames = Stream.of(argTypes).map(Class::getName).collect(Utils.toArray(String.class));
        return new PowerAnnotations(index.classInfo(type).method(methodName, argTypeNames)
                .orElseThrow(() -> new MethodNotFoundException(type, methodName, argTypeNames)));
    }

    private static class PowerAnnotations implements Annotations {
        private final AnnotationTarget annotationTarget;

        public PowerAnnotations(AnnotationTarget annotationTarget) {
            this.annotationTarget = annotationTarget;
        }

        @Override
        public Stream<Annotation> all() {
            return annotationTarget.annotations().map(AnnotationProxy::proxy);
        }

        @Override
        public <T extends Annotation> Optional<T> get(Class<T> type) {
            return all(type)
                    .collect(io.smallrye.common.powerannotations.impl.Utils.toOptionalOrThrow(
                            list -> new PowerAnnotationsAmbiguousAnnotationResolutionException(type, annotationTarget, list)));
        }

        @Override
        public <T extends Annotation> Stream<T> all(Class<T> type) {
            return annotationTarget.annotations()
                    .filter(instance -> instance.typeName().equals(type.getName()))
                    .map(AnnotationProxy::proxy)
                    .map(type::cast);
        }
    }
}
