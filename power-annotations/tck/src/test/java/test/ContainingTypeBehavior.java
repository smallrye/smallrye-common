package test;

import static org.assertj.core.api.BDDAssertions.then;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.common.powerannotations.Annotations;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.ClassWithAnnotationsOnClassAndField;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.ClassWithAnnotationsOnClassAndMethod;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.ClassWithField;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.ClassWithMethod;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.ClassWithRepeatableAnnotationOnClassAndField;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.ClassWithRepeatableAnnotationOnClassAndMethod;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.ClassWithRepeatedAnnotationsForField;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.ClassWithRepeatedAnnotationsForMethod;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.SomeAnnotationWithOnlyTypeTargetAnnotation;
import io.smallrye.common.powerannotations.tck.ContainingTypeClasses.SomeAnnotationWithoutTargetAnnotation;
import io.smallrye.common.powerannotations.tck.RepeatableAnnotation;
import io.smallrye.common.powerannotations.tck.SomeAnnotation;

public class ContainingTypeBehavior {
    @Nested
    class FieldAnnotations {
        Annotations classWithFieldAnnotations = Annotations.onField(ClassWithField.class, "someField");

        @Test
        void shouldGetFieldAnnotationFromClass() {
            Optional<SomeAnnotation> annotation = classWithFieldAnnotations.get(SomeAnnotation.class);

            assert annotation.isPresent();
            then(annotation.get().value()).isEqualTo("class-annotation");
        }

        @Test
        void shouldNotGetFieldAnnotationWithoutTargetAnnotationFromClass() {
            Optional<SomeAnnotationWithoutTargetAnnotation> annotation = classWithFieldAnnotations
                    .get(SomeAnnotationWithoutTargetAnnotation.class);

            then(annotation).isEmpty();
        }

        @Test
        void shouldNotGetFieldAnnotationWithOnlyTypeTargetAnnotationFromClass() {
            Optional<SomeAnnotationWithOnlyTypeTargetAnnotation> annotation = classWithFieldAnnotations
                    .get(SomeAnnotationWithOnlyTypeTargetAnnotation.class);

            then(annotation).isEmpty();
        }

        @Test
        void shouldNotGetAllFieldAnnotationFromClass() {
            Optional<SomeAnnotation> annotation = classWithFieldAnnotations.get(SomeAnnotation.class);

            assert annotation.isPresent();
            then(annotation.get().value()).isEqualTo("class-annotation");
        }

        @Test
        void shouldGetRepeatableFieldAnnotationFromClass() {
            Annotations annotations = Annotations.onField(ClassWithRepeatedAnnotationsForField.class, "someField");

            Stream<RepeatableAnnotation> annotation = annotations.all(RepeatableAnnotation.class);

            then(annotation.map(RepeatableAnnotation::value)).containsExactly(1, 2);
        }

        @Test
        void shouldGetMoreRepeatableFieldAnnotationsFromClass() {
            Annotations annotations = Annotations.onField(ClassWithRepeatableAnnotationOnClassAndField.class, "someField");

            Stream<RepeatableAnnotation> annotation = annotations.all(RepeatableAnnotation.class);

            then(annotation.map(RepeatableAnnotation::value)).containsExactly(1, 2);
        }

        @Test
        void shouldOnlyGetAllFieldAnnotationAndNotFromClass() {
            Annotations annotations = Annotations.onField(ClassWithAnnotationsOnClassAndField.class, "someField");

            Stream<Annotation> list = annotations.all();

            then(list.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + SomeAnnotation.class.getName() + "(value = \"class-annotation\")");
        }
    }

    @Nested
    class MethodAnnotations {
        Annotations classWithMethodAnnotations = Annotations.onMethod(ClassWithMethod.class, "someMethod");

        @Test
        void shouldGetMethodAnnotationFromClass() {
            Optional<SomeAnnotation> annotation = classWithMethodAnnotations.get(SomeAnnotation.class);

            assert annotation.isPresent();
            then(annotation.get().value()).isEqualTo("class-annotation");
        }

        @Test
        void shouldNotGetMethodAnnotationWithoutTargetAnnotationFromClass() {
            Optional<SomeAnnotationWithoutTargetAnnotation> annotation = classWithMethodAnnotations
                    .get(SomeAnnotationWithoutTargetAnnotation.class);

            then(annotation).isEmpty();
        }

        @Test
        void shouldNotGetMethodAnnotationWithOnlyTypeTargetAnnotationFromClass() {
            Optional<SomeAnnotationWithOnlyTypeTargetAnnotation> annotation = classWithMethodAnnotations
                    .get(SomeAnnotationWithOnlyTypeTargetAnnotation.class);

            then(annotation).isEmpty();
        }

        @Test
        void shouldNotGetAllMethodAnnotationFromClass() {
            Optional<SomeAnnotation> annotation = classWithMethodAnnotations.get(SomeAnnotation.class);

            assert annotation.isPresent();
            then(annotation.get().value()).isEqualTo("class-annotation");
        }

        @Test
        void shouldGetRepeatableMethodAnnotationFromClass() {
            Annotations annotations = Annotations.onMethod(ClassWithRepeatedAnnotationsForMethod.class, "someMethod");

            Stream<RepeatableAnnotation> annotation = annotations.all(RepeatableAnnotation.class);

            then(annotation.map(RepeatableAnnotation::value)).containsExactly(1, 2);
        }

        @Test
        void shouldGetMoreRepeatableMethodAnnotationsFromClass() {
            Annotations annotations = Annotations.onMethod(ClassWithRepeatableAnnotationOnClassAndMethod.class, "someMethod");

            Stream<RepeatableAnnotation> annotation = annotations.all(RepeatableAnnotation.class);

            then(annotation.map(RepeatableAnnotation::value)).containsExactly(1, 2);
        }

        @Test
        void shouldGetAllMethodAndClassAnnotations() {
            Annotations annotations = Annotations.onMethod(ClassWithAnnotationsOnClassAndMethod.class, "someMethod");

            Stream<Annotation> list = annotations.all();

            then(list.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + SomeAnnotation.class.getName() + "(value = \"class-annotation\")");
        }
    }
}
