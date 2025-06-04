package io.smallrye.common.process.helpers;

/**
 * A utility that exits with an exit code after printing a message to stderr.
 */
public final class Errorifier {
    public static void main(String[] args) {
        System.err.println("Some error text");
        System.exit(Integer.parseInt(args[0]));
    }
}
