package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.CommonUtils.createSSLContext;
import static com.chavaillaz.appender.log4j.elastic.ElasticsearchUtils.createClient;
import static java.time.OffsetDateTime.now;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.chavaillaz.appender.LogDelivery;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Implementation of logs transmission for Elasticsearch.
 */
@Log4j2
public class ElasticsearchLogDelivery implements LogDelivery {

    private final List<Map<String, Object>> batch = new ArrayList<>();

    @Getter
    private final ElasticsearchConfiguration configuration;

    @Getter
    private final ElasticsearchClient client;

    /**
     * Creates a new logs delivery handler for Elasticsearch.
     *
     * @param configuration The configuration to use
     */
    public ElasticsearchLogDelivery(ElasticsearchConfiguration configuration) {
        this.configuration = configuration;

        if (isNotBlank(configuration.getApiKey())) {
            client = createClient(configuration.getUrl(), createSSLContext(), configuration.getApiKey());
        } else {
            client = createClient(configuration.getUrl(), createSSLContext(), configuration.getUser(), configuration.getPassword());
        }
    }

    @Override
    public void send(Map<String, Object> document) {
        send(singletonList(document));
    }

    @Override
    public void send(List<Map<String, Object>> documents) {
        if (documents != null && !documents.isEmpty()) {
            stackAndSend(documents);
        }
    }

    @Override
    public synchronized void flush() {
        if (!batch.isEmpty()) {
            log.debug("Sending partial batch of {}/{}", batch.size(), configuration.getFlushThreshold());
            sendBatch();
        }
    }

    private synchronized void stackAndSend(List<Map<String, Object>> data) {
        batch.addAll(data);
        log.trace("Batch size {}/{}", batch.size(), configuration.getFlushThreshold());
        if (batch.size() >= configuration.getFlushThreshold()) {
            sendBatch();
        }
    }

    private synchronized void sendBatch() {
        if (!batch.isEmpty() && sendBulk(batch)) {
            batch.clear();
        }
    }

    private boolean sendBulk(List<Map<String, Object>> documents) {
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (Map<String, Object> document : documents) {
                builder.operations(operation -> operation
                        .index(index -> index
                                .index(configuration.generateIndexName(now()))
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
            sendBatch();
        }
    }

}
