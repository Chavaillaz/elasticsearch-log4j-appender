package com.chavaillaz.appender.log4j;

import com.chavaillaz.appender.log4j.converter.DefaultEventConverter;
import com.chavaillaz.appender.log4j.converter.EventConverter;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchConfigurationTest {

    @Test
    void testGenerateIndexNameDate() {
        // Given
        String index = "idx";
        String suffix = "yyyy.MM.dd";
        ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
        configuration.setIndex(index);
        configuration.setIndexSuffix("-" + suffix);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        OffsetDateTime date = now();

        // When
        String indexName = configuration.generateIndexName(date.toString());

        // Then
        assertThat(indexName).isEqualTo(index + "-" + formatter.format(date));
    }

    @Test
    void testWrongConverter() {
        // Given
        ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
        configuration.setEventConverter("package.does.not.exist.NotFoundConverter");

        // When
        EventConverter converter = configuration.getEventConverter();

        // Then
        assertThat(converter.getClass()).isEqualTo(DefaultEventConverter.class);
    }

}
