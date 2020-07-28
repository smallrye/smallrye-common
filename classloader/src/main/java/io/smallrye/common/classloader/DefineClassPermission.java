package io.smallrye.common.classloader;

import java.security.Permission;

public class DefineClassPermission extends Permission {
    private static final long serialVersionUID = 142067672163413424L;
    private static final DefineClassPermission INSTANCE = new DefineClassPermission();

    public DefineClassPermission() {
        super("");
    }

    public DefineClassPermission(final String name, final String actions) {
        this();
    }

    public static DefineClassPermission getInstance() {
        return INSTANCE;
    }

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
