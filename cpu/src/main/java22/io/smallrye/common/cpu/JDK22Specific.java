package io.smallrye.common.cpu;

import java.lang.foreign.ValueLayout;

final class JDK22Specific {
    static final int ADDRESS_SIZE = (int) ValueLayout.ADDRESS.byteSize();
}
