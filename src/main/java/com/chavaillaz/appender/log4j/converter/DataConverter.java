package com.chavaillaz.appender.log4j.converter;

import com.chavaillaz.appender.log4j.ElasticsearchAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Map;

public interface DataConverter {

    Map<String, Object> convert(ElasticsearchAppender appender, LoggingEvent event);

}
