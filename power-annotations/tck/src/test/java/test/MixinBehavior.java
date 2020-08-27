package test;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.common.powerannotations.AmbiguousAnnotationResolutionException;
import io.smallrye.common.powerannotations.Annotations;
import io.smallrye.common.powerannotations.tck.MixinClasses.AnotherAnnotation;
import io.smallrye.common.powerannotations.tck.MixinClasses.FieldAnnotationMixinClasses.SomeClassWithFieldWithVariousAnnotations;
import io.smallrye.common.powerannotations.tck.MixinClasses.FieldAnnotationMixinClasses.TargetFieldClassWithTwoMixins;
import io.smallrye.common.powerannotations.tck.MixinClasses.FieldAnnotationMixinClasses.TargetFieldClassWithTwoNonRepeatableMixins;
import io.smallrye.common.powerannotations.tck.MixinClasses.FieldAnnotationMixinClasses.TargetFieldClassWithTwoRepeatableMixins;
import io.smallrye.common.powerannotations.tck.MixinClasses.MethodAnnotationMixinClasses.SomeClassWithMethodWithVariousAnnotations;
import io.smallrye.common.powerannotations.tck.MixinClasses.MethodAnnotationMixinClasses.TargetMethodClassWithTwoMixins;
import io.smallrye.common.powerannotations.tck.MixinClasses.MethodAnnotationMixinClasses.TargetMethodClassWithTwoNonRepeatableMixins;
import io.smallrye.common.powerannotations.tck.MixinClasses.MethodAnnotationMixinClasses.TargetMethodClassWithTwoRepeatableMixins;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.OriginalAnnotatedTarget;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.SomeAnnotationTargetedByMixin;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.SomeAnnotationWithoutValue;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.SomeClassWithAnnotationTargetedByMixin;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.SomeClassWithVariousAnnotations;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.TargetClassWithTwoMixins;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.TargetClassWithTwoNonRepeatableMixins;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.TargetClassWithTwoRepeatableMixins;
import io.smallrye.common.powerannotations.tck.RepeatableAnnotation;
import io.smallrye.common.powerannotations.tck.SomeAnnotation;

public class MixinBehavior {

    @Nested
    class ClassAnnotations {
        Annotations annotations = Annotations.on(SomeClassWithVariousAnnotations.class);

        @Test
        void shouldGetTargetClassAnnotation() {
            Optional<SomeAnnotationWithoutValue> annotation = annotations.get(SomeAnnotationWithoutValue.class);

            then(annotation).isPresent();
        }

        @Test
        void shouldGetMixinClassAnnotation() {
            Optional<AnotherAnnotation> annotation = annotations.get(AnotherAnnotation.class);

            then(annotation).isPresent();
        }

        @Test
        void shouldGetReplacedClassAnnotation() {
            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("replacing");
        }

        @Test
        void shouldFailToGetRepeatedClassAnnotation() {
            Throwable throwable = catchThrowable(() -> annotations.get(RepeatableAnnotation.class));

            then(throwable).isInstanceOf(AmbiguousAnnotationResolutionException.class);
        }

