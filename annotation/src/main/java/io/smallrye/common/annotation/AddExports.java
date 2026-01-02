package io.smallrye.common.annotation;

import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declare that a module's packages should be exported to the annotated module.
 * <p>
 * This annotation is only respected by certain cooperating tools and runtimes.
 */
@Retention(CLASS)
@Target(MODULE)
@Repeatable(AddExports.List.class)
@Documented
public @interface AddExports {
    /**
     * {@return the module name to export from}
     */
    String module();

    /**
     * {@return the packages which should be exported}
     */
    String[] packages();

    /**
     * The repeating holder annotation for {@link AddExports}.
     */
    @Retention(CLASS)
    @Target(MODULE)
    @Documented
    @interface List {
        /**
         * {@return the annotations}
         */
        AddExports[] value();
    }
}
