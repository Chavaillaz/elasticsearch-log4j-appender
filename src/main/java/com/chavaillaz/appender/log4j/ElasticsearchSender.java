package com.chavaillaz.appender.log4j;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.http.auth.AuthScope.ANY;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

/**
 * Data sender to Elasticsearch.
 */
@Slf4j
public class ElasticsearchSender implements AutoCloseable {

    private final List<Map<String, Object>> batch = new ArrayList<>();
    private final ElasticsearchConfiguration configuration;
    private RestHighLevelClient client;

    /**
     * Creates a new data sender.
     *
     * @param configuration The configuration to use.
     */
    public ElasticsearchSender(ElasticsearchConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Opens the connection to Elasticsearch.
     * This method has to be called before sending data.
     */
    public void open() {
        try {
            RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create(configuration.getUrl()))
                .setHttpClientConfigCallback(httpClientBuilder -> getCredentials().map(httpClientBuilder::setDefaultCredentialsProvider).orElse(httpClientBuilder))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                    .setConnectTimeout(5000)
                    .setSocketTimeout(30000)
                    .setConnectionRequestTimeout(0))
                .setCompressionEnabled(true);

            client = new RestHighLevelClient(restClientBuilder);
        } catch (Exception e) {
            log.error("Elasticsearch client configuration error", e);
        }
    }

    protected Optional<CredentialsProvider> getCredentials() {
        if (configuration.hasCredentials()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(ANY, new UsernamePasswordCredentials(configuration.getUser(), configuration.getPassword()));
            return Optional.of(credentialsProvider);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public synchronized void close() {
        if (client != null) {
            log.debug("Closing client");
            sendBatch();

            Try.run(client::close)
                .onFailure(error -> log.error("Error when closing the rest client", error));

            log.debug("Client closed");
        }
    }

    /**
     * Sends the record data in Elasticsearch.
     *
     * @param record The record data.
     */
    public void send(Map<String, Object> record) {
        send(singletonList(record));
    }

    /**
     * Sends the list of record data in Elasticsearch.
     *
     * @param records The list of record data.
     */
    private void send(List<Map<String, Object>> records) {
        if (records != null && !records.isEmpty()) {
            stackAndSend(records);
        }
    }

    /**
     * Sends a partially filled batch.
     * <p>
     * Intended to be called periodically to clean out pending log messages
     */
    public synchronized void sendPartialBatches() {
        if (!batch.isEmpty()) {
            log.debug("Sending partial batch of {}/{}", batch.size(), configuration.getBatchSize());
            sendBatch();
        }
    }

    private synchronized void stackAndSend(List<Map<String, Object>> data) {
        Optional.ofNullable(data).ifPresent(batch::addAll);
        log.debug("Batch size {}/{}", batch.size(), configuration.getBatchSize());
        if (batch.size() >= configuration.getBatchSize()) {
            sendBatch();
        }
    }

    private void sendBatch() {
        if (!batch.isEmpty()) {
            sendBulk(batch.stream()
                .map(this::generateIndex)
                .collect(Collectors.toList()));

            batch.clear();
        }
    }

    private IndexRequest generateIndex(Map<String, Object> record) {
        // select the index based on the datetime in the record
        String datetime = (String) record.get("datetime");
        return new IndexRequest()
            .source(record)
            .id(randomUUID().toString())
            .index(configuration.generateIndexName(datetime));
    }

    private void sendBulk(List<DocWriteRequest> action) {
        BulkRequest bulk = new BulkRequest();
        action.forEach(bulk::add);

        try {
            log.debug("Sending bulk of {} elements ({} bytes)", bulk.numberOfActions(), bulk.estimatedSizeInBytes());
            BulkResponse result = client.bulk(bulk, DEFAULT);
            if (result.hasFailures()) {
                log.warn("Error when sending bulk ({})\r\n{}", result.status(), result.buildFailureMessage());
            } else {
                log.debug("Bulk sent with success in {}", result.getTook());
            }
        } catch (Exception e) {
            log.warn("Error when sending bulk", e);
        }
    }

}
