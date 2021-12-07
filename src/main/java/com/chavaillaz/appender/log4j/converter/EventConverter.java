package com.chavaillaz.appender.log4j.converter;

import com.chavaillaz.appender.log4j.ElasticsearchAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Map;

/**
 * Interface to implement in order to customize the creation of key/value document from the event that will be stored.
 */
public interface EventConverter {

    /**
     * Gets the field used as date of the event.
     *
     * @return The date field name
     */
    String getDateField();

    /**
     * Converts a logging event into multiple properties (key/value) to log in Elasticsearch.
     *
     * @param appender The logging appender
     * @param event    The logging event
     * @return The {@link Map} containing the properties to send
     */
    Map<String, Object> convert(ElasticsearchAppender appender, LoggingEvent event);

}
