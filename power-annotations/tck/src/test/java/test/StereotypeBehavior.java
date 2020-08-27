package test;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.BDDAssertions.then;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.common.powerannotations.Annotations;
import io.smallrye.common.powerannotations.tck.RepeatableAnnotation;
import io.smallrye.common.powerannotations.tck.SomeAnnotation;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.AnotherStereotype;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.ClassWithStereotypedField;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.ClassWithStereotypedMethod;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.DoubleIndirectlyStereotypedClass;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.DoubleStereotypedClass;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.IndirectlyStereotypedClass;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.SomeDoubleIndirectedStereotype;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.SomeIndirectedStereotype;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.SomeStereotype;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.SomeTardyIndirectedStereotype;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.StereotypedClass;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.StereotypedClassWithSomeAnnotation;
import io.smallrye.common.powerannotations.tck.StereotypeClasses.TardyIndirectlyStereotypedClass;

public class StereotypeBehavior {

    @Nested
    class StereotypedClasses {
        Annotations annotations = Annotations.on(StereotypedClass.class);

        @Test
        void shouldGetAnnotationFromClassStereotype() {
            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("some-stereotype");
        }

        @Test
        void shouldGetAllAnnotationsFromClassStereotype() {
            Stream<Annotation> someAnnotation = annotations.all();

            then(someAnnotation.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 5)",
                    "@" + SomeStereotype.class.getName(),
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetAllNonRepeatableAnnotationsFromClassStereotype() {
            Stream<SomeAnnotation> someAnnotation = annotations.all(SomeAnnotation.class);

            then(someAnnotation.map(Objects::toString)).containsExactlyInAnyOrder(
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")");
        }

        @Test
        void shouldGetAllRepeatableAnnotationFromClassStereotype() {
            Stream<RepeatableAnnotation> someAnnotation = annotations.all(RepeatableAnnotation.class);

            then(someAnnotation.map(Objects::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 5)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetAllFromIndirectClassStereotype() {
            Annotations annotations = Annotations.on(IndirectlyStereotypedClass.class);

            Stream<Annotation> all = annotations.all();

            then(all.map(Objects::toString)).containsExactlyInAnyOrder(
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)",
                    "@" + SomeStereotype.class.getName(),
                    "@" + SomeIndirectedStereotype.class.getName());
        }

        @Test
        void shouldGetAllFromIndirectClassStereotypeResolvedAlphabeticallyAfterSomeStereotype() {
            Annotations annotations = Annotations.on(TardyIndirectlyStereotypedClass.class);

            Stream<Annotation> all = annotations.all();

            then(all.map(Objects::toString)).containsExactlyInAnyOrder(
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)",
                    "@" + SomeStereotype.class.getName(),
                    "@" + SomeTardyIndirectedStereotype.class.getName());
        }

        @Test
        void shouldGetAllFromDoubleIndirectClassStereotype() {
            Annotations annotations = Annotations.on(DoubleIndirectlyStereotypedClass.class);

            Stream<Annotation> all = annotations.all();

            then(all.map(Objects::toString)).containsExactlyInAnyOrder(
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)",
                    "@" + SomeStereotype.class.getName(),
                    "@" + SomeIndirectedStereotype.class.getName(),
                    "@" + SomeDoubleIndirectedStereotype.class.getName());
        }

        @Test
        void shouldGetClassAnnotationAmbiguousWithStereotype() {
            Annotations annotations = Annotations.on(StereotypedClassWithSomeAnnotation.class);

            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("on-class");
        }
    }

    @Nested
    class DoubleStereotypedClasses {
        Annotations annotations = Annotations.on(DoubleStereotypedClass.class);

        @Test
        void shouldGetFirstOfAmbiguousAnnotationFromTwoStereotypes() {
            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isIn( // both are allowed:
                    "some-stereotype",
                    "another-stereotype");
        }

        @Test
        void shouldGetAllNonRepeatableAnnotationsFromTwoStereotypes() {
            Stream<SomeAnnotation> someAnnotations = annotations.all(SomeAnnotation.class);

            then(someAnnotations.map(Objects::toString)).containsAnyOf( // both are allowed:
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")",
                    "@" + SomeAnnotation.class.getName() + "(value = \"another-stereotype\")");
        }

        @Test
        void shouldGetAllRepeatableAnnotationsFromTwoStereotypes() {
            Stream<RepeatableAnnotation> repeatableAnnotations = annotations.all(RepeatableAnnotation.class);

            then(repeatableAnnotations.map(Objects::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 6)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 3)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 4)");
        }

        @Test
        void shouldGetAllAnnotationsFromTwoStereotypes() {
            Stream<Annotation> all = annotations.all();

            List<String> list = all.map(Objects::toString).collect(toList());
            then(list).contains(
                    "@" + SomeStereotype.class.getName(),
                    "@" + AnotherStereotype.class.getName(),
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 3)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 4)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 6)");
            then(list).containsAnyOf( // both are allowed:
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")",
                    "@" + SomeAnnotation.class.getName() + "(value = \"another-stereotype\")");
            then(list).hasSize(8);
        }
    }

    @Nested
    class StereotypedFields {
        Annotations annotations = Annotations.onField(ClassWithStereotypedField.class, "foo");

        @Test
        void shouldGetAnnotationFromFieldStereotype() {
            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("some-stereotype");
        }

        @Test
        void shouldGetAllAnnotationsFromFieldStereotype() {
            Stream<Annotation> someAnnotation = annotations.all();

            then(someAnnotation.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 7)",
                    "@" + SomeStereotype.class.getName(),
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetAllAnnotationNonRepeatableTypedFromFieldStereotype() {
            Stream<SomeAnnotation> someAnnotation = annotations.all(SomeAnnotation.class);

            then(someAnnotation.map(Objects::toString)).containsExactlyInAnyOrder(
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")");
        }
    }

    @Nested
    class StereotypedMethods {
        Annotations annotations = Annotations.onMethod(ClassWithStereotypedMethod.class, "foo");

        @Test
        void shouldGetAnnotationFromMethodStereotype() {
            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("some-stereotype");
        }

        @Test
        void shouldGetAllAnnotationsFromMethodStereotype() {
            Stream<Annotation> someAnnotation = annotations.all();

            then(someAnnotation.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 7)",
                    "@" + SomeStereotype.class.getName(),
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetAllAnnotationNonRepeatableTypedFromMethodStereotype() {
            Stream<SomeAnnotation> someAnnotation = annotations.all(SomeAnnotation.class);

            then(someAnnotation.map(Objects::toString)).containsExactlyInAnyOrder(
                    "@" + SomeAnnotation.class.getName() + "(value = \"some-stereotype\")");
        }
    }
}
