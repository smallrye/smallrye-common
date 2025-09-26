package io.smallrye.common.io;

import static java.lang.invoke.MethodHandles.lookup;

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
}
