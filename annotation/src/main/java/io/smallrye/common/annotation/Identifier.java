package io.smallrye.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * String-based {@linkplain Qualifier qualifier}.
 * Unlike {@link javax.inject.Named @Named}, this is a proper qualifier in CDI environment.
 * <p>
 * Identifier must always be provided.
 * Unlike {@link javax.inject.Named @Named}, it is not deduced from the annotation use.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE })
public @interface Identifier {
    /**
     * The identifier.
     *
     * @return the identifier.
     */
    String value();

    /**
     * Supports inline instantiation of the {@link Identifier} qualifier.
     */
    final class Literal extends AnnotationLiteral<Identifier> implements Identifier {
        private static final long serialVersionUID = 1L;

        private final String value;

        public static Literal of(String value) {
            return new Literal(value);
        }

        private Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
