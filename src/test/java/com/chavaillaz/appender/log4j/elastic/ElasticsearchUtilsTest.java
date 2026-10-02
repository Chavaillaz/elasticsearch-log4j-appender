package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.log4j.elastic.ElasticsearchUtils.createClient;
import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import org.junit.jupiter.api.Test;

class ElasticsearchUtilsTest {

    @Test
    void testCreateClientWithoutCredentials() throws Exception {
        // Given: a cluster without security, reached with neither a user nor an API key
        ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                .url("http://localhost:9200")
                .build();

        // When
        try (ElasticsearchAsyncClient client = createClient(configuration)) {

            // Then
            assertThat(client).isNotNull();
        }
    }

}
