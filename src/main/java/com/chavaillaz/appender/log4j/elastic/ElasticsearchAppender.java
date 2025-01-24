package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.CommonUtils.getInitialHostname;
import static com.chavaillaz.appender.CommonUtils.getProperty;
import static org.apache.logging.log4j.core.Appender.ELEMENT_TYPE;
import static org.apache.logging.log4j.core.Core.CATEGORY_NAME;
import static org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout;

import java.time.Duration;
import java.util.Optional;

import com.chavaillaz.appender.LogDelivery;
import com.chavaillaz.appender.log4j.AbstractLogDeliveryAppender;
import com.chavaillaz.appender.log4j.DefaultLogConverter;
import com.chavaillaz.appender.log4j.LogConverter;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * Appender implementation using Elasticsearch for transmissions of logs from Log4j.
 */
@Plugin(name = "ElasticsearchAppender", category = CATEGORY_NAME, elementType = ELEMENT_TYPE)
public class ElasticsearchAppender extends AbstractLogDeliveryAppender<ElasticsearchConfiguration> {

    protected ElasticsearchAppender(String name, Filter filter, Layout<?> layout, ElasticsearchConfiguration configuration) {
        super(name, filter, layout, configuration);
    }

    @PluginBuilderFactory
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LogDelivery createLogDeliveryHandler() {
        return new ElasticsearchLogDelivery(getLogConfiguration());
    }

    @Override
    public Runnable createLogDeliveryTask(LogEvent loggingEvent) {
        LogConverter converter = getLogConfiguration().getConverter();
        LogEvent immutableEvent = loggingEvent.toImmutable();
        return () -> Optional.ofNullable(getLogDeliveryHandler())
                .ifPresent(handler -> handler.send(converter.convert(immutableEvent)));
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

        @PluginBuilderAttribute("Application")
        private String applicationName = getProperty("APP", "unknown");

        @PluginBuilderAttribute("Host")
        private String hostName = getProperty("HOST", getInitialHostname());

        @PluginBuilderAttribute("Environment")
        private String environmentName = getProperty("ENV", "local");

        @PluginBuilderAttribute("Converter")
        private String elasticConverter = getProperty("CONVERTER", DefaultLogConverter.class.getName());

        @PluginBuilderAttribute("Index")
        private String elasticIndex = getProperty("INDEX", "ha");

        @PluginBuilderAttribute("IndexSuffix")
        private String elasticIndexSuffix = getProperty("INDEX_SUFFIX", "");

        @PluginBuilderAttribute("Url")
        private String elasticUrl = getProperty("ELASTIC_URL", null);

        @PluginBuilderAttribute("User")
        private String elasticUser = getProperty("ELASTIC_USER", null);

        @PluginBuilderAttribute("Password")
        private String elasticPassword = getProperty("ELASTIC_PASSWORD", null);

        @PluginBuilderAttribute("ApiKey")
        private String elasticApiKey = getProperty("ELASTIC_API_KEY", null);

        @PluginBuilderAttribute("FlushThreshold")
        private long flushThreshold = 100;

        @PluginBuilderAttribute("FlushInterval")
        private long flushInterval = 5_000;

        @Override
        public ElasticsearchAppender build() {
            ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
            configuration.setApplication(getApplicationName());
            configuration.setHost(getHostName());
            configuration.setEnvironment(getEnvironmentName());
            configuration.setConverter(getElasticConverter());
            configuration.setIndex(getElasticIndex());
            configuration.setIndexSuffix(getElasticIndexSuffix());
            configuration.setUrl(getElasticUrl());
            configuration.setUser(getElasticUser());
            configuration.setPassword(getElasticPassword());
            configuration.setApiKey(getElasticApiKey());
            configuration.setFlushThreshold(getFlushThreshold());
            configuration.setFlushInterval(Duration.ofMillis(getFlushInterval()));
            return new ElasticsearchAppender(getName(), getFilter(), getLayout(), configuration);
        }

    }

}