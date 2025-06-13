package io.smallrye.common.process.helpers;

import java.io.IOException;

/**
 * A silly version of the {@code cat} utility (which only supports stdin/stdout).
 */
public final class Cat {
    public static void main(String[] args) throws IOException {
        System.in.transferTo(System.out);
        System.exit(0);
    }
}
