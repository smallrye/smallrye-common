package io.smallrye.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that specifies that an element is experimental and may change without notice.
 *
 * @see TechPreview
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.PACKAGE })
public @interface Experimental {
    /**
     * Describes why the annotated element is experimental.
     *
     * @return the experimental description.
     */
    String value();
}
