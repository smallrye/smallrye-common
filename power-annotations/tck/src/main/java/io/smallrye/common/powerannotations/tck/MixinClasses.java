package io.smallrye.common.powerannotations.tck;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

import io.smallrye.common.powerannotations.MixinFor;
import io.smallrye.common.powerannotations.tck.MixinClasses.TypeAnnotationMixinClasses.SomeAnnotationWithoutValue;

public class MixinClasses {
    @Retention(RUNTIME)
    public @interface AnotherAnnotation {
    }

    public static class TypeAnnotationMixinClasses {
        @Retention(RUNTIME)
        public @interface SomeAnnotationWithoutValue {
        }

        @SomeAnnotationWithoutValue
        @SomeAnnotation("to-be-replaced")
        @RepeatableAnnotation(2)
        public static class SomeClassWithVariousAnnotations {
        }

        @MixinFor(SomeClassWithVariousAnnotations.class)
        @AnotherAnnotation
        @SomeAnnotation("replacing")
        @RepeatableAnnotation(1)
        public static class MixinForSomeClassWithVariousAnnotations {
        }

        @SomeAnnotationTargetedByMixin
        @RepeatableAnnotation(1)
        public static class SomeClassWithAnnotationTargetedByMixin {
        }

        @Retention(RUNTIME)
        @RepeatableAnnotation(3)
        public @interface SomeAnnotationTargetedByMixin {
        }

        @MixinFor(SomeAnnotationTargetedByMixin.class)
        @SomeAnnotation("annotation-mixin")
        @RepeatableAnnotation(2)
        public static class MixinForAnnotation {
        }

        @SomeAnnotationTargetedByMixin
        @SomeAnnotation("original")
        public static class OriginalAnnotatedTarget {
        }

        public static class TargetClassWithTwoMixins {
        }

        @MixinFor(TargetClassWithTwoMixins.class)
        @SomeAnnotation("one")
        static class MixinForTargetClassWithTwoMixins1 {
        }

        @MixinFor(TargetClassWithTwoMixins.class)
        @RepeatableAnnotation(2)
        static class MixinForTargetClassWithTwoMixins2 {
        }

        public static class TargetClassWithTwoNonRepeatableMixins {
        }

        @MixinFor(TargetClassWithTwoNonRepeatableMixins.class)
        @SomeAnnotation("one")
        static class MixinForTargetClassWithTwoNonRepeatableMixins1 {
        }

        @MixinFor(TargetClassWithTwoNonRepeatableMixins.class)
        @SomeAnnotation("one")
        static class MixinForTargetClassWithTwoNonRepeatableMixins2 {
        }

        public static class TargetClassWithTwoRepeatableMixins {
        }

        @MixinFor(TargetClassWithTwoRepeatableMixins.class)
        @RepeatableAnnotation(1)
        static class MixinForTargetClassWithTwoRepeatableMixins1 {
        }

        @MixinFor(TargetClassWithTwoRepeatableMixins.class)
        @RepeatableAnnotation(2)
        static class MixinForTargetClassWithTwoRepeatableMixins2 {
        }
    }

    public static class FieldAnnotationMixinClasses {
        public static class SomeClassWithFieldWithVariousAnnotations {
            @SuppressWarnings("unused")
            @SomeAnnotationWithoutValue
            @SomeAnnotation("to-be-replaced")
            @RepeatableAnnotation(2)
            String foo;

            @SuppressWarnings("unused")
            String bar;
        }

        @MixinFor(SomeClassWithFieldWithVariousAnnotations.class)
        public static class MixinForSomeClassWithFieldWithVariousAnnotations {
            @SuppressWarnings("unused")
            @AnotherAnnotation
            @SomeAnnotation("replacing")
            @RepeatableAnnotation(1)
            String foo;
        }

        public static class TargetFieldClassWithTwoMixins {
            @SuppressWarnings("unused")
            String foo;
        }

