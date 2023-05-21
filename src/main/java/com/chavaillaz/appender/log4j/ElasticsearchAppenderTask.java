package com.chavaillaz.appender.log4j;

import java.util.concurrent.Callable;

import com.chavaillaz.appender.log4j.converter.EventConverter;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Simple callable that inserts the logging event into Elasticsearch.
 */
public class ElasticsearchAppenderTask implements Callable<LogEvent> {

    private final ElasticsearchAppender appender;
    private final LogEvent loggingEvent;

    /**
     * Creates a new task to send an event to Elasticsearch.
     *
     * @param appender     The appender from which the task is launched
     * @param loggingEvent The logging event to send
     */
    public ElasticsearchAppenderTask(ElasticsearchAppender appender, LogEvent loggingEvent) {
        this.appender = appender;
        this.loggingEvent = loggingEvent;
    }

    /**
     * Called by the executor service and inserts the document into Elasticsearch.
     *
     * @return The logging event
     */
    @Override
    public LogEvent call() {
        if (appender.getClient() != null) {
            EventConverter converter = appender.getClient().getConfiguration().getEventConverter();
            appender.getClient().send(converter.convert(appender, loggingEvent));
        }
        return loggingEvent;
    }

}

