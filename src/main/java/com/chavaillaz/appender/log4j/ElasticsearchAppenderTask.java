package com.chavaillaz.appender.log4j;

import com.chavaillaz.appender.log4j.converter.EventConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.spi.LoggingEvent;

import java.util.concurrent.Callable;

/**
 * Simple callable that inserts the logging event into Elasticsearch.
 */
@Slf4j
public class ElasticsearchAppenderTask implements Callable<LoggingEvent> {

    private final ElasticsearchAppender appender;
    private final LoggingEvent loggingEvent;

    /**
     * Creates a new task to send an event to Elasticsearch.
     *
     * @param appender     The appender from which the task is launched
     * @param loggingEvent The logging event to send
     */
    public ElasticsearchAppenderTask(ElasticsearchAppender appender, LoggingEvent loggingEvent) {
        this.appender = appender;
        this.loggingEvent = loggingEvent;
    }

    /**
     * Called by the executor service and inserts the document into Elasticsearch.
     *
     * @return The logging event
     */
    @Override
    public LoggingEvent call() {
        if (appender.getClient() != null) {
            EventConverter converter = appender.getClient().getConfiguration().getEventConverter();
            appender.getClient().send(converter.convert(appender, loggingEvent));
        }
        return loggingEvent;
    }

}

