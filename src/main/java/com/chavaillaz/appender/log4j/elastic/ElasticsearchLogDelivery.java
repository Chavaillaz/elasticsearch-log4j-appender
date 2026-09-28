package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.log4j.elastic.ElasticsearchUtils.createClient;
import static java.time.OffsetDateTime.now;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.chavaillaz.appender.TransmissionResult;
import com.chavaillaz.appender.log4j.AbstractBatchLogDelivery;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of logs transmission for Elasticsearch.
 */
public class ElasticsearchLogDelivery extends AbstractBatchLogDelivery<ElasticsearchConfiguration> {

    private final ElasticsearchClient client;

    /**
     * Creates a new logs delivery handler for Elasticsearch.
     *
     * @param configuration The configuration to use
     */
    public ElasticsearchLogDelivery(ElasticsearchConfiguration configuration) {
        this(configuration, createClient(configuration));
    }

    /**
     * Creates a new logs delivery handler for Elasticsearch.
     *
     * @param configuration The configuration to use
     * @param client        The Elasticsearch client to use, closed along with this delivery
     */
    public ElasticsearchLogDelivery(ElasticsearchConfiguration configuration, ElasticsearchClient client) {
        super(configuration);
        this.client = client;
    }

    @Override
    protected CompletableFuture<TransmissionResult<Map<String, Object>>> sendBulk(List<Map<String, Object>> documents) {
        try {
            String index = getConfiguration().generateIndexName(now());
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (Map<String, Object> document : documents) {
                builder.operations(operation -> operation
                        .index(action -> action
                                .index(index)
                                .document(document)));
            }

            BulkResponse response = client.bulk(builder.build());
            return completedFuture(response.errors() ? TransmissionResult.failure(null) : TransmissionResult.success());
        } catch (Exception e) {
            // Logged with its cause and retried by the library
            return completedFuture(TransmissionResult.failure(e));
        }
    }

    @Override
    public void close(@Nullable Duration timeout) throws Exception {
        super.close(timeout);
        client.close();
    }

}
