package test;

import static org.assertj.core.api.BDDAssertions.then;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.smallrye.common.powerannotations.Annotations;
import io.smallrye.common.powerannotations.tck.CombinedAnnotationClasses.SomeInheritingInterface;
import io.smallrye.common.powerannotations.tck.CombinedAnnotationClasses.SomeStereotypedClass;
import io.smallrye.common.powerannotations.tck.CombinedAnnotationClasses.SomeStereotypedInterface;
import io.smallrye.common.powerannotations.tck.SomeAnnotation;

public class CombinedBehavior {
    @Test
    void shouldResolveInterfaceStereotypesBeforeTypeToMember() {
        Annotations fooAnnotations = Annotations.onMethod(SomeStereotypedInterface.class, "foo");

        Stream<Annotation> all = fooAnnotations.all();

        then(all.map(Object::toString)).containsExactlyInAnyOrder(
                "@" + SomeAnnotation.class.getName() + "(value = \"from-stereotype\")");
    }

    @Test
    void shouldResolveClassStereotypesBeforeTypeToMember() {
        Annotations fooAnnotations = Annotations.onMethod(SomeStereotypedClass.class, "foo");

        Stream<Annotation> all = fooAnnotations.all();

        then(all.map(Object::toString)).containsExactlyInAnyOrder(
                "@" + SomeAnnotation.class.getName() + "(value = \"from-stereotype\")");
    }

    @Test
    void shouldResolveInterfaceInheritedBeforeTypeToMember() {
        Annotations fooAnnotations = Annotations.onMethod(SomeInheritingInterface.class, "foo");

        Stream<Annotation> all = fooAnnotations.all();

        then(all.map(Object::toString)).containsExactlyInAnyOrder(
                "@" + SomeAnnotation.class.getName() + "(value = \"from-sub-interface\")");
    }
}
