package io.smallrye.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate to code checkers that the annotated usage of a forbidden API
 * should be allowed even if there is a policy forbidding it.
 * <p>
 * A valid reason must be given for this kind of exception; therefore,
 * the {@link #reason() reason} element is required.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE })
public @interface SuppressForbidden {
    /**
     * {@return the reason for allowing a forbidden API in the annotated element}
     */
    String reason();
}
