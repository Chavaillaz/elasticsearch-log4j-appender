package com.chavaillaz.appender.log4j;

import com.chavaillaz.appender.log4j.converter.DefaultEventConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.concurrent.ScheduledExecutorService;

import static com.chavaillaz.appender.log4j.ElasticsearchUtils.getInitialHostname;
import static com.chavaillaz.appender.log4j.ElasticsearchUtils.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.log4j.spi.ErrorCode.GENERIC_FAILURE;

/**
 * Appender using Elasticsearch to store logging events.
 */
@Slf4j
@Getter
@Setter
@ToString
public class ElasticsearchAppender extends AppenderSkeleton {

    @Exclude
    private final ScheduledExecutorService threadPool = newSingleThreadScheduledExecutor();
    private ElasticsearchSender client;
    private String applicationName = getProperty("APPLICATION", "unknown");
    private String hostName = getProperty("HOST", getInitialHostname());
    private String environmentName = getProperty("ENV", "local");
    private String elasticConverter = getProperty("CONVERTER", DefaultEventConverter.class.getName());
    private String elasticIndex = getProperty("INDEX", "ha");
    private String elasticIndexSuffix = getProperty("INDEX_SUFFIX", "-yyyy.MM.dd");
    private String elasticUrl;
    private String elasticUser;
    private String elasticPassword;
    private boolean elasticParallelExecution = true;
    private int elasticBatchSize = 1;
    private long elasticBatchDelay = 1000;
    private long elasticBatchInitialDelay = 1000;

    /**
     * Indicates if the current appender use credentials to send events to Elasticsearch.
     *
     * @return {@code true} if the user and the password are configured, {@code false} otherwise
     */
    public boolean hasCredentials() {
        return getElasticUser() != null && getElasticPassword() != null;
    }

    /**
     * Gets the Elasticsearch sender.
     *
     * @return The client to send messages
     */
    protected ElasticsearchSender getClient() {
        return client;
    }

    /**
     * Creates the elasticsearch client.
     */
    @Override
    public void activateOptions() {
        try {
            ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
            configuration.setUrl(getElasticUrl());
            configuration.setIndex(getElasticIndex());
            configuration.setIndexSuffix(getElasticIndexSuffix());
            configuration.setBatchSize(elasticBatchSize);
            configuration.setEventConverter(elasticConverter);

            if (hasCredentials()) {
                configuration.setUser(getElasticUser());
                configuration.setPassword(getElasticPassword());
            }

            client = new ElasticsearchSender(configuration);
            client.open();

        } catch (Exception e) {
            errorHandler.error("Client configuration error", e, GENERIC_FAILURE);
        }

        if (elasticBatchSize > 1) {
            threadPool.scheduleWithFixedDelay(() -> client.sendPartialBatches(), elasticBatchInitialDelay, elasticBatchDelay, MILLISECONDS);
        }

        super.activateOptions();
    }

    /**
     * Submits the logging event to insert the document if it reaches the severity threshold.
     *
     * @param loggingEvent The logging event to send.
     */
    @Override
    protected void append(LoggingEvent loggingEvent) {
        if (isAsSevereAsThreshold(loggingEvent.getLevel())) {
            loggingEvent.getMDCCopy();
            ElasticsearchAppenderTask task = new ElasticsearchAppenderTask(this, loggingEvent);
            if (elasticParallelExecution) {
                threadPool.submit(task);
            } else {
                task.call();
            }
        }
    }

    /**
     * Closes Elasticsearch client.
     */
    @Override
    public void close() {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1, MINUTES);
        } catch (InterruptedException e) {
            errorHandler.error("Thread interrupted during termination", e, GENERIC_FAILURE);
            currentThread().interrupt();
        } finally {
            client.close();
        }
    }

    /**
     * Ensures that a Layout property is not required
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }

}
