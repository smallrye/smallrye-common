package io.smallrye.common.process;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A log formatter to make things readable during testing.
 */
public final class LogFormatter extends Formatter {
    public String format(final LogRecord record) {
        String message = record.getMessage();
        Object[] parameters = record.getParameters();
        if (parameters != null && parameters.length > 0) {
            message = MessageFormat.format(message, parameters);
        }
        return String.format("%s %s (%s) %s%n",
                record.getInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                record.getLevel(),
                Thread.currentThread().getName(), // this doesn't work in the real world
                message);
    }
}
