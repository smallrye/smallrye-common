package io.smallrye.common.powerannotations.impl;

import static java.util.stream.Collectors.joining;

import java.lang.annotation.Annotation;
import java.util.List;

import io.smallrye.common.powerannotations.AmbiguousAnnotationResolutionException;
import io.smallrye.common.powerannotations.index.AnnotationTarget;

public class PowerAnnotationsAmbiguousAnnotationResolutionException extends AmbiguousAnnotationResolutionException {
    public <T extends Annotation> PowerAnnotationsAmbiguousAnnotationResolutionException(
            Class<T> type, AnnotationTarget target, List<? extends Annotation> list) {
        super(type.getName() + " is ambiguous on " + target + ":"
                + list.stream().map(Annotation::toString).collect(joining("\n- ", "\n- ", "\n")));
    }
}
