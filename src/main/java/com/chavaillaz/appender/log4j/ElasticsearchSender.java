package com.chavaillaz.appender.log4j;

import static com.chavaillaz.appender.log4j.ElasticsearchUtils.createClient;
import static com.chavaillaz.appender.log4j.ElasticsearchUtils.createSSLContext;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Data sender to Elasticsearch.
 */
@Log4j2
public class ElasticsearchSender implements AutoCloseable {

    private final List<Map<String, Object>> batch = new ArrayList<>();

    @Getter
    private final ElasticsearchConfiguration configuration;

    @Getter
    private ElasticsearchClient client;

    /**
     * Creates a new data sender.
     *
     * @param configuration The configuration to use
     */
    public ElasticsearchSender(ElasticsearchConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Opens the connection to Elasticsearch.
     * This method has to be called before sending data.
     */
    public void open() {
        if (isNotBlank(configuration.getApiKey())) {
            client = createClient(configuration.getUrl(), createSSLContext(), configuration.getApiKey());
        } else {
            client = createClient(configuration.getUrl(), createSSLContext(), configuration.getUser(), configuration.getPassword());
        }
    }

    /**
     * Sends the record data in Elasticsearch.
     *
     * @param record The record data
     */
    public void send(Map<String, Object> record) {
        send(singletonList(record));
    }

    /**
     * Sends the list of record data in Elasticsearch.
     *
     * @param records The list of record data
     */
    public void send(List<Map<String, Object>> records) {
        if (records != null && !records.isEmpty()) {
            stackAndSend(records);
        }
    }

    /**
     * Sends a partially filled batch.
     * Intended to be called periodically to clean out pending log messages
     */
    public synchronized void sendPartialBatches() {
        if (!batch.isEmpty()) {
            log.debug("Sending partial batch of {}/{}", batch.size(), configuration.getBatchSize());
            sendBatch();
        }
    }

    private synchronized void stackAndSend(List<Map<String, Object>> data) {
        batch.addAll(data);
        log.trace("Batch size {}/{}", batch.size(), configuration.getBatchSize());
        if (batch.size() >= configuration.getBatchSize()) {
            sendBatch();
        }
    }

    private void sendBatch() {
        if (!batch.isEmpty() && sendBulk(batch)) {
            batch.clear();
        }
    }

    private boolean sendBulk(List<Map<String, Object>> documents) {
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (Map<String, Object> document : documents) {
                String dateTime = document.get(configuration.getEventConverter().getDateField()).toString();
                builder.operations(operation -> operation
                        .index(index -> index
                                .index(configuration.generateIndexName(dateTime))
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
    public synchronized void close() {
        if (client != null) {
            sendBatch();
        }
    }

}
