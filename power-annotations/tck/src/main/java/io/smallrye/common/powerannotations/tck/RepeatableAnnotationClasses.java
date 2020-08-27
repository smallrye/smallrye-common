package io.smallrye.common.powerannotations.tck;

public class RepeatableAnnotationClasses {
    @RepeatableAnnotation(1)
    public static class UnrepeatedAnnotationClass {
    }

    @RepeatableAnnotation(1)
    @RepeatableAnnotation(2)
    public static class RepeatedAnnotationClass {
    }
}
