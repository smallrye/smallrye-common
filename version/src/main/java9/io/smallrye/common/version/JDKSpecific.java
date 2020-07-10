package io.smallrye.common.version;

import java.lang.module.ModuleDescriptor;

final class JDKSpecific {
    private JDKSpecific() {}

    static boolean hasJpms() {
        return true;
    }

    static int compareJpms(String v1, String v2) {
        return Integer.signum(ModuleDescriptor.Version.parse(v1).compareTo(ModuleDescriptor.Version.parse(v2)));
    }
}
