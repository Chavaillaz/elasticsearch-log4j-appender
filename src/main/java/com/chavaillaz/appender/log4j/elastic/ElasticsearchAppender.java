package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.CommonUtils.getInitialHostname;
import static com.chavaillaz.appender.CommonUtils.getProperty;
import static org.apache.logging.log4j.core.Appender.ELEMENT_TYPE;
import static org.apache.logging.log4j.core.Core.CATEGORY_NAME;
import static org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout;

import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogDelivery;
import com.chavaillaz.appender.log4j.AbstractLogDeliveryAppender;
import com.chavaillaz.appender.log4j.DefaultLogConverter;
import com.chavaillaz.appender.log4j.LogConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.jspecify.annotations.Nullable;

/**
 * Appender implementation using Elasticsearch for transmissions of logs from Log4j.
 */
@Plugin(name = "ElasticsearchAppender", category = CATEGORY_NAME, elementType = ELEMENT_TYPE)
public class ElasticsearchAppender extends AbstractLogDeliveryAppender<ElasticsearchConfiguration> {

    /**
     * The status logger of Log4j2 rather than an application logger, which would feed these diagnostics back
     * into this appender.
     */
    private static final Logger log = StatusLogger.getLogger();

    /**
     * The converter the configuration names, or {@code null} for the default one of the appender.
     */
    private final @Nullable LogConverter converter;

    protected ElasticsearchAppender(String name, @Nullable Filter filter, @Nullable Layout<?> layout, ElasticsearchConfiguration configuration) {
        super(name, filter, layout, configuration);
        this.converter = createConverter(configuration);
    }

    @PluginBuilderFactory
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates the converter the configuration names, configured with it.
     *
     * @param configuration The configuration naming the converter
     * @return The converter, or {@code null} for the default one, which is also used, and reported, when the
     *         converter named cannot be created
     */
    private static @Nullable LogConverter createConverter(ElasticsearchConfiguration configuration) {
        String className = configuration.getConverter();
        if (className == null || className.isBlank() || DefaultLogConverter.class.getName().equals(className)) {
            return null;
        }
        try {
            LogConverter created = LoaderUtil.newCheckedInstanceOf(className, LogConverter.class);
            created.configure(configuration);
            return created;
        } catch (Exception | LinkageError e) {
            log.error("Unable to create the converter {}, converting with the default one instead", className, e);
            return null;
        }
    }

    @Override
    public LogDelivery createLogDeliveryHandler() {
        return new ElasticsearchLogDelivery(getLogConfiguration());
    }

