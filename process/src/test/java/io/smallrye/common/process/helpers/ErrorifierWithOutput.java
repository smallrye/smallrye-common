package io.smallrye.common.process.helpers;

/**
 * A utility that exits with an exit code after printing a message to stderr and stdout.
 */
public final class ErrorifierWithOutput {
    public static void main(String[] args) {
        System.out.println("Some output text");
        System.err.println("Some error text");
        System.exit(Integer.parseInt(args[0]));
    }
}
