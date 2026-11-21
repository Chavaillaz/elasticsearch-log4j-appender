package com.chavaillaz.appender.log4j.elastic;

import static org.apache.logging.log4j.core.Appender.ELEMENT_TYPE;
import static org.apache.logging.log4j.core.Core.CATEGORY_NAME;

import java.time.format.DateTimeFormatter;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.jspecify.annotations.Nullable;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogDelivery;
import com.chavaillaz.appender.log4j.AbstractLogDeliveryAppender;
import com.chavaillaz.appender.log4j.LogDeliveryAppenderBuilder;

/**
 * Appender implementation using Elasticsearch for transmissions of logs from Log4j.
 */
@Plugin(name = "ElasticsearchAppender", category = CATEGORY_NAME, elementType = ELEMENT_TYPE)
public class ElasticsearchAppender extends AbstractLogDeliveryAppender<ElasticsearchConfiguration> {

    protected ElasticsearchAppender(Builder builder, ElasticsearchConfiguration configuration) {
        super(builder, configuration);
    }

    @PluginBuilderFactory
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LogDelivery createLogDeliveryHandler() {
        return new ElasticsearchLogDelivery(getLogConfiguration());
    }

    /**
     * Holds the attributes of the element, those every appender built on the common library takes, inherited, and
     * those of Elasticsearch.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder extends LogDeliveryAppenderBuilder<ElasticsearchAppender, Builder> {

        @PluginBuilderAttribute("Index")
        private String index = "ha";

        @PluginBuilderAttribute("IndexSuffix")
        private @Nullable String indexSuffix;

        @PluginBuilderAttribute("Url")
        @Required(message = "No URL of Elasticsearch provided")
        private String url;

        @PluginBuilderAttribute("UrlTrusted")
        private boolean urlTrusted = false;

        @PluginBuilderAttribute("User")
        private @Nullable String user;

        /**
         * The password, which the status logger lists hashed among the attributes of the appender, as the API
         * key, rather than as given.
         */
        @PluginBuilderAttribute(value = "Password", sensitive = true)
        private @Nullable String password;

        @PluginBuilderAttribute(value = "ApiKey", sensitive = true)
        private @Nullable String apiKey;

        /**
         * Creates a builder, every attribute at its default.
         */
        public Builder() {
            // Called by ElasticsearchAppender.builder(), the factory method Log4j2 builds the plugin with
        }

        /**
         * The delivery options an attribute left unset keeps the value of.
         *
         * @return The defaults of the appender, batching documents
         */
        @Override
        protected DeliveryOptions defaultDeliveryOptions() {
            return ElasticsearchConfiguration.DEFAULT_DELIVERY_OPTIONS;
        }

        /**
         * Builds the appender.
         *
         * @return The new appender
         * @throws IllegalArgumentException If a delivery option is out of its range, or the flush level unknown
         */
        @Override
        public ElasticsearchAppender build() {
            ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                    .application(getApplication())
                    .host(getHost())
                    .environment(getEnvironment())
                    .index(index)
                    .indexSuffix(toFormatter(indexSuffix))
                    .url(url)
                    .urlTrusted(urlTrusted)
                    .user(user)
                    .password(password)
                    .apiKey(apiKey)
                    .deliveryOptions(getDeliveryOptions())
                    .build();
            return new ElasticsearchAppender(this, configuration);
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

    }

}
