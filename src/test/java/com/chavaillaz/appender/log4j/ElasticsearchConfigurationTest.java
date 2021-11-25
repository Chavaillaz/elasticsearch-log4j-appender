package com.chavaillaz.appender.log4j;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchConfigurationTest {

    @Test
    void testGenerateIndexNameDate() {
        // Given
        ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
        configuration.setIndex("idx");
        configuration.setIndexSuffix("-yyyy.MM.dd");

        // When
        String indexName = configuration.generateIndexName("2021-01-20T16:30:15.791");

        // Then
        assertThat(indexName).isEqualTo("idx-2021.01.20");
    }

}