        @MixinFor(TargetFieldClassWithTwoMixins.class)
        static class MixinForTargetFieldClassWithTwoMixins1 {
            @SomeAnnotation("one")
            @SuppressWarnings("unused")
            String foo;
        }

        @MixinFor(TargetFieldClassWithTwoMixins.class)
        static class MixinForTargetFieldClassWithTwoMixins2 {
            @RepeatableAnnotation(2)
            @SuppressWarnings("unused")
            String foo;
        }

        public static class TargetFieldClassWithTwoNonRepeatableMixins {
            @SuppressWarnings("unused")
            String foo;
        }

        @MixinFor(TargetFieldClassWithTwoNonRepeatableMixins.class)
        static class MixinForTargetFieldClassWithTwoNonRepeatableMixins1 {
            @SomeAnnotation("one")
            @SuppressWarnings("unused")
            String foo;
        }

        @MixinFor(TargetFieldClassWithTwoNonRepeatableMixins.class)
        static class MixinForTargetFieldClassWithTwoNonRepeatableMixins2 {
            @SomeAnnotation("one")
            @SuppressWarnings("unused")
            String foo;
        }

        public static class TargetFieldClassWithTwoRepeatableMixins {
            @SuppressWarnings("unused")
            String foo;
        }

        @MixinFor(TargetFieldClassWithTwoRepeatableMixins.class)
        static class MixinForTargetFieldClassWithTwoRepeatableMixins1 {
            @RepeatableAnnotation(1)
            @SuppressWarnings("unused")
            String foo;
        }

        @MixinFor(TargetFieldClassWithTwoRepeatableMixins.class)
        static class MixinForTargetFieldClassWithTwoRepeatableMixins2 {
            @RepeatableAnnotation(2)
            @SuppressWarnings("unused")
            String foo;
        }
    }

    public static class MethodAnnotationMixinClasses {
        public static class SomeClassWithMethodWithVariousAnnotations {
            @SuppressWarnings("unused")
            @SomeAnnotationWithoutValue
            @SomeAnnotation("to-be-replaced")
            @RepeatableAnnotation(2)
            String foo() {
                return "foo";
            }

            @SuppressWarnings("unused")
            String bar() {
                return "bar";
            }
        }

        @MixinFor(SomeClassWithMethodWithVariousAnnotations.class)
        public static class MixinForSomeClassWithMethodWithVariousAnnotations {
            @SuppressWarnings("unused")
            @AnotherAnnotation
            @SomeAnnotation("replacing")
            @RepeatableAnnotation(1)
            String foo() {
                return "foo";
            }
        }

        public static class TargetMethodClassWithTwoMixins {
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }

        @MixinFor(TargetMethodClassWithTwoMixins.class)
        static class MixinForTargetMethodClassWithTwoMixins1 {
            @SomeAnnotation("one")
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }

        @MixinFor(TargetMethodClassWithTwoMixins.class)
        static class MixinForTargetMethodClassWithTwoMixins2 {
            @RepeatableAnnotation(2)
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }

        public static class TargetMethodClassWithTwoNonRepeatableMixins {
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }

        @MixinFor(TargetMethodClassWithTwoNonRepeatableMixins.class)
        static class MixinForTargetMethodClassWithTwoNonRepeatableMixins1 {
            @SomeAnnotation("one")
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }

        @MixinFor(TargetMethodClassWithTwoNonRepeatableMixins.class)
        static class MixinTargetMethodClassWithTwoNonRepeatableMixins2 {
            @SomeAnnotation("one")
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }

        public static class TargetMethodClassWithTwoRepeatableMixins {
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }

        @MixinFor(TargetMethodClassWithTwoRepeatableMixins.class)
        static class MixinForTargetMethodClassWithTwoRepeatableMixins1 {
            @RepeatableAnnotation(1)
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }

        @MixinFor(TargetMethodClassWithTwoRepeatableMixins.class)
        static class MixinForTargetMethodClassWithTwoRepeatableMixins2 {
            @RepeatableAnnotation(2)
            @SuppressWarnings("unused")
            String foo() {
                return "foo";
            }
        }
    }
}
