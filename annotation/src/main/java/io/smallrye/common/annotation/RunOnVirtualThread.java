package io.smallrye.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If supported, this annotation indicates that the (blocking) method should be invoked on a virtual thread instead of
 * a regular (OS) worker thread.
 * This annotation should only be used on blocking methods, either marked explicitly blocking (using
 * {@link io.smallrye.common.annotation.Blocking}) or considered blocking by the underlying framework.
 *
 * @see Blocking,
 * @see NonBlocking
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Experimental("This is an experimental feature still at the alpha stage")
public @interface RunOnVirtualThread {

    // Just a marker annotation.
}
