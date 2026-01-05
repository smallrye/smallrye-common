package io.smallrye.common.io;

import static java.lang.invoke.MethodHandles.lookup;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRCOM", length = 5)
interface Messages extends BasicLogger {
    Messages log = org.jboss.logging.Logger.getMessageLogger(lookup(), Messages.class, "io.smallrye.common.io");

    @Message(id = 4000, value = "Secure directory streams not supported by %s for path \"%s\"")
    UnsupportedOperationException secureDirectoryNotSupported(FileSystem fileSystem, Path path);

    @Message(id = 4001, value = "An unrecognized option was given: %s")
    IllegalArgumentException unknownOption(Object option);

    @Message(id = 4002, value = "Unexpected partial copy from %s to %s (expected to copy %d bytes, but only copied %d bytes)")
    IOException partialCopy(Object in, Object out, long expected, long actual);

    @Message(id = 4003, value = "Symbolic link creation in a secure directory is not supported by this JDK")
    UnsupportedOperationException secureSymlinkNotSupported();

    @Message(id = 4004, value = "Directory creation in a secure directory is not supported by this JDK")
    UnsupportedOperationException secureMkdirNotSupported();

    @Message(id = 4005, value = "File \"%s\" already exists, and REPLACE_EXISTING was not given")
    FileAlreadyExistsException copyFileExists(Path destFile);

    @Message(id = 4006, value = "Symbolic link reading in a secure directory is not supported by this JDK")
    UnsupportedOperationException secureReadlinkNotSupported();

    @Message(id = 4007, value = "Option %s is not supported for this operation")
    IllegalArgumentException unsupportedForOperation(Object option);
}
