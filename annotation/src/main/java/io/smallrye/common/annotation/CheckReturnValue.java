package io.smallrye.common.annotation;

import java.lang.annotation.*;

/**
 * Marker annotation for methods whose return values shall not be ignored under common API usage patterns.
 * <p>
 * Static analysis tools can leverage this annotation under JSR 305 semantics.
 * <p>
 * IntelliJ IDEA is known to consider {@code CheckReturnValue} annotations, no matter what the package.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface CheckReturnValue {
}
