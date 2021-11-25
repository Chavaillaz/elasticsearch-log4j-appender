package com.chavaillaz.appender.log4j;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Simple callable that inserts the document into Elasticsearch.
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
            Map<String, Object> data = new HashMap<>();
            writeBasic(data, loggingEvent);
            writeThrowable(data, loggingEvent);
            writeMDC(data, loggingEvent);
            appender.getClient().send(data);
        }
        return loggingEvent;
    }

    protected void writeBasic(Map<String, Object> json, LoggingEvent event) {
        json.put("datetime", new Date(event.getTimeStamp()).toInstant().toString());
        json.put("host", appender.getHostName());
        json.put("environment", appender.getEnvironmentName());
        json.put("application", appender.getApplicationName());
        json.put("logger", event.getLoggerName());
        json.put("level", event.getLevel().toString());
        json.put("logmessage", event.getMessage());
        json.put("thread", event.getThreadName());
    }

    protected void writeMDC(Map<String, Object> json, LoggingEvent event) {
        for (Object key : event.getProperties().keySet()) {
            json.put(key.toString(), event.getProperties().get(key).toString());
        }
    }

    protected void writeThrowable(Map<String, Object> json, LoggingEvent event) {
        ThrowableInformation throwableInformation = event.getThrowableInformation();
        if (throwableInformation != null) {
            Throwable throwable = throwableInformation.getThrowable();
            json.put("class", throwable.getClass().getCanonicalName());
            json.put("stacktrace", getStackTrace(throwable));
        }
    }

    protected String getStackTrace(Throwable throwable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        throwable.printStackTrace(printWriter);
        return result.toString();
    }

}

