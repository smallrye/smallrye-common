package io.smallrye.common.annotation;

import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declare that the annotated module requests native access.
 * <p>
 * This annotation is only respected by certain cooperating tools and runtimes.
 */
@Retention(CLASS)
@Target(MODULE)
@Documented
public @interface NativeAccess {
}
