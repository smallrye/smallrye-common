package io.smallrye.common.annotation;

import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declare that the annotated module requests native access.
 */
@Retention(CLASS)
@Target(MODULE)
@Documented
public @interface NativeAccess {
}
