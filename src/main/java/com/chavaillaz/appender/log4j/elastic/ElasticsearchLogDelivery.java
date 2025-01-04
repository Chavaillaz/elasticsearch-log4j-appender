package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.CommonUtils.createSSLContext;
import static com.chavaillaz.appender.log4j.elastic.ElasticsearchUtils.createClient;
import static java.time.OffsetDateTime.now;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.chavaillaz.appender.log4j.AbstractBatchLogDeliveryAppender;
import lombok.extern.log4j.Log4j2;

/**
 * Implementation of logs transmission for Elasticsearch.
 */
@Log4j2
public class ElasticsearchLogDelivery extends AbstractBatchLogDeliveryAppender<ElasticsearchConfiguration> {

    private final ElasticsearchClient client;

    /**
     * Creates a new logs delivery handler for Elasticsearch.
     *
     * @param configuration The configuration to use
     */
    public ElasticsearchLogDelivery(ElasticsearchConfiguration configuration) {
        super(configuration);

        if (isNotBlank(configuration.getApiKey())) {
            client = createClient(configuration.getUrl(), createSSLContext(), configuration.getApiKey());
        } else {
            client = createClient(configuration.getUrl(), createSSLContext(), configuration.getUser(), configuration.getPassword());
        }
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
