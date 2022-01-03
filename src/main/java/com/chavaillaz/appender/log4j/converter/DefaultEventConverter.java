package com.chavaillaz.appender.log4j.converter;

import com.chavaillaz.appender.log4j.ElasticsearchAppender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    public Map<String, Object> convert(ElasticsearchAppender appender, LogEvent event) {
        Map<String, Object> data = new HashMap<>();
        writeBasic(data, appender, event);
        writeThrowable(data, event);
        writeMDC(data, event);
        return data;
    }

    protected void writeBasic(Map<String, Object> json, ElasticsearchAppender appender, LogEvent event) {
        json.put(getDateField(), Instant.ofEpochMilli(event.getInstant().getEpochMillisecond()).toString());
        json.put("host", appender.getConfiguration().getHostName());
        json.put("environment", appender.getConfiguration().getEnvironmentName());
        json.put("application", appender.getConfiguration().getApplicationName());
        json.put("logger", event.getLoggerFqcn());
        json.put("level", Optional.of(event)
                .map(LogEvent::getLevel)
                .map(Level::toString)
                .orElse(null));
        json.put("logmessage", Optional.of(event)
                .map(LogEvent::getMessage)
                .map(Message::getFormattedMessage)
                .orElse(null));
        json.put("thread", event.getThreadName());
    }

    protected void writeMDC(Map<String, Object> json, LogEvent event) {
        if (event.getContextData() != null) {
            event.getContextData().forEach(json::putIfAbsent);
        }
    }

    protected void writeThrowable(Map<String, Object> json, LogEvent event) {
        Throwable throwable = event.getThrown();
        if (throwable != null) {
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
