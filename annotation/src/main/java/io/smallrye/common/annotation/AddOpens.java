package io.smallrye.common.annotation;

import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declare that a module's packages should be opened to the annotated module.
 * <p>
 * This annotation is only respected by certain cooperating tools and runtimes.
 */
@Retention(CLASS)
@Target(MODULE)
@Repeatable(AddOpens.List.class)
@Documented
public @interface AddOpens {
    /**
     * {@return the module name to open from}
     */
    String module();

    /**
     * {@return the packages which should be opened}
     */
    String[] packages();

    /**
     * The repeating holder annotation for {@link AddOpens}.
     */
    @Retention(CLASS)
    @Target(MODULE)
    @Documented
    @interface List {
        /**
         * {@return the annotations}
         */
        AddOpens[] value();
    }
}
