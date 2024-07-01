package io.smallrye.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that the annotated method is inherently blocking and so should not be
 * executed on a <em>non-blockable</em> thread (I/O thread, event loops...).
 * <p>
 * Frameworks can add support for this annotation and offload the work to another thread if the current thread cannot
 * be blocked. It's particularly useful for frameworks using a reactive execution model. Framework relying on this
 * annotation must specify the exact behavior:
 * <ul>
 * <li><em>what</em> thread is considered <em>non-blockable</em>;</li>
 * <li>on which thread is the execution offloaded;</li>
 * <li>whether, when the current thread can block, the execution of the annotated method is still offloaded to
 * another thread, or stays on the same thread;</li>
 * <li>if the execution of the method is offloaded, whether the initial thread is restored after the method
 * execution.</li>
 * </ul>
 * <p>
 * When this annotation is used on a {@code class}, all the methods declared by the annotated class are considered
 * <em>blocking</em>.
 * <p>
 * This annotation is not <em>inheritable</em>, so the user must repeat the annotation when sub-classing the class
 * or overriding the method.
 *
 * @see NonBlocking
 * @see RunOnVirtualThread
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Blocking {

    // Just a marker annotation.
}
