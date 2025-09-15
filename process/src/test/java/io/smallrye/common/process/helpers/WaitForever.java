package io.smallrye.common.process.helpers;

import java.util.concurrent.locks.LockSupport;

public final class WaitForever {
    public static void main(String[] args) {
        System.out.close();
        System.err.close();
        for (;;) {
            LockSupport.park();
        }
    }
}
