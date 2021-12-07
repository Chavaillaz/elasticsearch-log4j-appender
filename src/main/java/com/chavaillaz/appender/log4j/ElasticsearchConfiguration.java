package com.chavaillaz.appender.log4j;

import com.chavaillaz.appender.log4j.converter.DefaultEventConverter;
import com.chavaillaz.appender.log4j.converter.EventConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Getter
@Setter
public class ElasticsearchConfiguration {

    private String user;
    private String password;
    private String url;
    private String index;
    private String indexSuffix;
    private DateTimeFormatter indexSuffixFormatter;
    private int batchSize;
    private EventConverter eventConverter;

    /**
     * Sets the event converter by instantiating the given class name.
     * Note that it has to have an empty constructor.
     *
     * @param className The class name of the converter
     */
    public void setEventConverter(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor();
            this.eventConverter = (EventConverter) constructor.newInstance();
        } catch (Exception e) {
            this.eventConverter = new DefaultEventConverter();
        }
    }

    /**
     * Sets the date suffix to use when generating the index to which send the documents.
     * Note that it has to follow a pattern recognized by {@link DateTimeFormatter}.
     *
     * @param indexSuffix The index suffix
     */
    public void setIndexSuffix(String indexSuffix) {
        this.indexSuffix = indexSuffix;
        this.indexSuffixFormatter = DateTimeFormatter.ofPattern(indexSuffix);
    }

    /**
     * Generates the index name using the suffix if present.
     *
     * @param utcDateTime The date and time of the event in the format {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
     *                    or {@code null} to avoid using the index prefix if defined
     * @return The computed index name
     */
    public String generateIndexName(String utcDateTime) {
        if (utcDateTime != null && indexSuffixFormatter != null) {
            OffsetDateTime odt = OffsetDateTime.parse(utcDateTime);
            return getIndex() + odt.format(indexSuffixFormatter);
        } else {
            return getIndex();
        }
    }

}
