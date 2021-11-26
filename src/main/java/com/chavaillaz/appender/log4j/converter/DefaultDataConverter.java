package com.chavaillaz.appender.log4j.converter;

import com.chavaillaz.appender.log4j.ElasticsearchAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DefaultDataConverter implements DataConverter {

    @Override
    public Map<String, Object> convert(ElasticsearchAppender appender, LoggingEvent event) {
        Map<String, Object> data = new HashMap<>();
        writeBasic(data, appender, event);
        writeThrowable(data, event);
        writeMDC(data, event);
        return data;
    }

    protected void writeBasic(Map<String, Object> json, ElasticsearchAppender appender, LoggingEvent event) {
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
