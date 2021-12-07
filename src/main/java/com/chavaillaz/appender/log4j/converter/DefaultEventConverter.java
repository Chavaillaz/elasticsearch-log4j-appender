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

/**
 * Default converter converting the following fields:
 * <ul>
 *     <li><strong>datetime:</strong> Date of logging event</li>
 *     <li><strong>host:</strong> Taken from appender configuration)</li>
 *     <li><strong>environment:</strong> Taken from appender configuration)</li>
 *     <li><strong>application:</strong> Taken from appender configuration)</li>
 *     <li><strong>logger:</strong> Logger of logging event</li>
 *     <li><strong>level:</strong> Level of logging event</li>
 *     <li><strong>logmessage:</strong> Message of the logging event</li>
 *     <li><strong>thread:</strong> Thread that created the logging event</li>
 * </ul>
 * <p>All the MDC fields will also be added as is (if they not already exist).</p>
 * <p>In case the event contains an exception, it also includes the fields:</p>
 * <ul>
 *     <li><strong>class:</strong> Class of the exception</li>
 *     <li><strong>stacktrace:</strong> Stacktrace of the exception</li>
 * </ul>
 */
public class DefaultEventConverter implements EventConverter {

    @Override
    public String getDateField() {
        return "datetime";
    }

    @Override
    public Map<String, Object> convert(ElasticsearchAppender appender, LoggingEvent event) {
        Map<String, Object> data = new HashMap<>();
        writeBasic(data, appender, event);
        writeThrowable(data, event);
        writeMDC(data, event);
        return data;
    }

    protected void writeBasic(Map<String, Object> json, ElasticsearchAppender appender, LoggingEvent event) {
        json.put(getDateField(), new Date(event.getTimeStamp()).toInstant().toString());
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
            json.putIfAbsent(key.toString(), event.getProperties().get(key).toString());
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
