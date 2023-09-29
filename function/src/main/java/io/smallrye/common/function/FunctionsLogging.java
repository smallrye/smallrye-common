package io.smallrye.common.function;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRCFG", length = 5)
public interface FunctionsLogging {
    FunctionsLogging log = Logger.getMessageLogger(FunctionsLogging.class, FunctionsLogging.class.getPackage().getName());

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 4000, value = "An exception was logged by the exception consumer")
    void exception(@Cause Throwable cause);
}
