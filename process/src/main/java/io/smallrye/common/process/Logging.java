package io.smallrye.common.process;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRCOM", length = 5)
interface Logging extends BasicLogger {
    Logging log = Logger.getMessageLogger(MethodHandles.lookup(), Logging.class, Logging.class.getPackageName());

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 5000, value = "Command %s (pid %d) completed successfully but logged on stderr:%s")
    void logErrors(Path command, long pid, StringBuilder errors);
}
