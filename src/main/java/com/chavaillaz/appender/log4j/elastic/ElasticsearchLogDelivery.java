package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.log4j.elastic.ElasticsearchUtils.createClient;
import static java.time.OffsetDateTime.now;

import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.chavaillaz.appender.log4j.AbstractBatchLogDelivery;
import lombok.extern.log4j.Log4j2;

/**
 * Implementation of logs transmission for Elasticsearch.
 */
@Log4j2
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
     * @param client        The Elasticsearch client to use
     */
    public ElasticsearchLogDelivery(ElasticsearchConfiguration configuration, ElasticsearchClient client) {
        super(configuration);
        this.client = client;
    }

    @Override
    protected boolean sendBulk(List<Map<String, Object>> documents) {
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (Map<String, Object> document : documents) {
                builder.operations(operation -> operation
                        .index(index -> index
                                .index(getConfiguration().generateIndexName(now()))
                                .document(document)));
            }

            BulkResponse response = client.bulk(builder.build());
            if (!response.errors()) {
                log.debug("Bulk of {} elements sent successfully in {}ms", documents.size(), response.took());
                return true;
            }
        } catch (Exception e) {
            log.warn("Error when sending bulk", e);
        }
        return false;
    }

    @Override
    public void close() {
        if (client != null) {
            super.close();
        }
    }

}
