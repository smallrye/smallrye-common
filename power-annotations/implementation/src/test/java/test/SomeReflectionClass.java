package test;

@SuppressWarnings("unused")
@SomeAnnotation("some-reflection-class")
@RepeatableAnnotation(1)
@RepeatableAnnotation(2)
public class SomeReflectionClass {
    @SomeAnnotation("some-reflection-method")
    @RepeatableAnnotation(3)
    void foo(String x) {
    }

    @SomeAnnotation("some-reflection-field")
    @RepeatableAnnotation(4)
    String bar;
}
