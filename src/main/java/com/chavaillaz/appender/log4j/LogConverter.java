package com.chavaillaz.appender.log4j;

import java.util.Map;

import com.chavaillaz.appender.LogConfiguration;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Conversion of events to records for logs transmission.
 */
public interface LogConverter {

    /**
     * Converts a logging event into multiple properties (key/value) to log.
     *
     * @param event The logging event
     * @return The {@link Map} containing the properties to send
     */
    Map<String, Object> convert(LogEvent event);

    /**
     * Configures the converter with the given settings.
     *
     * @param configuration The configuration
     */
    void configure(LogConfiguration configuration);

}
