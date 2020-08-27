package test;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.smallrye.common.powerannotations.AmbiguousAnnotationResolutionException;
import io.smallrye.common.powerannotations.Annotations;
import io.smallrye.common.powerannotations.tck.RepeatableAnnotation;
import io.smallrye.common.powerannotations.tck.RepeatableAnnotationClasses.RepeatedAnnotationClass;
import io.smallrye.common.powerannotations.tck.RepeatableAnnotationClasses.UnrepeatedAnnotationClass;

public class RepeatableAnnotationBehavior {

    @Test
    void shouldGetSingleRepeatedAnnotation() {
        Annotations annotations = Annotations.on(UnrepeatedAnnotationClass.class);

        Optional<RepeatableAnnotation> annotation = annotations.get(RepeatableAnnotation.class);

        assert annotation.isPresent();
        then(annotation.get().value()).isEqualTo(1);
    }

    Annotations repeatedAnnotations = Annotations.on(RepeatedAnnotationClass.class);

    @Test
    void shouldFailToGetRepeatingAnnotation() {
        Throwable throwable = catchThrowable(() -> repeatedAnnotations.get(RepeatableAnnotation.class));

        then(throwable)
                .isInstanceOf(AmbiguousAnnotationResolutionException.class)
                // TODO message detail about the target .hasMessageContaining(SomeClass.class.getName())
                .hasMessageContaining(RepeatableAnnotation.class.getName());
    }

    @Test
    void shouldGetAll() {
        Stream<Annotation> all = repeatedAnnotations.all();

        then(all.map(Object::toString)).containsExactlyInAnyOrder(
                "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
    }

    @Test
    void shouldGetTypedAll() {
        Stream<RepeatableAnnotation> all = repeatedAnnotations.all(RepeatableAnnotation.class);

        then(all.map(RepeatableAnnotation::value)).containsExactlyInAnyOrder(1, 2);
    }
}
