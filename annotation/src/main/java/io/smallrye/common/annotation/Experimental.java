package io.smallrye.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that specifies that an element is experimental and may change without notice.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Experimental {

    /**
     * The reason why the annotated element is experimental.
     *
     * @return the reason for being experimental.
     */
    Reason reason();

    /**
     * Further description why the annotated element is experimental.
     *
     * @return the experimental description.
     */
    String explanation() default "";

    enum Reason {
        NOT_COVERED_BY_SPECIFICATION,
        DIFFERENCE_FROM_SPECIFICATION
    }
}
