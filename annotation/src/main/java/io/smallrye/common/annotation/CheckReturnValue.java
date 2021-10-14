package io.smallrye.common.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
/**
 * Marker annotation for methods whose return values shall not be ignored under common API usage patterns.
 * <p>
 * Static analysis tools can leverage this annotation under JSR 305 semantics.
 *
 * IntelliJ IDEA is known to consider {@code CheckReturnValue} annotations, no matter what the package.
 */
public @interface CheckReturnValue {
}