    @Override
    protected LogConverter getLogConverter() {
        return converter != null ? converter : super.getLogConverter();
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ElasticsearchAppender> {

        /**
         * The delivery options an attribute left unset keeps the value of.
         */
        private static final DeliveryOptions DEFAULTS = DeliveryOptions.defaults();

        @PluginBuilderAttribute
        @Required(message = "No appender name provided")
        private String name;

        @PluginElement("Layout")
        private Layout<String> layout = createDefaultLayout();

        @PluginElement("Filter")
        private Filter filter;

        @PluginBuilderAttribute("Application")
        private String applicationName = getProperty("APP", "unknown");

        /**
         * The host name, left unset for the {@code HOST} variable or else the name of the machine, resolved
         * when building the appender: its lookup can take seconds, for nothing when a host name is given.
         */
        @PluginBuilderAttribute("Host")
        private String hostName;

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

        @PluginBuilderAttribute("UrlTrusted")
        private boolean elasticUrlTrusted = false;

        @PluginBuilderAttribute("User")
        private String elasticUser = getProperty("ELASTIC_USER", null);

        @PluginBuilderAttribute("Password")
        private String elasticPassword = getProperty("ELASTIC_PASSWORD", null);

        @PluginBuilderAttribute("ApiKey")
        private String elasticApiKey = getProperty("ELASTIC_API_KEY", null);

        @PluginBuilderAttribute("DeadLetterFile")
        private String deadLetterFile = getProperty("DEAD_LETTER_FILE", null);

        @PluginBuilderAttribute("FlushThreshold")
        private long flushThreshold = 100;

        @PluginBuilderAttribute("FlushInterval")
        private long flushInterval = 5_000;

        @PluginBuilderAttribute("FlushLevel")
        private String flushLevel;

        @PluginBuilderAttribute("MaxConcurrentTransmissions")
        private int maxConcurrentTransmissions = DEFAULTS.maxConcurrentTransmissions();

        @PluginBuilderAttribute("MaxPendingDocuments")
        private int maxPendingDocuments = DEFAULTS.maxPendingDocuments();

        @PluginBuilderAttribute("MaxRequestDocuments")
        private int maxRequestDocuments = DEFAULTS.maxRequestDocuments();

        @PluginBuilderAttribute("MaxPendingBytes")
        private long maxPendingBytes = DEFAULTS.maxPendingBytes();

        @PluginBuilderAttribute("MaxRequestBytes")
        private long maxRequestBytes = DEFAULTS.maxRequestBytes();

        @PluginBuilderAttribute("MaxConsecutiveFailures")
        private int maxConsecutiveFailures = DEFAULTS.maxConsecutiveFailures();

        @PluginBuilderAttribute("MaxQueuedEvents")
        private int maxQueuedEvents = DEFAULTS.maxQueuedEvents();

        @PluginBuilderAttribute("RetryBackoff")
        private long retryBackoff = DEFAULTS.retryBackoff().toMillis();

        @PluginBuilderAttribute("MaxRetryBackoff")
        private long maxRetryBackoff = DEFAULTS.maxRetryBackoff().toMillis();

        @PluginBuilderAttribute("CloseTimeout")
        private long closeTimeout = DEFAULTS.closeTimeout().toMillis();

        /**
         * Builds the appender.
         *
         * @return The new appender
         * @throws IllegalArgumentException If a delivery option is out of its range, or the flush level unknown
         */
        @Override
        public ElasticsearchAppender build() {
            ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                    .application(getApplicationName())
                    .host(resolveHostName())
                    .environment(getEnvironmentName())
                    .converter(getElasticConverter())
                    .index(getElasticIndex())
                    .indexSuffix(toFormatter(getElasticIndexSuffix()))
                    .url(getElasticUrl())
                    .urlTrusted(isElasticUrlTrusted())
                    .user(getElasticUser())
                    .password(getElasticPassword())
                    .apiKey(getElasticApiKey())
                    .deadLetterFile(toPath(getDeadLetterFile()))
                    .deliveryOptions(DeliveryOptions.builder()
                            .flushThreshold(getFlushThreshold())
                            .flushInterval(Duration.ofMillis(getFlushInterval()))
                            .flushLevel(toLevel(getFlushLevel()))
                            .maxConcurrentTransmissions(getMaxConcurrentTransmissions())
                            .maxPendingDocuments(getMaxPendingDocuments())
                            .maxRequestDocuments(getMaxRequestDocuments())
                            .maxPendingBytes(getMaxPendingBytes())
                            .maxRequestBytes(getMaxRequestBytes())
                            .maxConsecutiveFailures(getMaxConsecutiveFailures())
                            .maxQueuedEvents(getMaxQueuedEvents())
                            .retryBackoff(Duration.ofMillis(getRetryBackoff()))
                            .maxRetryBackoff(Duration.ofMillis(getMaxRetryBackoff()))
                            .closeTimeout(Duration.ofMillis(getCloseTimeout()))
                            .build())
                    .build();
            return new ElasticsearchAppender(getName(), getFilter(), getLayout(), configuration);
        }

        /**
         * The host name set, or else the {@code HOST} variable, or else the name of the machine.
         *
         * @return The host name
         */
        private String resolveHostName() {
            if (hostName != null) {
                return hostName;
            }
            String variable = getProperty("HOST", null);
            return variable != null ? variable : getInitialHostname();
        }

        /**
         * Parses the pattern of the index suffix.
         *
         * @param pattern The pattern, following {@link DateTimeFormatter}, or an empty one for no suffix
         * @return The formatter, or {@code null} for no suffix
         */
        private static @Nullable DateTimeFormatter toFormatter(@Nullable String pattern) {
            return pattern == null || pattern.isEmpty() ? null : DateTimeFormatter.ofPattern(pattern);
        }

        /**
         * Parses the path of the dead-letter file.
         *
         * @param path The path, or an empty one for none
         * @return The path, or {@code null} for none
         */
        private static @Nullable Path toPath(@Nullable String path) {
            return path == null || path.isBlank() ? null : Path.of(path);
        }

        /**
         * Parses the flush level, failing the configuration rather than ignoring a level misspelled.
         *
         * @param name The name of the level, whatever its case, or an empty one for none
         * @return The level, or {@code null} for none
         */
        private static @Nullable Level toLevel(@Nullable String name) {
            return name == null || name.isBlank() ? null : Level.valueOf(name);
        }

    }

}
