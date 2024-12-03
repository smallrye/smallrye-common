package io.smallrye.common.cpu;

import sun.misc.Unsafe;

final class JDK22Specific {
    static final int ADDRESS_SIZE = Unsafe.ADDRESS_SIZE;
}
