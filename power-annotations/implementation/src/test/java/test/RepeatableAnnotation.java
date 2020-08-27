package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import test.RepeatableAnnotation.RepeatableAnnotations;

@Retention(RUNTIME)
@Repeatable(RepeatableAnnotations.class)
public @interface RepeatableAnnotation {
    int value();

    @Retention(RUNTIME)
    @interface RepeatableAnnotations {
        RepeatableAnnotation[] value();
    }
}
