package io.smallrye.common.classloader;

import java.security.Permission;

/**
 * A security manager permission which indicates the ability to define a class.
 */
public class DefineClassPermission extends Permission {
    private static final long serialVersionUID = 142067672163413424L;
    private static final DefineClassPermission INSTANCE = new DefineClassPermission();

    /**
     * Construct a new instance.
     */
    public DefineClassPermission() {
        super("");
    }

    /**
     * Construct a new instance.
     *
     * @param name ignored
     * @param actions ignored
     */
    public DefineClassPermission(final String name, final String actions) {
        this();
    }

    /**
     * {@return the singular instance}
     */
    public static DefineClassPermission getInstance() {
        return INSTANCE;
    }

    /**
     * {@return {@code true} if this permission implies the given permission}
     */
    @Override
    public boolean implies(final Permission permission) {
        return permission != null && permission.getClass() == this.getClass();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof DefineClassPermission;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String getActions() {
        return "";
    }
}
