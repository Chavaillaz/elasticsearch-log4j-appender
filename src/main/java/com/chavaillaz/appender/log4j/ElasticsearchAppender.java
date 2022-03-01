package com.chavaillaz.appender.log4j;

import com.chavaillaz.appender.log4j.converter.DefaultEventConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.chavaillaz.appender.log4j.ElasticsearchUtils.getInitialHostname;
import static com.chavaillaz.appender.log4j.ElasticsearchUtils.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.logging.log4j.core.Appender.ELEMENT_TYPE;
import static org.apache.logging.log4j.core.Core.CATEGORY_NAME;
import static org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout;

/**
 * Appender using Elasticsearch to store logging events.
 */
@Log4j2
@Getter
@ToString
@Plugin(name = "ElasticsearchAppender", category = CATEGORY_NAME, elementType = ELEMENT_TYPE)
public class ElasticsearchAppender extends AbstractAppender {

    @Exclude
    private final ScheduledExecutorService threadPool = newSingleThreadScheduledExecutor();
    private final ElasticsearchConfiguration configuration;
    private ElasticsearchSender client;

    protected ElasticsearchAppender(String name, Filter filter, Layout<?> layout, ElasticsearchConfiguration configuration) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
        this.configuration = configuration;
    }

    @PluginBuilderFactory
    public static Builder builder() {
        return new Builder();
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
    public void start() {
        try {
            client = new ElasticsearchSender(configuration);
            client.open();
        } catch (Exception e) {
            error("Client configuration error", e);
        }

        if (configuration.getBatchSize() > 1) {
            threadPool.scheduleWithFixedDelay(
                    client::sendPartialBatches,
                    configuration.getBatchInitialDelay(),
                    configuration.getBatchDelay(),
                    MILLISECONDS);
        }

        super.start();
    }

    /**
     * Submits the logging event to insert the document if it reaches the severity threshold.
     *
     * @param loggingEvent The logging event to send.
     */
    @Override
    public void append(LogEvent loggingEvent) {
        ElasticsearchAppenderTask task = new ElasticsearchAppenderTask(this, loggingEvent);
        if (configuration.isParallelExecution()) {
            threadPool.submit(task);
        } else {
            task.call();
        }
    }

    /**
     * Closes Elasticsearch client.
     */
    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(timeout, timeUnit);
        } catch (InterruptedException e) {
            error("Thread interrupted during termination", e);
            currentThread().interrupt();
        } finally {
            client.close();
            super.stop(timeout, timeUnit);
        }
        return true;
    }

    @Setter
    @Getter
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ElasticsearchAppender> {

        @PluginBuilderAttribute
        @Required(message = "No appender name provided")
        private String name;

        @PluginElement("Layout")
        private Layout<String> layout = createDefaultLayout();

        @PluginElement("Filter")
        private Filter filter;

        @PluginBuilderAttribute("ApplicationName")
        private String applicationName = getProperty("APPLICATION", "unknown");

        @PluginBuilderAttribute("HostName")
        private String hostName = getProperty("HOST", getInitialHostname());

        @PluginBuilderAttribute("EnvironmentName")
        private String environmentName = getProperty("ENV", "local");

        @PluginBuilderAttribute("ElasticConverter")
        private String elasticConverter = getProperty("CONVERTER", DefaultEventConverter.class.getName());

        @PluginBuilderAttribute("ElasticIndex")
        private String elasticIndex = getProperty("INDEX", "ha");

        @PluginBuilderAttribute("ElasticIndexSuffix")
        private String elasticIndexSuffix = getProperty("INDEX_SUFFIX", "-yyyy.MM.dd");

        @PluginBuilderAttribute("ElasticUrl")
        private String elasticUrl;

        @PluginBuilderAttribute("ElasticUser")
        private String elasticUser;

        @PluginBuilderAttribute("ElasticPassword")
        private String elasticPassword;

        @PluginBuilderAttribute("ElasticParallelExecution")
        private boolean elasticParallelExecution = true;

        @PluginBuilderAttribute("ElasticBatchSize")
        private int elasticBatchSize = 1;

        @PluginBuilderAttribute("ElasticBatchDelay")
        private long elasticBatchDelay = 1000;

        @PluginBuilderAttribute("ElasticBatchInitialDelay")
        private long elasticBatchInitialDelay = 1000;

        @Override
        public ElasticsearchAppender build() {
            ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
            configuration.setApplicationName(getApplicationName());
            configuration.setHostName(getHostName());
            configuration.setEnvironmentName(getEnvironmentName());
            configuration.setEventConverter(getElasticConverter());
            configuration.setIndex(getElasticIndex());
            configuration.setIndexSuffix(getElasticIndexSuffix());
            configuration.setUrl(getElasticUrl());
            configuration.setUser(getElasticUser());
            configuration.setPassword(getElasticPassword());
            configuration.setParallelExecution(isElasticParallelExecution());
            configuration.setBatchSize(getElasticBatchSize());
            configuration.setBatchDelay(getElasticBatchDelay());
            configuration.setBatchInitialDelay(getElasticBatchInitialDelay());
            return new ElasticsearchAppender(getName(), getFilter(), getLayout(), configuration);
        }

    }

}