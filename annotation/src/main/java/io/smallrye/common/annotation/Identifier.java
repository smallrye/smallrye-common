package io.smallrye.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * String-based {@linkplain Qualifier qualifier}.
 * Unlike {@link jakarta.inject.Named @Named}, this is a proper qualifier in CDI environment.
 * <p>
 * Identifier must always be provided.
 * Unlike {@link jakarta.inject.Named @Named}, it is not deduced from the annotation use.
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

        /**
         * The identifier value.
         */
        private final String value;

        /**
         * Construct a new instance.
         *
         * @param value the identifier value (must not be {@code null})
         * @return the annotation literal
         */
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
