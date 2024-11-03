package com.chavaillaz.appender.log4j.elastic;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.chavaillaz.appender.LogConfiguration;
import com.chavaillaz.appender.log4j.DefaultLogConverter;
import com.chavaillaz.appender.log4j.LogConverter;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for logs transmissions using Elasticsearch.
 */
@Getter
@Setter
public class ElasticsearchConfiguration implements LogConfiguration {

    private String application;
    private String host;
    private String environment;
    private LogConverter converter;
    private String index;
    private DateTimeFormatter indexSuffix;
    private String url;
    private String user;
    private String password;
    private String apiKey;
    private long flushThreshold;
    private Duration flushInterval;

    /**
     * Sets the logs converter by instantiating the given class name.
     * Note that it must have an empty constructor.
     *
     * @param className The class name of the converter
     */
    public void setConverter(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor();
            this.converter = (LogConverter) constructor.newInstance();
        } catch (Exception e) {
            this.converter = new DefaultLogConverter();
        }
        this.converter.configure(this);
    }

    /**
     * Sets the date suffix to use when generating the index to which send the documents.
     * Note that it has to follow a pattern recognized by {@link DateTimeFormatter}.
     *
     * @param indexSuffix The index suffix
     */
    public void setIndexSuffix(String indexSuffix) {
        this.indexSuffix = DateTimeFormatter.ofPattern(Optional.ofNullable(indexSuffix).orElse(EMPTY));
    }

    /**
     * Generates the index name using the suffix if present.
     *
     * @param dateTime The date and time of the event in the format {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
     *                 or {@code null} to avoid using the index prefix if defined
     * @return The computed index name
     */
    public String generateIndexName(String dateTime) {
        return generateIndexName(Optional.ofNullable(dateTime)
                .map(OffsetDateTime::parse)
                .orElse(null));
    }

    /**
     * Generates the index name using the suffix if present.
     *
     * @param dateTime The date and time of the event or {@code null} to avoid using the index prefix if defined
     * @return The computed index name
     */
    public String generateIndexName(OffsetDateTime dateTime) {
        if (dateTime != null && getIndexSuffix() != null) {
            return getIndex() + dateTime.format(getIndexSuffix());
        } else {
            return getIndex();
        }
    }

}
