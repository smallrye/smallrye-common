package io.smallrye.common.annotation;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that the annotated method is inherently non-blocking and so can be
 * executed on a <em>non-blockable</em> thread (I/O threads, event loops...) without the need to offload the work to
 * another thread. If the caller thread can be blocked, it should also be safe to execute the method on that thread.
 * <p>
 * It's up to the framework relying on this annotation do define the exact behavior, like <em>what</em> thread is
 * considered as a <em>non-blockable</em> thread.
 * <p>
 * This annotation is not <em>inheritable</em>, so the user must repeat the annotation when overriding the method.
 *
 * @see Blocking
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(METHOD)
public @interface NonBlocking {

    // Just a marker annotation.
}