        @Test
        void shouldGetAllClassAnnotations() {
            Stream<Annotation> list = annotations.all();

            then(list.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + SomeAnnotation.class.getName() + "(value = \"replacing\")",
                    "@" + AnotherAnnotation.class.getName(),
                    "@" + SomeAnnotationWithoutValue.class.getName(),
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetAllRepeatableClassAnnotations() {
            Stream<RepeatableAnnotation> list = annotations.all(RepeatableAnnotation.class);

            then(list.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        Annotations annotationsFromAnnotationTargetedByMixin = Annotations.on(SomeClassWithAnnotationTargetedByMixin.class);

        @Test
        void shouldGetMixedInAnnotation() {
            Optional<SomeAnnotation> someAnnotation = annotationsFromAnnotationTargetedByMixin.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("annotation-mixin");
        }

        @Test
        void shouldGetAllNonRepeatableMixedInAnnotations() {
            Stream<SomeAnnotation> someAnnotation = annotationsFromAnnotationTargetedByMixin.all(SomeAnnotation.class);

            then(someAnnotation.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + SomeAnnotation.class.getName() + "(value = \"annotation-mixin\")");
        }

        @Test
        void shouldGetAllRepeatableMixedInAnnotations() {
            Stream<RepeatableAnnotation> repeatableAnnotations = annotationsFromAnnotationTargetedByMixin
                    .all(RepeatableAnnotation.class);

            then(repeatableAnnotations.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetAllMixedInAnnotation() {
            Stream<Annotation> all = annotationsFromAnnotationTargetedByMixin.all();

            then(all.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + SomeAnnotationTargetedByMixin.class.getName(),
                    "@" + SomeAnnotation.class.getName() + "(value = \"annotation-mixin\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldOverwriteAnnotationWithAnnotationMixedIn() {
            Annotations annotations = Annotations.on(OriginalAnnotatedTarget.class);

            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("annotation-mixin");
        }

        @Test
        void shouldGetClassAnnotationFromMultipleMixins() {
            Annotations annotations = Annotations.on(TargetClassWithTwoMixins.class);

            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("one");
        }

        @Test
        void shouldGetOneOfDuplicateNonRepeatableClassAnnotationFromMultipleMixins() {
            Annotations annotations = Annotations.on(TargetClassWithTwoNonRepeatableMixins.class);

            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("one");
        }

        @Test
        void shouldFailToGetDuplicateRepeatableClassAnnotationFromMultipleMixins() {
            Annotations annotations = Annotations.on(TargetClassWithTwoRepeatableMixins.class);

            Throwable throwable = catchThrowable(() -> annotations.get(RepeatableAnnotation.class));

            then(throwable).isInstanceOf(AmbiguousAnnotationResolutionException.class);
        }
    }

    @Nested
    class FieldAnnotations {
        Annotations annotations = Annotations.onField(SomeClassWithFieldWithVariousAnnotations.class, "foo");

        @Test
        void shouldSkipUndefinedMixinFieldAnnotation() {
            Annotations annotations = Annotations.onField(SomeClassWithFieldWithVariousAnnotations.class, "bar");

            Optional<SomeAnnotationWithoutValue> someAnnotationWithoutValue = annotations.get(SomeAnnotationWithoutValue.class);

            then(someAnnotationWithoutValue).isNotPresent();
        }

        @Test
        void shouldGetTargetFieldAnnotation() {
            Optional<SomeAnnotationWithoutValue> someAnnotationWithoutValue = annotations.get(SomeAnnotationWithoutValue.class);

            then(someAnnotationWithoutValue).isPresent();
        }

        @Test
        void shouldGetMixinFieldAnnotation() {
            Optional<AnotherAnnotation> anotherAnnotation = annotations.get(AnotherAnnotation.class);

            then(anotherAnnotation).isPresent();
        }

        @Test
        void shouldGetReplacedFieldAnnotation() {
            Optional<SomeAnnotation> annotation = annotations.get(SomeAnnotation.class);

            assert annotation.isPresent();
            then(annotation.get().value()).isEqualTo("replacing");
        }

        @Test
        void shouldFailToGetRepeatableFieldAnnotation() {
            Throwable throwable = catchThrowable(() -> annotations.get(RepeatableAnnotation.class));

            then(throwable).isInstanceOf(AmbiguousAnnotationResolutionException.class);
        }

        @Test
        void shouldGetAllFieldAnnotations() {
            Stream<Annotation> list = annotations.all();

            then(list.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + AnotherAnnotation.class.getName(),
                    "@" + SomeAnnotationWithoutValue.class.getName(),
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + SomeAnnotation.class.getName() + "(value = \"replacing\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetAllRepeatableFieldAnnotations() {
            Stream<RepeatableAnnotation> list = annotations.all(RepeatableAnnotation.class);

            then(list.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetOneOfDuplicateFieldAnnotationsFromMultipleMixins() {
            Annotations annotations = Annotations.onField(TargetFieldClassWithTwoMixins.class, "foo");

            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("one");
        }

        @Test
        void shouldGetOneRepeatableFieldAnnotationFromMultipleMixins() {
            Annotations annotations = Annotations.onField(TargetFieldClassWithTwoNonRepeatableMixins.class, "foo");

            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("one");
        }

        @Test
        void shouldFailToGetDuplicateRepeatableFieldAnnotationFromMultipleMixins() {
            Annotations annotations = Annotations.onField(TargetFieldClassWithTwoRepeatableMixins.class, "foo");

            Throwable throwable = catchThrowable(() -> annotations.get(RepeatableAnnotation.class));

            then(throwable).isInstanceOf(AmbiguousAnnotationResolutionException.class);
        }

        // TODO test unknown field mixin
    }

    @Nested
    class MethodAnnotations {
        Annotations annotations = Annotations.onMethod(SomeClassWithMethodWithVariousAnnotations.class, "foo");

        @Test
        void shouldSkipUndefinedMixinMethodAnnotation() {
            Annotations annotations = Annotations.onMethod(SomeClassWithMethodWithVariousAnnotations.class, "bar");

            Optional<SomeAnnotationWithoutValue> someAnnotationWithoutValue = annotations.get(SomeAnnotationWithoutValue.class);

            then(someAnnotationWithoutValue).isNotPresent();
        }

        @Test
        void shouldGetTargetMethodAnnotation() {
            Optional<SomeAnnotationWithoutValue> someAnnotationWithoutValue = annotations.get(SomeAnnotationWithoutValue.class);

            then(someAnnotationWithoutValue).isPresent();
        }

        @Test
        void shouldGetMixinMethodAnnotation() {
            Optional<AnotherAnnotation> anotherAnnotation = annotations.get(AnotherAnnotation.class);

            then(anotherAnnotation).isPresent();
        }

        @Test
        void shouldGetReplacedMethodAnnotation() {
            Optional<SomeAnnotation> annotation = annotations.get(SomeAnnotation.class);

            assert annotation.isPresent();
            then(annotation.get().value()).isEqualTo("replacing");
        }

        @Test
        void shouldFailToGetRepeatableMethodAnnotation() {
            Throwable throwable = catchThrowable(() -> annotations.get(RepeatableAnnotation.class));

            then(throwable).isInstanceOf(AmbiguousAnnotationResolutionException.class);
        }

        @Test
        void shouldGetAllMethodAnnotations() {
            Stream<Annotation> list = annotations.all();

            then(list.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + AnotherAnnotation.class.getName(),
                    "@" + SomeAnnotationWithoutValue.class.getName(),
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + SomeAnnotation.class.getName() + "(value = \"replacing\")",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetAllRepeatableMethodAnnotations() {
            Stream<RepeatableAnnotation> list = annotations.all(RepeatableAnnotation.class);

            then(list.map(Object::toString)).containsExactlyInAnyOrder(
                    "@" + RepeatableAnnotation.class.getName() + "(value = 1)",
                    "@" + RepeatableAnnotation.class.getName() + "(value = 2)");
        }

        @Test
        void shouldGetMethodAnnotationFromMultipleMixins() {
            Annotations annotations = Annotations.onMethod(TargetMethodClassWithTwoMixins.class, "foo");

            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("one");
        }

        @Test
        void shouldGetOneOfDuplicateNonRepeatableMethodAnnotationFromMultipleMixins() {
            Annotations annotations = Annotations.onMethod(TargetMethodClassWithTwoNonRepeatableMixins.class, "foo");

            Optional<SomeAnnotation> someAnnotation = annotations.get(SomeAnnotation.class);

            assert someAnnotation.isPresent();
            then(someAnnotation.get().value()).isEqualTo("one");
        }

        @Test
        void shouldFailToGetDuplicateRepeatableMethodAnnotationFromMultipleMixins() {
            Annotations annotations = Annotations.onMethod(TargetMethodClassWithTwoRepeatableMixins.class, "foo");

            Throwable throwable = catchThrowable(() -> annotations.get(RepeatableAnnotation.class));

            then(throwable).isInstanceOf(AmbiguousAnnotationResolutionException.class);
        }

        // TODO constructor mixins
        // TODO test unknown method mixin (name or args)
    }
}
