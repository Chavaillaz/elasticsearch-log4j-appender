package com.chavaillaz.appender.log4j;

import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.log4j.spi.ErrorCode.GENERIC_FAILURE;

/**
 * Using Elasticsearch to store Logging Events for insert the document log4j.
 */
@Slf4j
@Getter
@Setter
@ToString
public class ElasticsearchAppender extends AppenderSkeleton {

    private static final String DEFAULT_ENVIRONMENT = "local";
    private static final String DEFAULT_INDEX = "ha";
    private static final String DEFAULT_INDEX_SUFFIX = "-yyyy.MM.dd";
    private static final int DEFAULT_BATCH_SIZE = 1;
    private static final long DEFAULT_BATCH_DELAY = 1000;
    private static final long DEFAULT_BATCH_INITIAL_DELAY = 1000;
    private static final String VARIABLE_NAME_ENVIRONMENT = "ENV";

    @Exclude
    private final ScheduledExecutorService threadPool = newSingleThreadScheduledExecutor();

    @Exclude
    private ElasticsearchSender client;
    private String applicationName = "unknown";
    private String hostName = getInitialHostname();
    private String environmentName = getProperty(VARIABLE_NAME_ENVIRONMENT, DEFAULT_ENVIRONMENT);
    private String elasticIndex = DEFAULT_INDEX;
    private String elasticIndexSuffix = DEFAULT_INDEX_SUFFIX;
    private String elasticUrl;
    private String elasticUser;
    private String elasticPassword;
    private boolean elasticParallelExecution = true;
    private int elasticBatchSize = DEFAULT_BATCH_SIZE;
    private long elasticBatchDelay = DEFAULT_BATCH_DELAY;
    private long elasticBatchInitialDelay = DEFAULT_BATCH_INITIAL_DELAY;

    private static String getProperty(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key)).orElse(System.getProperty(key, defaultValue));
    }

    protected String getInitialHostname() {
        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            errorHandler.error("Unknown host", e, GENERIC_FAILURE);
        }
        return host;
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
            if (elasticParallelExecution) {
                threadPool.submit(new ElasticsearchAppenderTask(this, loggingEvent));
            } else {
                new ElasticsearchAppenderTask(this, loggingEvent).call();
            }
        }
    }

    /**
     * Creates the elasticsearch client.
     */
    @Override
    public void activateOptions() {
        try {
            ElasticsearchConfiguration configuration = new ElasticsearchConfiguration()
                    .setUrl(getElasticUrl())
                    .setIndex(getElasticIndex())
                    .setIndexSuffix(getElasticIndexSuffix())
                    .setBatchSize(elasticBatchSize);

            if (hasCredentials()) {
                configuration
                        .setUser(getElasticUser())
                        .setPassword(getElasticPassword());
            }

            client = new ElasticsearchSender(configuration);
            client.open();

        } catch (Exception e) {
            errorHandler.error("Client configuration error", e, GENERIC_FAILURE);
        }

        if (elasticBatchSize > 1) {
            threadPool.scheduleWithFixedDelay(() -> client.sendPartialBatches(), elasticBatchInitialDelay, elasticBatchDelay, MILLISECONDS);
        }

        log.info("Appender initialized: {}", this);
        super.activateOptions();
    }

    /**
     * Indicates if the current appender use credentials to send events to Elasticsearch.
     *
     * @return {@code true} if the user and the password are configured, {@code false} otherwise.
     */
    public boolean hasCredentials() {
        return getElasticUser() != null && getElasticPassword() != null;
    }

    /**
     * Closes Elasticsearch client.
     */
    @Override
    public void close() {
        threadPool.shutdown();
        Try.run(() -> threadPool.awaitTermination(1, MINUTES));
        client.close();
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
