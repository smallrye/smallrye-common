package io.smallrye.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that specifies that an element is tech preview and may change in the future.
 * <p>
 * Annotated elements are feature-complete, but have known limitations, need bake-time or
 * have rough angles. The API is more stable than with {@link Experimental}.
 * <p>
 * Tech preview API can still be changed, but changes will be communicated.
 *
 * @see Experimental
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.PACKAGE })
public @interface TechPreview {
    /**
     * Describes why the annotated element is in tech preview.
     *
     * @return the tech preview description.
     */
    String value();
}
