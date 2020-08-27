package test;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.smallrye.common.powerannotations.AmbiguousAnnotationResolutionException;
import io.smallrye.common.powerannotations.Annotations;
import io.smallrye.common.powerannotations.AnnotationsLoader;

/**
 * If the Jandex index file is not available, silently build the index at runtime.
 * This test also covers the {@link Annotations API} and {@link AnnotationsLoader SPI}
 */
public class DynamicJandexBehavior {

    @Test
    void shouldGetSingleClassAnnotation() {
        Optional<SomeAnnotation> annotation = Annotations.on(SomeReflectionClass.class).get(SomeAnnotation.class);

        assert annotation.isPresent();
        SomeAnnotation someAnnotation = annotation.get();
        then(someAnnotation.annotationType()).isEqualTo(SomeAnnotation.class);
        then(someAnnotation.value()).isEqualTo("some-reflection-class");
    }

    @Test
    void shouldGetAllClassAnnotations() {
        Stream<Annotation> annotations = Annotations.on(SomeReflectionClass.class).all();

        then(annotations.map(Objects::toString)).containsExactlyInAnyOrder(
                "@" + SomeAnnotation.class.getName() + "(value = \"some-reflection-class\")",
                "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
    }

    @Test
    void shouldGetSingleFieldAnnotation() {
        Annotations fieldAnnotations = Annotations.onField(SomeReflectionClass.class, "bar");

        Optional<SomeAnnotation> annotation = fieldAnnotations.get(SomeAnnotation.class);

        assert annotation.isPresent();
        SomeAnnotation someAnnotation = annotation.get();
        then(someAnnotation.annotationType()).isEqualTo(SomeAnnotation.class);
        then(someAnnotation.value()).isEqualTo("some-reflection-field");
    }

    // implementation detail
    @Test
    void shouldFailToGetUnknownFieldAnnotation() {
        Throwable throwable = catchThrowable(() -> Annotations.onField(SomeReflectionClass.class, "unknown"));

        then(throwable).isInstanceOf(RuntimeException.class)
                .hasMessage("no field 'unknown' in " + SomeReflectionClass.class);
    }

    @Test
    void shouldGetSingleMethodAnnotation() {
        Annotations methodAnnotations = Annotations.onMethod(SomeReflectionClass.class, "foo", String.class);

        Optional<SomeAnnotation> annotation = methodAnnotations.get(SomeAnnotation.class);

        assert annotation.isPresent();
        SomeAnnotation someAnnotation = annotation.get();
        then(someAnnotation.annotationType()).isEqualTo(SomeAnnotation.class);
        then(someAnnotation.value()).isEqualTo("some-reflection-method");
    }

    // implementation detail
    @Test
    void shouldFailToGetUnknownMethodAnnotation() {
        Throwable throwable = catchThrowable(() -> Annotations.onMethod(SomeReflectionClass.class, "unknown", String.class));

        then(throwable).isInstanceOf(RuntimeException.class)
                .hasMessage("no method unknown(java.lang.String) in " + SomeReflectionClass.class);
    }

    @Test
    void shouldFailToGetRepeatedAnnotation() {
        Annotations annotations = Annotations.on(SomeReflectionClass.class);

        Throwable throwable = catchThrowable(() -> annotations.get(RepeatableAnnotation.class));

        then(throwable)
                .isInstanceOf(AmbiguousAnnotationResolutionException.class)
                // TODO message detail about the target .hasMessageContaining(SomeReflectionClass.class.getName())
                .hasMessageContaining(RepeatableAnnotation.class.getName());
    }

    @Test
    void shouldGetTypedAll() {
        Annotations annotations = Annotations.on(SomeReflectionClass.class);

        Stream<RepeatableAnnotation> someAnnotations = annotations.all(RepeatableAnnotation.class);

        then(someAnnotations.map(RepeatableAnnotation::value)).containsExactlyInAnyOrder(1, 2);
    }
}
