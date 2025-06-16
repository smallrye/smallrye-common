package io.smallrye.common.process;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A log configurator.
 */
public final class LoggingConfigurator {
    public LoggingConfigurator() {
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new LogFormatter());
        logger.addHandler(handler);
    }
}
